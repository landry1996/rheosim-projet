package com.rheosim.domain.privacy.port;

import java.util.UUID;

public interface DataExportPort {

    record ExportData(
            Object userProfile,
            Object projects,
            Object materials,
            Object simulations,
            Object reports,
            Object consents
    ) {}

    ExportData collectUserData(UUID userId);
    byte[] generateExportZip(ExportData data);
    String uploadExport(byte[] zipBytes, UUID userId);
    void deleteExpiredExports();
}
