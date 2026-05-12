package com.rheosim.infrastructure.simulation.adapter;

import com.rheosim.application.simulation.dto.JobResponse;
import com.rheosim.application.simulation.dto.JobResultResponse;
import com.rheosim.application.simulation.dto.SubmitJobRequest;
import com.rheosim.application.simulation.usecase.SimulationUseCase;
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
@RequestMapping("/v1/simulations")
@Tag(name = "Simulations", description = "Simulation job management and execution")
@SecurityRequirement(name = "bearerAuth")
public class SimulationController {

    private final SimulationUseCase simulationUseCase;

    public SimulationController(SimulationUseCase simulationUseCase) {
        this.simulationUseCase = simulationUseCase;
    }

    @PostMapping
    @Operation(summary = "Submit a new simulation job")
    public ResponseEntity<JobResponse> submitJob(
            @Valid @RequestBody SubmitJobRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        JobResponse response = simulationUseCase.submitJob(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get simulation job status")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID jobId) {
        JobResponse response = simulationUseCase.getJob(jobId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List all simulation jobs for a project")
    public ResponseEntity<List<JobResponse>> listJobsByProject(@PathVariable UUID projectId) {
        List<JobResponse> jobs = simulationUseCase.listJobsByProject(projectId);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{jobId}/result")
    @Operation(summary = "Get simulation result (only if completed)")
    public ResponseEntity<JobResultResponse> getJobResult(@PathVariable UUID jobId) {
        JobResultResponse result = simulationUseCase.getJobResult(jobId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{jobId}/cancel")
    @Operation(summary = "Cancel a queued or running job")
    public ResponseEntity<JobResponse> cancelJob(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal User currentUser
    ) {
        JobResponse response = simulationUseCase.cancelJob(jobId, currentUser.getId());
        return ResponseEntity.ok(response);
    }
}
