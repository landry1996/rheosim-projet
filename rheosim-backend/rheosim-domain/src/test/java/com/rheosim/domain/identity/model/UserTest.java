package com.rheosim.domain.identity.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User Domain Entity")
class UserTest {

    @Test
    @DisplayName("should create user with valid fields")
    void createUser_valid() {
        User user = User.builder()
                .email("elise@acme.com")
                .passwordHash("$2a$12$hash")
                .firstName("Elise")
                .lastName("Durand")
                .organization("ACME R&D")
                .build();

        assertThat(user.getEmail()).isEqualTo("elise@acme.com");
        assertThat(user.getFirstName()).isEqualTo("Elise");
        assertThat(user.getLastName()).isEqualTo("Durand");
        assertThat(user.getFullName()).isEqualTo("Elise Durand");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isLocked()).isFalse();
        assertThat(user.isActive()).isTrue();
        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should reject user without email")
    void createUser_noEmail() {
        assertThatThrownBy(() -> User.builder()
                .passwordHash("hash")
                .firstName("Test")
                .lastName("User")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email is required");
    }

    @Test
    @DisplayName("should reject user without password")
    void createUser_noPassword() {
        assertThatThrownBy(() -> User.builder()
                .email("test@test.com")
                .firstName("Test")
                .lastName("User")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password hash is required");
    }

    @Test
    @DisplayName("should assign and remove roles")
    void assignAndRemoveRoles() {
        User user = User.builder()
                .email("test@test.com")
                .passwordHash("hash")
                .firstName("Test")
                .lastName("User")
                .build();

        Role engineer = new Role(RoleName.ENGINEER, "Engineer");
        Role researcher = new Role(RoleName.RESEARCHER, "Researcher");

        user.assignRole(engineer);
        assertThat(user.getRoles()).hasSize(1);

        user.assignRole(researcher);
        assertThat(user.getRoles()).hasSize(2);

        user.removeRole(engineer);
        assertThat(user.getRoles()).hasSize(1);
        assertThat(user.getRoles()).containsExactly(researcher);
    }

    @Test
    @DisplayName("should lock and unlock user")
    void lockUnlock() {
        User user = User.builder()
                .email("test@test.com")
                .passwordHash("hash")
                .firstName("Test")
                .lastName("User")
                .build();

        assertThat(user.isActive()).isTrue();

        user.lock();
        assertThat(user.isLocked()).isTrue();
        assertThat(user.isActive()).isFalse();

        user.unlock();
        assertThat(user.isLocked()).isFalse();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    @DisplayName("should record login timestamp")
    void recordLogin() {
        User user = User.builder()
                .email("test@test.com")
                .passwordHash("hash")
                .firstName("Test")
                .lastName("User")
                .build();

        assertThat(user.getLastLoginAt()).isNull();

        user.recordLogin();
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("disabled user is not active")
    void disabledUser() {
        User user = User.builder()
                .email("test@test.com")
                .passwordHash("hash")
                .firstName("Test")
                .lastName("User")
                .enabled(false)
                .build();

        assertThat(user.isActive()).isFalse();
    }
}
