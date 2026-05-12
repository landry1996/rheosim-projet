package com.rheosim.domain.experiment.port;

import com.rheosim.domain.experiment.model.Dataset;
import com.rheosim.domain.experiment.model.DatasetStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetRepository {

    Dataset save(Dataset dataset);

    Optional<Dataset> findById(UUID id);

    List<Dataset> findByProjectId(UUID projectId);

    List<Dataset> findByProjectIdAndStatus(UUID projectId, DatasetStatus status);

    void deleteById(UUID id);
}
