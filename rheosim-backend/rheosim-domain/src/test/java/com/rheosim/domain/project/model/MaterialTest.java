package com.rheosim.domain.project.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Material Entity")
class MaterialTest {

    @Test
    @DisplayName("Builder creates material with version 1")
    void builder_shouldCreateMaterialWithVersion1() {
        Material material = Material.builder()
                .name("HDPE 4040")
                .family(MaterialFamily.THERMOPLASTIC)
                .projectId(UUID.randomUUID())
                .build();

        assertThat(material.getId()).isNotNull();
        assertThat(material.getName()).isEqualTo("HDPE 4040");
        assertThat(material.getFamily()).isEqualTo(MaterialFamily.THERMOPLASTIC);
        assertThat(material.getVersion()).isEqualTo(1);
        assertThat(material.getModel()).isNull();
    }

    @Test
    @DisplayName("Builder fails without name")
    void builder_shouldFailWithoutName() {
        assertThatThrownBy(() -> Material.builder()
                .family(MaterialFamily.ELASTOMER)
                .projectId(UUID.randomUUID())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("Builder fails without family")
    void builder_shouldFailWithoutFamily() {
        assertThatThrownBy(() -> Material.builder()
                .name("Test")
                .projectId(UUID.randomUUID())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("family");
    }

    @Test
    @DisplayName("updateModel increments version")
    void updateModel_shouldIncrementVersion() {
        Material material = createTestMaterial();
        MaterialModel model = new MaterialModel(
                ConstitutiveModelType.MAXWELL, 1, 100.0, List.of(), 298.15
        );

        material.updateModel(model);

        assertThat(material.getVersion()).isEqualTo(2);
        assertThat(material.getModel()).isEqualTo(model);
    }

    @Test
    @DisplayName("updateModel increments version each time")
    void updateModel_shouldIncrementVersionEachTime() {
        Material material = createTestMaterial();
        MaterialModel model1 = new MaterialModel(ConstitutiveModelType.MAXWELL, 1, 100.0, List.of(), 298.15);
        MaterialModel model2 = new MaterialModel(ConstitutiveModelType.PRONY, 3, 50.0,
                List.of(new MaterialModel.PronyBranch(500.0, 0.1)), 300.0);

        material.updateModel(model1);
        material.updateModel(model2);

        assertThat(material.getVersion()).isEqualTo(3);
        assertThat(material.getModel()).isEqualTo(model2);
    }

    @Test
    @DisplayName("updateDetails updates non-null fields only")
    void updateDetails_shouldUpdateNonNullFields() {
        Material material = createTestMaterial();
        material.updateDetails("NewName", null, null, "NewSupplier", null);

        assertThat(material.getName()).isEqualTo("NewName");
        assertThat(material.getGrade()).isEqualTo("High");
        assertThat(material.getSupplier()).isEqualTo("NewSupplier");
    }

    private Material createTestMaterial() {
        return Material.builder()
                .name("SBR Rubber")
                .grade("High")
                .family(MaterialFamily.ELASTOMER)
                .supplier("BASF")
                .projectId(UUID.randomUUID())
                .build();
    }
}
