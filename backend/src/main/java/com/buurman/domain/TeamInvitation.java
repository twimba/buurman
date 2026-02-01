package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * TeamInvitation domain object (POJO).
 * Represents an invitation to join a team.
 */
public class TeamInvitation {

    private UUID id;
    private UUID teamId;
    private String email;
    private String role;
    private String token;
    private Instant expiresAt;
    private UUID invitedBy;
    private Instant invitedAt;
    private Instant acceptedAt;
    private UUID acceptedBy;


    // Default constructor for MapStruct
    public TeamInvitation() {
    }

    // All-args constructor
    public TeamInvitation(UUID id, UUID teamId, String email, String role, String token,
                          Instant expiresAt, UUID invitedBy, Instant invitedAt,
                          Instant acceptedAt, UUID acceptedBy) {
        this.id = id;
        this.teamId = teamId;
        this.email = email;
        this.role = role;
        this.token = token;
        this.expiresAt = expiresAt;
        this.invitedBy = invitedBy;
        this.invitedAt = invitedAt;
        this.acceptedAt = acceptedAt;
        this.acceptedBy = acceptedBy;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(UUID invitedBy) {
        this.invitedBy = invitedBy;
    }

    public Instant getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(Instant invitedAt) {
        this.invitedAt = invitedAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public UUID getAcceptedBy() {
        return acceptedBy;
    }

    public void setAcceptedBy(UUID acceptedBy) {
        this.acceptedBy = acceptedBy;
    }
}
