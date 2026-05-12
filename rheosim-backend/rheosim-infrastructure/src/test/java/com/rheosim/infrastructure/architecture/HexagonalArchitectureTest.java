package com.rheosim.infrastructure.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("Hexagonal Architecture Rules")
class HexagonalArchitectureTest {

    private static JavaClasses allClasses;

    @BeforeAll
    static void importClasses() {
        allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.rheosim");
    }

    @Test
    @DisplayName("Domain layer must not depend on application or infrastructure")
    void domain_shouldNotDependOnOuterLayers() {
        noClasses()
                .that().resideInAPackage("com.rheosim.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.rheosim.application..",
                        "com.rheosim.infrastructure.."
                )
                .because("Domain is the innermost layer and must not know about outer layers")
                .check(allClasses);
    }

    @Test
    @DisplayName("Domain layer must not depend on Spring Framework")
    void domain_shouldNotDependOnSpring() {
        noClasses()
                .that().resideInAPackage("com.rheosim.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence.."
                )
                .because("Domain must be framework-agnostic")
                .check(allClasses);
    }

    @Test
    @DisplayName("Application layer must not depend on infrastructure")
    void application_shouldNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAPackage("com.rheosim.application..")
                .should().dependOnClassesThat().resideInAPackage("com.rheosim.infrastructure..")
                .because("Application layer orchestrates domain, it must not know about infrastructure details")
                .check(allClasses);
    }

    @Test
    @DisplayName("Infrastructure adapters named *Adapter should implement a domain port interface")
    void infrastructure_adaptersShouldImplementDomainInterfaces() {
        classes()
                .that().resideInAPackage("..adapter..")
                .and().haveSimpleNameEndingWith("Adapter")
                .should().implement(com.rheosim.domain.identity.port.UserRepository.class)
                .orShould().implement(com.rheosim.domain.identity.port.RoleRepository.class)
                .orShould().implement(com.rheosim.domain.identity.port.RefreshTokenRepository.class)
                .orShould().implement(com.rheosim.domain.identity.port.PasswordEncoder.class)
                .orShould().implement(com.rheosim.domain.identity.port.TokenProvider.class)
                .orShould().implement(com.rheosim.domain.shared.AuditPort.class)
                .orShould().implement(com.rheosim.domain.project.port.ProjectRepository.class)
                .orShould().implement(com.rheosim.domain.project.port.MaterialRepository.class)
                .orShould().implement(com.rheosim.domain.experiment.port.DatasetRepository.class)
                .orShould().implement(com.rheosim.domain.experiment.port.FileStoragePort.class)
                .orShould().implement(com.rheosim.domain.experiment.port.DataParserPort.class)
                .orShould().implement(com.rheosim.domain.experiment.port.DataValidatorPort.class)
                .orShould().implement(com.rheosim.domain.simulation.port.SimulationJobRepository.class)
                .orShould().implement(com.rheosim.domain.simulation.port.ConstitutiveLaw.class)
                .orShould().implement(com.rheosim.domain.simulation.port.ParameterIdentificationPort.class)
                .because("Adapters must implement domain ports (hexagonal architecture)")
                .check(allClasses);
    }

    @Test
    @DisplayName("Controllers must not access JPA repositories directly")
    void controllers_shouldNotAccessRepositoriesDirectly() {
        noClasses()
                .that().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .because("Controllers should use application use cases, not repositories directly")
                .check(allClasses);
    }
}
