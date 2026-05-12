package com.rheosim.infrastructure.collaboration;

import com.rheosim.application.collaboration.OrganizationUseCase;
import com.rheosim.application.collaboration.ProjectCollaborationUseCase;
import com.rheosim.domain.collaboration.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/organizations")
public class CollaborationController {

    private final OrganizationUseCase organizationUseCase;
    private final ProjectCollaborationUseCase projectCollaborationUseCase;

    public CollaborationController(OrganizationUseCase organizationUseCase,
                                    ProjectCollaborationUseCase projectCollaborationUseCase) {
        this.organizationUseCase = organizationUseCase;
        this.projectCollaborationUseCase = projectCollaborationUseCase;
    }

    @PostMapping
    public ResponseEntity<Organization> createOrganization(@RequestBody CreateOrganizationRequest request,
                                                            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        Organization org = organizationUseCase.createOrganization(request.name(), request.slug(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(org);
    }

    @GetMapping
    public ResponseEntity<List<Organization>> getUserOrganizations(@AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(organizationUseCase.getUserOrganizations(userId));
    }

    @GetMapping("/{orgId}/members")
    public ResponseEntity<List<OrganizationMember>> getMembers(@PathVariable UUID orgId) {
        return ResponseEntity.ok(organizationUseCase.getMembers(orgId));
    }

    @PostMapping("/{orgId}/invitations")
    public ResponseEntity<Invitation> inviteMember(@PathVariable UUID orgId,
                                                    @RequestBody InviteRequest request,
                                                    @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        Invitation invitation = organizationUseCase.inviteMember(orgId, request.email(), request.role(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(invitation);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<Void> acceptInvitation(@PathVariable UUID invitationId,
                                                  @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        organizationUseCase.acceptInvitation(invitationId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{orgId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID orgId,
                                              @PathVariable UUID userId,
                                              @AuthenticationPrincipal UserDetails user) {
        UUID removedBy = UUID.fromString(user.getUsername());
        organizationUseCase.removeMember(orgId, userId, removedBy);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orgId}/projects/{projectId}/collaborators")
    public ResponseEntity<ProjectCollaborator> addCollaborator(@PathVariable UUID orgId,
                                                                @PathVariable UUID projectId,
                                                                @RequestBody AddCollaboratorRequest request,
                                                                @AuthenticationPrincipal UserDetails user) {
        UUID addedBy = UUID.fromString(user.getUsername());
        ProjectCollaborator collab = projectCollaborationUseCase.addCollaborator(projectId, request.userId(), request.role(), addedBy, orgId);
        return ResponseEntity.status(HttpStatus.CREATED).body(collab);
    }

    @GetMapping("/{orgId}/projects/{projectId}/collaborators")
    public ResponseEntity<List<ProjectCollaborator>> getCollaborators(@PathVariable UUID orgId,
                                                                      @PathVariable UUID projectId) {
        return ResponseEntity.ok(projectCollaborationUseCase.getCollaborators(projectId));
    }

    public record CreateOrganizationRequest(String name, String slug) {}
    public record InviteRequest(String email, OrganizationRole role) {}
    public record AddCollaboratorRequest(UUID userId, ProjectRole role) {}
}
