package com.rheosim.domain.project.model;

import com.rheosim.domain.shared.BaseEntity;

import java.util.UUID;

public class Material extends BaseEntity {

    private String name;
    private String grade;
    private MaterialFamily family;
    private String supplier;
    private MaterialModel model;
    private UUID projectId;
    private String notes;
    private int version;

    private Material() {
        super();
        this.version = 1;
    }

    private Material(Builder builder) {
        super();
        this.name = builder.name;
        this.grade = builder.grade;
        this.family = builder.family;
        this.supplier = builder.supplier;
        this.model = builder.model;
        this.projectId = builder.projectId;
        this.notes = builder.notes;
        this.version = 1;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void updateModel(MaterialModel newModel) {
        this.model = newModel;
        this.version++;
        markUpdated();
    }

    public void updateDetails(String name, String grade, MaterialFamily family, String supplier, String notes) {
        if (name != null && !name.isBlank()) this.name = name;
        if (grade != null) this.grade = grade;
        if (family != null) this.family = family;
        if (supplier != null) this.supplier = supplier;
        if (notes != null) this.notes = notes;
        markUpdated();
    }

    // Getters
    public String getName() { return name; }
    public String getGrade() { return grade; }
    public MaterialFamily getFamily() { return family; }
    public String getSupplier() { return supplier; }
    public MaterialModel getModel() { return model; }
    public UUID getProjectId() { return projectId; }
    public String getNotes() { return notes; }
    public int getVersion() { return version; }

    // Setters for reconstruction
    public void setId(UUID id) { super.setId(id); }
    public void setName(String name) { this.name = name; }
    public void setGrade(String grade) { this.grade = grade; }
    public void setFamily(MaterialFamily family) { this.family = family; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public void setModel(MaterialModel model) { this.model = model; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setVersion(int version) { this.version = version; }

    public static class Builder {
        private String name;
        private String grade;
        private MaterialFamily family;
        private String supplier;
        private MaterialModel model;
        private UUID projectId;
        private String notes;

        public Builder name(String name) { this.name = name; return this; }
        public Builder grade(String grade) { this.grade = grade; return this; }
        public Builder family(MaterialFamily family) { this.family = family; return this; }
        public Builder supplier(String supplier) { this.supplier = supplier; return this; }
        public Builder model(MaterialModel model) { this.model = model; return this; }
        public Builder projectId(UUID projectId) { this.projectId = projectId; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }

        public Material build() {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Material name is required");
            }
            if (family == null) {
                throw new IllegalArgumentException("Material family is required");
            }
            if (projectId == null) {
                throw new IllegalArgumentException("Project ID is required");
            }
            return new Material(this);
        }
    }
}
