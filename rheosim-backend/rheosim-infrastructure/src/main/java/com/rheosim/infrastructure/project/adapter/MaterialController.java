package com.rheosim.infrastructure.project.adapter;

import com.rheosim.application.project.dto.*;
import com.rheosim.application.project.usecase.MaterialUseCase;
import com.rheosim.domain.identity.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/projects/{projectId}/materials")
@Tag(name = "Materials", description = "Material management within projects")
@SecurityRequirement(name = "bearerAuth")
public class MaterialController {

    private final MaterialUseCase materialUseCase;

    public MaterialController(MaterialUseCase materialUseCase) {
        this.materialUseCase = materialUseCase;
    }

    @PostMapping
    @Operation(summary = "Add a material to a project")
    public ResponseEntity<MaterialResponse> createMaterial(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateMaterialRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        MaterialResponse response = materialUseCase.createMaterial(projectId, request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all materials in a project")
    public ResponseEntity<List<MaterialResponse>> listMaterials(@PathVariable UUID projectId) {
        List<MaterialResponse> materials = materialUseCase.listMaterialsByProject(projectId);
        return ResponseEntity.ok(materials);
    }

    @GetMapping("/{materialId}")
    @Operation(summary = "Get material by ID")
    public ResponseEntity<MaterialResponse> getMaterial(
            @PathVariable UUID projectId,
            @PathVariable UUID materialId
    ) {
        MaterialResponse response = materialUseCase.getMaterial(materialId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{materialId}/model")
    @Operation(summary = "Update material constitutive model parameters")
    public ResponseEntity<MaterialResponse> updateMaterialModel(
            @PathVariable UUID projectId,
            @PathVariable UUID materialId,
            @Valid @RequestBody UpdateMaterialModelRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        MaterialResponse response = materialUseCase.updateMaterialModel(materialId, request, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{materialId}")
    @Operation(summary = "Remove a material from a project")
    public ResponseEntity<Void> deleteMaterial(
            @PathVariable UUID projectId,
            @PathVariable UUID materialId,
            @AuthenticationPrincipal User currentUser
    ) {
        materialUseCase.deleteMaterial(materialId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
