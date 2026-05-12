package com.rheosim.application.project.usecase;

import com.rheosim.application.project.dto.CreateProjectRequest;
import com.rheosim.application.project.dto.ProjectResponse;
import com.rheosim.domain.project.model.Project;
import com.rheosim.domain.project.model.ProjectStatus;
import com.rheosim.domain.project.port.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectUseCase")
class ProjectUseCaseTest {

    @Mock
    private ProjectRepository projectRepository;

    private ProjectUseCase projectUseCase;

    private UUID ownerId;

    @BeforeEach
    void setUp() {
        projectUseCase = new ProjectUseCase(projectRepository);
        ownerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("createProject succeeds with valid data")
    void createProject_shouldSucceed() {
        CreateProjectRequest request = new CreateProjectRequest("Polymer Study", "Testing viscosity");
        when(projectRepository.existsByNameAndOwnerId("Polymer Study", ownerId)).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponse response = projectUseCase.createProject(request, ownerId);

        assertThat(response.name()).isEqualTo("Polymer Study");
        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("createProject fails if duplicate name exists")
    void createProject_shouldFailOnDuplicateName() {
        CreateProjectRequest request = new CreateProjectRequest("Existing", "desc");
        when(projectRepository.existsByNameAndOwnerId("Existing", ownerId)).thenReturn(true);

        assertThatThrownBy(() -> projectUseCase.createProject(request, ownerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("getProject returns project for owner")
    void getProject_shouldReturnProjectForOwner() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().name("Test").ownerId(ownerId).build();
        project.setId(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        ProjectResponse response = projectUseCase.getProject(projectId, ownerId);

        assertThat(response.name()).isEqualTo("Test");
    }

    @Test
    @DisplayName("getProject throws for non-owner")
    void getProject_shouldThrowForNonOwner() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().name("Test").ownerId(UUID.randomUUID()).build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectUseCase.getProject(projectId, ownerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("listProjects returns owner's projects")
    void listProjects_shouldReturnOwnerProjects() {
        Project p1 = Project.builder().name("P1").ownerId(ownerId).build();
        Project p2 = Project.builder().name("P2").ownerId(ownerId).build();
        when(projectRepository.findByOwnerId(ownerId)).thenReturn(List.of(p1, p2));

        List<ProjectResponse> projects = projectUseCase.listProjects(ownerId);

        assertThat(projects).hasSize(2);
    }

    @Test
    @DisplayName("archiveProject transitions to ARCHIVED")
    void archiveProject_shouldTransitionToArchived() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().name("Test").ownerId(ownerId).build();
        project.setId(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        projectUseCase.archiveProject(projectId, ownerId);

        verify(projectRepository).save(argThat(p -> p.getStatus() == ProjectStatus.ARCHIVED));
    }

    @Test
    @DisplayName("deleteProject calls deleteById")
    void deleteProject_shouldCallDeleteById() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().name("Test").ownerId(ownerId).build();
        project.setId(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        projectUseCase.deleteProject(projectId, ownerId);

        verify(projectRepository).deleteById(projectId);
    }
}
