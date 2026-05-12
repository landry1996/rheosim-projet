package com.rheosim.infrastructure.project.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rheosim.domain.project.model.*;
import com.rheosim.domain.project.port.MaterialRepository;
import com.rheosim.infrastructure.project.entity.MaterialJpaEntity;
import com.rheosim.infrastructure.project.repository.MaterialJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MaterialRepositoryAdapter implements MaterialRepository {

    private final MaterialJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public MaterialRepositoryAdapter(MaterialJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Material save(Material material) {
        MaterialJpaEntity entity = toJpaEntity(material);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<Material> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Material> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Material> findByFamily(MaterialFamily family) {
        return jpaRepository.findByFamily(family.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private MaterialJpaEntity toJpaEntity(Material material) {
        MaterialJpaEntity entity = new MaterialJpaEntity();
        entity.setId(material.getId());
        entity.setName(material.getName());
        entity.setGrade(material.getGrade());
        entity.setFamily(material.getFamily().name());
        entity.setSupplier(material.getSupplier());
        entity.setNotes(material.getNotes());
        entity.setProjectId(material.getProjectId());
        entity.setVersion(material.getVersion());
        entity.setCreatedAt(material.getCreatedAt());
        entity.setUpdatedAt(material.getUpdatedAt());

        if (material.getModel() != null) {
            entity.setModelType(material.getModel().type().name());
            try {
                entity.setModelData(objectMapper.writeValueAsString(material.getModel()));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize material model", e);
            }
        }

        return entity;
    }

    private Material toDomain(MaterialJpaEntity entity) {
        Material material = Material.builder()
                .name(entity.getName())
                .grade(entity.getGrade())
                .family(MaterialFamily.valueOf(entity.getFamily()))
                .supplier(entity.getSupplier())
                .notes(entity.getNotes())
                .projectId(entity.getProjectId())
                .build();

        material.setId(entity.getId());
        material.setVersion(entity.getVersion());
        material.setCreatedAt(entity.getCreatedAt());
        material.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getModelData() != null && !entity.getModelData().isBlank()) {
            try {
                MaterialModel model = objectMapper.readValue(entity.getModelData(), MaterialModel.class);
                material.setModel(model);
            } catch (JsonProcessingException e) {
                // Log and continue without model if JSON is corrupted
            }
        }

        return material;
    }
}
