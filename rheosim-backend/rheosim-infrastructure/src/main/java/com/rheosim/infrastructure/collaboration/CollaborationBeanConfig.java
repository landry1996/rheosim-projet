package com.rheosim.infrastructure.collaboration;

import com.rheosim.application.collaboration.OrganizationUseCase;
import com.rheosim.application.collaboration.ProjectCollaborationUseCase;
import com.rheosim.domain.collaboration.port.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CollaborationBeanConfig {

    @Bean
    public OrganizationUseCase organizationUseCase(OrganizationRepository organizationRepository,
                                                    OrganizationMemberRepository memberRepository,
                                                    InvitationRepository invitationRepository,
                                                    AuditEventRepository auditRepository) {
        return new OrganizationUseCase(organizationRepository, memberRepository, invitationRepository, auditRepository);
    }

    @Bean
    public ProjectCollaborationUseCase projectCollaborationUseCase(ProjectCollaboratorRepository collaboratorRepository,
                                                                    AuditEventRepository auditRepository) {
        return new ProjectCollaborationUseCase(collaboratorRepository, auditRepository);
    }
}
