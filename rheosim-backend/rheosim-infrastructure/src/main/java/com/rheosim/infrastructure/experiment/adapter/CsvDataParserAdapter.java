package com.rheosim.infrastructure.experiment.adapter;

import com.rheosim.domain.experiment.model.DataColumn;
import com.rheosim.domain.experiment.port.DataParserPort;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvDataParserAdapter implements DataParserPort {

    @Override
    public boolean supports(String contentType) {
        return "text/csv".equals(contentType)
                || "application/csv".equals(contentType)
                || "text/plain".equals(contentType);
    }

    @Override
    public ParseResult parse(InputStream input, String contentType) {
        try (CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()
                .parse(new InputStreamReader(input, StandardCharsets.UTF_8))) {

            List<String> headers = parser.getHeaderNames();
            List<DataColumn> columns = headers.stream()
                    .map(this::inferColumn)
                    .toList();

            List<double[]> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                double[] row = new double[headers.size()];
                for (int i = 0; i < headers.size(); i++) {
                    String value = record.get(i).trim();
                    row[i] = value.isEmpty() ? Double.NaN : Double.parseDouble(value);
                }
                rows.add(row);
            }

            return new ParseResult(columns, rows, rows.size());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse CSV file", e);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("CSV contains non-numeric values in data rows", e);
        }
    }

    private DataColumn inferColumn(String header) {
        String normalized = header.toLowerCase().trim();
        String unit = extractUnit(header);
        DataColumn.PhysicalQuantity quantity = inferQuantity(normalized);
        return new DataColumn(header, unit, quantity);
    }

    private String extractUnit(String header) {
        int start = header.indexOf('(');
        int end = header.indexOf(')');
        if (start >= 0 && end > start) {
            return header.substring(start + 1, end).trim();
        }
        int bracketStart = header.indexOf('[');
        int bracketEnd = header.indexOf(']');
        if (bracketStart >= 0 && bracketEnd > bracketStart) {
            return header.substring(bracketStart + 1, bracketEnd).trim();
        }
        return "";
    }

    private DataColumn.PhysicalQuantity inferQuantity(String header) {
        if (header.contains("time") || header.contains("temps")) {
            return DataColumn.PhysicalQuantity.TIME;
        } else if (header.contains("freq") || header.contains("omega")) {
            return DataColumn.PhysicalQuantity.FREQUENCY;
        } else if (header.contains("shear_rate") || header.contains("gamma_dot")) {
            return DataColumn.PhysicalQuantity.SHEAR_RATE;
        } else if (header.contains("shear_stress") || header.contains("tau") || header.contains("sigma")) {
            return DataColumn.PhysicalQuantity.SHEAR_STRESS;
        } else if (header.contains("eta*") || header.contains("complex_visc")) {
            return DataColumn.PhysicalQuantity.COMPLEX_VISCOSITY;
        } else if (header.contains("viscosity") || header.contains("eta")) {
            return DataColumn.PhysicalQuantity.VISCOSITY;
        } else if (header.contains("g'") || header.contains("storage") || header.contains("g_prime")) {
            return DataColumn.PhysicalQuantity.STORAGE_MODULUS;
        } else if (header.contains("g\"") || header.contains("loss") || header.contains("g_double")) {
            return DataColumn.PhysicalQuantity.LOSS_MODULUS;
        } else if (header.contains("strain") || header.contains("gamma") || header.contains("deformation")) {
            return DataColumn.PhysicalQuantity.STRAIN;
        } else if (header.contains("temp") || header.contains("t(")) {
            return DataColumn.PhysicalQuantity.TEMPERATURE;
        } else if (header.contains("compliance") || header.contains("j(")) {
            return DataColumn.PhysicalQuantity.COMPLIANCE;
        }
        return DataColumn.PhysicalQuantity.OTHER;
    }
}
