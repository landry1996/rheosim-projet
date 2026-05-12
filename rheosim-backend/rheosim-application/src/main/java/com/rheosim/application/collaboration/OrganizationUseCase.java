package com.rheosim.application.collaboration;

import com.rheosim.domain.collaboration.model.*;
import com.rheosim.domain.collaboration.port.*;

import java.util.List;
import java.util.UUID;

public class OrganizationUseCase {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final InvitationRepository invitationRepository;
    private final AuditEventRepository auditRepository;

    public OrganizationUseCase(OrganizationRepository organizationRepository,
                                OrganizationMemberRepository memberRepository,
                                InvitationRepository invitationRepository,
                                AuditEventRepository auditRepository) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.auditRepository = auditRepository;
    }

    public Organization createOrganization(String name, String slug, UUID ownerUserId) {
        Organization org = Organization.create(name, slug, ownerUserId);
        org = organizationRepository.save(org);

        OrganizationMember ownerMember = OrganizationMember.create(org.id(), ownerUserId, OrganizationRole.OWNER);
        memberRepository.save(ownerMember);

        auditRepository.save(AuditEvent.create(org.id(), ownerUserId, "CREATE", "ORGANIZATION", org.id(), "Organization created: " + name));

        return org;
    }

    public List<Organization> getUserOrganizations(UUID userId) {
        return organizationRepository.findByMemberUserId(userId);
    }

    public Invitation inviteMember(UUID organizationId, String email, OrganizationRole role, UUID invitedByUserId) {
        if (!memberRepository.existsByOrganizationIdAndUserId(organizationId, invitedByUserId)) {
            throw new IllegalStateException("User is not a member of this organization");
        }

        Invitation invitation = Invitation.create(organizationId, email, role, invitedByUserId);
        invitation = invitationRepository.save(invitation);

        auditRepository.save(AuditEvent.create(organizationId, invitedByUserId, "INVITE", "MEMBER", invitation.id(), "Invited " + email + " as " + role));

        return invitation;
    }

    public void acceptInvitation(UUID invitationId, UUID userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        if (invitation.isExpired()) {
            throw new IllegalStateException("Invitation has expired");
        }

        if (invitation.status() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Invitation is no longer pending");
        }

        OrganizationMember member = OrganizationMember.create(invitation.organizationId(), userId, invitation.role());
        memberRepository.save(member);

        Invitation accepted = new Invitation(invitation.id(), invitation.organizationId(), invitation.email(),
                invitation.role(), InvitationStatus.ACCEPTED, invitation.invitedByUserId(), invitation.createdAt(), invitation.expiresAt());
        invitationRepository.save(accepted);

        auditRepository.save(AuditEvent.create(invitation.organizationId(), userId, "ACCEPT_INVITE", "MEMBER", invitationId, "Accepted invitation"));
    }

    public List<OrganizationMember> getMembers(UUID organizationId) {
        return memberRepository.findByOrganizationId(organizationId);
    }

    public void removeMember(UUID organizationId, UUID userId, UUID removedByUserId) {
        memberRepository.deleteByOrganizationIdAndUserId(organizationId, userId);
        auditRepository.save(AuditEvent.create(organizationId, removedByUserId, "REMOVE_MEMBER", "MEMBER", userId, "Member removed"));
    }
}
