package com.rheosim.domain.experiment.port;

import java.io.InputStream;
import java.util.UUID;

public interface FileStoragePort {

    String store(UUID datasetId, String fileName, InputStream content);

    InputStream load(String storagePath);

    void delete(String storagePath);

    boolean exists(String storagePath);
}
