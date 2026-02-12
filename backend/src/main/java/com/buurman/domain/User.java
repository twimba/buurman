package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

public class User {

    private UUID id;
    private String identifier;
    private String keycloakId;
    private String email;
    private String firstName;
    private String lastName;
    private UUID defaultTeamId;
    private UUID activeTeamId;
    private String phone;
    private Instant emailVerifiedAt;
    private Instant phoneVerifiedAt;
    private Instant disabledAt;
    private Instant createdAt;
    private Instant updatedAt;

    public User() {
    }

    public User(UUID id, String keycloakId, String email, String firstName, String lastName, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.keycloakId = keycloakId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public void setKeycloakId(String keycloakId) {
        this.keycloakId = keycloakId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getDefaultTeamId() {
        return defaultTeamId;
    }

    public void setDefaultTeamId(UUID defaultTeamId) {
        this.defaultTeamId = defaultTeamId;
    }

    public UUID getActiveTeamId() {
        return activeTeamId;
    }

    public void setActiveTeamId(UUID activeTeamId) {
        this.activeTeamId = activeTeamId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(Instant emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public Instant getPhoneVerifiedAt() {
        return phoneVerifiedAt;
    }

    public void setPhoneVerifiedAt(Instant phoneVerifiedAt) {
        this.phoneVerifiedAt = phoneVerifiedAt;
    }

    public Instant getDisabledAt() {
        return disabledAt;
    }

    public void setDisabledAt(Instant disabledAt) {
        this.disabledAt = disabledAt;
    }
}
