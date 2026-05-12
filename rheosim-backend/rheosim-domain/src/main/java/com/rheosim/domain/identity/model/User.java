package com.rheosim.domain.identity.model;

import com.rheosim.domain.shared.BaseEntity;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class User extends BaseEntity {

    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String organization;
    private boolean enabled;
    private boolean locked;
    private Instant lastLoginAt;
    private Set<Role> roles;

    private User() {
        super();
        this.roles = new HashSet<>();
    }

    private User(Builder builder) {
        super();
        this.email = builder.email;
        this.passwordHash = builder.passwordHash;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.organization = builder.organization;
        this.enabled = builder.enabled;
        this.locked = false;
        this.roles = builder.roles;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void assignRole(Role role) {
        this.roles.add(role);
        markUpdated();
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
        markUpdated();
    }

    public void recordLogin() {
        this.lastLoginAt = Instant.now();
        markUpdated();
    }

    public void lock() {
        this.locked = true;
        markUpdated();
    }

    public void unlock() {
        this.locked = false;
        markUpdated();
    }

    public void enable() {
        this.enabled = true;
        markUpdated();
    }

    public void disable() {
        this.enabled = false;
        markUpdated();
    }

    public boolean isActive() {
        return enabled && !locked;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Getters
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getOrganization() { return organization; }
    public boolean isEnabled() { return enabled; }
    public boolean isLocked() { return locked; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Set<Role> getRoles() { return Collections.unmodifiableSet(roles); }

    // Setters for reconstruction from persistence
    public void setId(UUID id) { super.setId(id); }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setOrganization(String organization) { this.organization = organization; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setRoles(Set<Role> roles) { this.roles = roles != null ? roles : new HashSet<>(); }

    public static class Builder {
        private String email;
        private String passwordHash;
        private String firstName;
        private String lastName;
        private String organization;
        private boolean enabled = true;
        private Set<Role> roles = new HashSet<>();

        public Builder email(String email) { this.email = email; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder firstName(String firstName) { this.firstName = firstName; return this; }
        public Builder lastName(String lastName) { this.lastName = lastName; return this; }
        public Builder organization(String organization) { this.organization = organization; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder roles(Set<Role> roles) { this.roles = roles; return this; }

        public User build() {
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Email is required");
            }
            if (passwordHash == null || passwordHash.isBlank()) {
                throw new IllegalArgumentException("Password hash is required");
            }
            if (firstName == null || firstName.isBlank()) {
                throw new IllegalArgumentException("First name is required");
            }
            if (lastName == null || lastName.isBlank()) {
                throw new IllegalArgumentException("Last name is required");
            }
            return new User(this);
        }
    }
}
