package com.rheosim.domain.project.model;

import com.rheosim.domain.shared.BaseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Project extends BaseEntity {

    private String name;
    private String description;
    private UUID ownerId;
    private ProjectStatus status;
    private List<UUID> materialIds;

    private Project() {
        super();
        this.materialIds = new ArrayList<>();
    }

    private Project(Builder builder) {
        super();
        this.name = builder.name;
        this.description = builder.description;
        this.ownerId = builder.ownerId;
        this.status = builder.status;
        this.materialIds = builder.materialIds != null ? builder.materialIds : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void activate() {
        this.status = ProjectStatus.ACTIVE;
        markUpdated();
    }

    public void archive() {
        this.status = ProjectStatus.ARCHIVED;
        markUpdated();
    }

    public void addMaterial(UUID materialId) {
        if (!this.materialIds.contains(materialId)) {
            this.materialIds.add(materialId);
            markUpdated();
        }
    }

    public void removeMaterial(UUID materialId) {
        this.materialIds.remove(materialId);
        markUpdated();
    }

    public void updateDetails(String name, String description) {
        if (name != null && !name.isBlank()) this.name = name;
        if (description != null) this.description = description;
        markUpdated();
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public UUID getOwnerId() { return ownerId; }
    public ProjectStatus getStatus() { return status; }
    public List<UUID> getMaterialIds() { return Collections.unmodifiableList(materialIds); }

    // Setters for reconstruction
    public void setId(UUID id) { super.setId(id); }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public void setStatus(ProjectStatus status) { this.status = status; }
    public void setMaterialIds(List<UUID> materialIds) { this.materialIds = materialIds != null ? materialIds : new ArrayList<>(); }

    public static class Builder {
        private String name;
        private String description;
        private UUID ownerId;
        private ProjectStatus status = ProjectStatus.DRAFT;
        private List<UUID> materialIds;

        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder ownerId(UUID ownerId) { this.ownerId = ownerId; return this; }
        public Builder status(ProjectStatus status) { this.status = status; return this; }
        public Builder materialIds(List<UUID> materialIds) { this.materialIds = materialIds; return this; }

        public Project build() {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Project name is required");
            }
            if (ownerId == null) {
                throw new IllegalArgumentException("Owner ID is required");
            }
            return new Project(this);
        }
    }
}
