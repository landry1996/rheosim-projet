package com.rheosim.domain.project.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Project Entity")
class ProjectTest {

    @Test
    @DisplayName("Builder creates project with DRAFT status")
    void builder_shouldCreateProjectWithDraftStatus() {
        Project project = Project.builder()
                .name("Test Project")
                .description("A test")
                .ownerId(UUID.randomUUID())
                .build();

        assertThat(project.getId()).isNotNull();
        assertThat(project.getName()).isEqualTo("Test Project");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.DRAFT);
        assertThat(project.getMaterialIds()).isEmpty();
    }

    @Test
    @DisplayName("Builder fails without name")
    void builder_shouldFailWithoutName() {
        assertThatThrownBy(() -> Project.builder().ownerId(UUID.randomUUID()).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("Builder fails without owner")
    void builder_shouldFailWithoutOwner() {
        assertThatThrownBy(() -> Project.builder().name("Test").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Owner");
    }

    @Test
    @DisplayName("activate transitions to ACTIVE")
    void activate_shouldTransitionToActive() {
        Project project = createTestProject();
        project.activate();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    @DisplayName("archive transitions to ARCHIVED")
    void archive_shouldTransitionToArchived() {
        Project project = createTestProject();
        project.archive();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    @DisplayName("addMaterial adds unique material ID")
    void addMaterial_shouldAddUniqueMaterialId() {
        Project project = createTestProject();
        UUID materialId = UUID.randomUUID();

        project.addMaterial(materialId);
        project.addMaterial(materialId); // duplicate

        assertThat(project.getMaterialIds()).hasSize(1).contains(materialId);
    }

    @Test
    @DisplayName("removeMaterial removes material ID")
    void removeMaterial_shouldRemoveMaterialId() {
        Project project = createTestProject();
        UUID materialId = UUID.randomUUID();
        project.addMaterial(materialId);

        project.removeMaterial(materialId);

        assertThat(project.getMaterialIds()).isEmpty();
    }

    @Test
    @DisplayName("updateDetails updates name and description")
    void updateDetails_shouldUpdateNameAndDescription() {
        Project project = createTestProject();
        project.updateDetails("New Name", "New Desc");

        assertThat(project.getName()).isEqualTo("New Name");
        assertThat(project.getDescription()).isEqualTo("New Desc");
    }

    @Test
    @DisplayName("updateDetails ignores blank name")
    void updateDetails_shouldIgnoreBlankName() {
        Project project = createTestProject();
        project.updateDetails("  ", "New Desc");

        assertThat(project.getName()).isEqualTo("My Project");
        assertThat(project.getDescription()).isEqualTo("New Desc");
    }

    private Project createTestProject() {
        return Project.builder()
                .name("My Project")
                .description("Description")
                .ownerId(UUID.randomUUID())
                .build();
    }
}
