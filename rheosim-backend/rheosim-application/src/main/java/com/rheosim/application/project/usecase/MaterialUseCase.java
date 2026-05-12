package com.rheosim.application.project.usecase;

import com.rheosim.application.project.dto.*;
import com.rheosim.domain.project.model.*;
import com.rheosim.domain.project.port.MaterialRepository;
import com.rheosim.domain.project.port.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MaterialUseCase {

    private final MaterialRepository materialRepository;
    private final ProjectRepository projectRepository;

    public MaterialUseCase(MaterialRepository materialRepository, ProjectRepository projectRepository) {
        this.materialRepository = materialRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public MaterialResponse createMaterial(UUID projectId, CreateMaterialRequest request, UUID ownerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (!project.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("Access denied to project: " + projectId);
        }

        MaterialFamily family = MaterialFamily.valueOf(request.family().toUpperCase());

        Material material = Material.builder()
                .name(request.name())
                .grade(request.grade())
                .family(family)
                .supplier(request.supplier())
                .notes(request.notes())
                .projectId(projectId)
                .build();

        material = materialRepository.save(material);

        project.addMaterial(material.getId());
        projectRepository.save(project);

        return toResponse(material);
    }

    @Transactional(readOnly = true)
    public MaterialResponse getMaterial(UUID materialId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
        return toResponse(material);
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> listMaterialsByProject(UUID projectId) {
        return materialRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MaterialResponse updateMaterialModel(UUID materialId, UpdateMaterialModelRequest request, UUID ownerId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));

        Project project = projectRepository.findById(material.getProjectId())
                .orElseThrow(() -> new IllegalStateException("Associated project not found"));

        if (!project.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("Access denied");
        }

        ConstitutiveModelType modelType = ConstitutiveModelType.valueOf(request.type().toUpperCase());

        List<MaterialModel.PronyBranch> branches = request.branches() != null
                ? request.branches().stream()
                    .map(b -> new MaterialModel.PronyBranch(b.modulus(), b.relaxationTime()))
                    .toList()
                : List.of();

        MaterialModel newModel = new MaterialModel(
                modelType,
                branches.size(),
                request.equilibriumModulus(),
                branches,
                request.referenceTemperatureK()
        );

        material.updateModel(newModel);
        material = materialRepository.save(material);

        return toResponse(material);
    }

    @Transactional
    public void deleteMaterial(UUID materialId, UUID ownerId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));

        Project project = projectRepository.findById(material.getProjectId())
                .orElseThrow(() -> new IllegalStateException("Associated project not found"));

        if (!project.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("Access denied");
        }

        project.removeMaterial(materialId);
        projectRepository.save(project);
        materialRepository.deleteById(materialId);
    }

    private MaterialResponse toResponse(Material material) {
        MaterialModelResponse modelResponse = null;
        if (material.getModel() != null) {
            MaterialModel m = material.getModel();
            List<MaterialModelResponse.BranchResponse> branchResponses = m.branches() != null
                    ? m.branches().stream()
                        .map(b -> new MaterialModelResponse.BranchResponse(b.modulus(), b.relaxationTime()))
                        .toList()
                    : List.of();

            modelResponse = new MaterialModelResponse(
                    m.type().name(),
                    m.numberOfBranches(),
                    m.equilibriumModulus(),
                    branchResponses,
                    m.referenceTemperatureK()
            );
        }

        return new MaterialResponse(
                material.getId(),
                material.getName(),
                material.getGrade(),
                material.getFamily().name(),
                material.getSupplier(),
                material.getNotes(),
                material.getProjectId(),
                material.getVersion(),
                modelResponse,
                material.getCreatedAt(),
                material.getUpdatedAt()
        );
    }
}
