package com.rheosim.infrastructure.project.adapter;

import com.rheosim.application.project.dto.CreateProjectRequest;
import com.rheosim.application.project.dto.ProjectResponse;
import com.rheosim.application.project.usecase.ProjectUseCase;
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
@RequestMapping("/v1/projects")
@Tag(name = "Projects", description = "Project management")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectUseCase projectUseCase;

    public ProjectController(ProjectUseCase projectUseCase) {
        this.projectUseCase = projectUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new project")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        ProjectResponse response = projectUseCase.createProject(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all projects for the authenticated user")
    public ResponseEntity<List<ProjectResponse>> listProjects(@AuthenticationPrincipal User currentUser) {
        List<ProjectResponse> projects = projectUseCase.listProjects(currentUser.getId());
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal User currentUser
    ) {
        ProjectResponse response = projectUseCase.getProject(projectId, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Update project details")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        ProjectResponse response = projectUseCase.updateProject(
                projectId, currentUser.getId(), request.name(), request.description()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectId}/archive")
    @Operation(summary = "Archive a project")
    public ResponseEntity<Void> archiveProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal User currentUser
    ) {
        projectUseCase.archiveProject(projectId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete a project")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal User currentUser
    ) {
        projectUseCase.deleteProject(projectId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
