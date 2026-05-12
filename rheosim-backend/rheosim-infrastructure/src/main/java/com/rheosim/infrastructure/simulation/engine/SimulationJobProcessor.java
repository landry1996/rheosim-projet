package com.rheosim.infrastructure.simulation.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rheosim.domain.experiment.model.DataColumn;
import com.rheosim.domain.experiment.model.Dataset;
import com.rheosim.domain.experiment.port.DataParserPort;
import com.rheosim.domain.experiment.port.DatasetRepository;
import com.rheosim.domain.experiment.port.FileStoragePort;
import com.rheosim.domain.project.model.ConstitutiveModelType;
import com.rheosim.domain.simulation.model.JobStatus;
import com.rheosim.domain.simulation.model.SimulationJob;
import com.rheosim.domain.simulation.model.SimulationResult;
import com.rheosim.domain.simulation.port.ConstitutiveLaw;
import com.rheosim.domain.simulation.port.ParameterIdentificationPort;
import com.rheosim.domain.simulation.port.ParameterIdentificationPort.FitTarget;
import com.rheosim.domain.simulation.port.SimulationJobRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SimulationJobProcessor {

    private final SimulationJobRepository jobRepository;
    private final DatasetRepository datasetRepository;
    private final FileStoragePort fileStoragePort;
    private final List<DataParserPort> parsers;
    private final ParameterIdentificationPort identifier;
    private final Map<ConstitutiveModelType, ConstitutiveLaw> laws;
    private final ObjectMapper objectMapper;

    public SimulationJobProcessor(SimulationJobRepository jobRepository,
                                  DatasetRepository datasetRepository,
                                  FileStoragePort fileStoragePort,
                                  List<DataParserPort> parsers,
                                  ParameterIdentificationPort identifier,
                                  List<ConstitutiveLaw> lawList,
                                  ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.datasetRepository = datasetRepository;
        this.fileStoragePort = fileStoragePort;
        this.parsers = parsers;
        this.identifier = identifier;
        this.laws = lawList.stream()
                .collect(Collectors.toMap(ConstitutiveLaw::getModelType, Function.identity()));
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 2000)
    public void processQueuedJobs() {
        List<SimulationJob> queuedJobs = jobRepository.findByStatus(JobStatus.QUEUED);
        long runningCount = jobRepository.countByStatus(JobStatus.RUNNING);

        for (SimulationJob job : queuedJobs) {
            if (runningCount >= 4) break;
            processJob(job);
            runningCount++;
        }
    }

    private void processJob(SimulationJob job) {
        try {
            job.start();
            jobRepository.save(job);

            Dataset dataset = datasetRepository.findById(job.getDatasetId())
                    .orElseThrow(() -> new IllegalStateException("Dataset not found: " + job.getDatasetId()));

            InputStream fileContent = fileStoragePort.load(dataset.getFileName());
            DataParserPort parser = parsers.stream()
                    .filter(p -> p.supports(dataset.getContentType()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No parser for: " + dataset.getContentType()));

            DataParserPort.ParseResult parseResult = parser.parse(fileContent, dataset.getContentType());

            ConstitutiveLaw law = laws.get(job.getModelType());
            if (law == null) {
                throw new IllegalStateException("No law implementation for: " + job.getModelType());
            }

            job.updateProgress(10.0);
            jobRepository.save(job);

            FitTarget fitTarget = determineFitTarget(parseResult.columns());
            int xColIdx = findXColumnIndex(parseResult.columns(), fitTarget);
            int yColIdx = findYColumnIndex(parseResult.columns(), fitTarget);

            double[] xData = extractColumn(parseResult.rows(), xColIdx);
            double[] yData = extractColumn(parseResult.rows(), yColIdx);

            job.updateProgress(20.0);
            jobRepository.save(job);

            int paramCount = law.getParameterCount(job.getNumberOfBranches());
            double[] initialGuess = buildInitialGuess(law, job.getNumberOfBranches());
            double[] lowerBounds = buildBounds(law, job.getNumberOfBranches(), true);
            double[] upperBounds = buildBounds(law, job.getNumberOfBranches(), false);

            SimulationResult result = identifier.identify(
                    law, xData, yData, initialGuess, lowerBounds, upperBounds, fitTarget);

            job.updateProgress(90.0);
            jobRepository.save(job);

            String resultJson = objectMapper.writeValueAsString(result);
            job.complete(resultJson);
            jobRepository.save(job);

        } catch (Exception e) {
            job.fail(e.getMessage());
            jobRepository.save(job);
        }
    }

    private FitTarget determineFitTarget(List<DataColumn> columns) {
        for (DataColumn col : columns) {
            switch (col.quantity()) {
                case STORAGE_MODULUS -> { return FitTarget.STORAGE_MODULUS; }
                case LOSS_MODULUS -> { return FitTarget.LOSS_MODULUS; }
                case COMPLEX_VISCOSITY -> { return FitTarget.COMPLEX_VISCOSITY; }
                case COMPLIANCE -> { return FitTarget.CREEP_COMPLIANCE; }
                case SHEAR_STRESS -> { return FitTarget.RELAXATION_MODULUS; }
                default -> {}
            }
        }
        return FitTarget.RELAXATION_MODULUS;
    }

    private int findXColumnIndex(List<DataColumn> columns, FitTarget target) {
        for (int i = 0; i < columns.size(); i++) {
            DataColumn.PhysicalQuantity q = columns.get(i).quantity();
            if (target == FitTarget.STORAGE_MODULUS || target == FitTarget.LOSS_MODULUS
                    || target == FitTarget.COMPLEX_VISCOSITY) {
                if (q == DataColumn.PhysicalQuantity.FREQUENCY) return i;
            } else {
                if (q == DataColumn.PhysicalQuantity.TIME) return i;
            }
        }
        return 0;
    }

    private int findYColumnIndex(List<DataColumn> columns, FitTarget target) {
        DataColumn.PhysicalQuantity expected = switch (target) {
            case STORAGE_MODULUS -> DataColumn.PhysicalQuantity.STORAGE_MODULUS;
            case LOSS_MODULUS -> DataColumn.PhysicalQuantity.LOSS_MODULUS;
            case COMPLEX_VISCOSITY -> DataColumn.PhysicalQuantity.COMPLEX_VISCOSITY;
            case CREEP_COMPLIANCE -> DataColumn.PhysicalQuantity.COMPLIANCE;
            case RELAXATION_MODULUS -> DataColumn.PhysicalQuantity.SHEAR_STRESS;
        };
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).quantity() == expected) return i;
        }
        return columns.size() > 1 ? 1 : 0;
    }

    private double[] extractColumn(List<double[]> rows, int colIdx) {
        return rows.stream()
                .mapToDouble(row -> row[colIdx])
                .filter(v -> !Double.isNaN(v))
                .toArray();
    }

    private double[] buildInitialGuess(ConstitutiveLaw law, int numberOfBranches) {
        double[] defaults = law.getDefaultParameters();
        int paramCount = law.getParameterCount(numberOfBranches);
        if (defaults.length >= paramCount) return defaults;

        // Extend for Prony series with multiple branches
        double[] guess = new double[paramCount];
        System.arraycopy(defaults, 0, guess, 0, defaults.length);
        for (int i = defaults.length; i < paramCount; i += 2) {
            guess[i] = defaults.length > 1 ? defaults[1] / (i + 1) : 100.0;
            if (i + 1 < paramCount) {
                guess[i + 1] = defaults.length > 2 ? defaults[2] * (i + 1) : Math.pow(10, i / 2);
            }
        }
        return guess;
    }

    private double[] buildBounds(ConstitutiveLaw law, int numberOfBranches, boolean lower) {
        double[] base = lower ? law.getLowerBounds() : law.getUpperBounds();
        int paramCount = law.getParameterCount(numberOfBranches);
        if (base.length >= paramCount) return base;

        double[] bounds = new double[paramCount];
        System.arraycopy(base, 0, bounds, 0, base.length);
        for (int i = base.length; i < paramCount; i++) {
            bounds[i] = lower ? base[Math.min(i % base.length, base.length - 1)]
                    : base[Math.min(i % base.length, base.length - 1)];
        }
        return bounds;
    }
}
