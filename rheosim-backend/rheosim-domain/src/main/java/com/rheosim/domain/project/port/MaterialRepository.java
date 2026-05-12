package com.rheosim.domain.project.port;

import com.rheosim.domain.project.model.Material;
import com.rheosim.domain.project.model.MaterialFamily;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaterialRepository {

    Material save(Material material);

    Optional<Material> findById(UUID id);

    List<Material> findByProjectId(UUID projectId);

    List<Material> findByFamily(MaterialFamily family);

    void deleteById(UUID id);
}
