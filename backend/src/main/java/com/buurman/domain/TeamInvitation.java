package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

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
    private Instant emailSentAt;
    private String emailError;
    private String pendingFirstName;
    private String pendingLastName;
    private Instant resentAt;
    private Integer resentCount;

    public TeamInvitation() {
    }

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

    public Instant getEmailSentAt() {
        return emailSentAt;
    }

    public void setEmailSentAt(Instant emailSentAt) {
        this.emailSentAt = emailSentAt;
    }

    public String getEmailError() {
        return emailError;
    }

    public void setEmailError(String emailError) {
        this.emailError = emailError;
    }

    public String getPendingFirstName() {
        return pendingFirstName;
    }

    public void setPendingFirstName(String pendingFirstName) {
        this.pendingFirstName = pendingFirstName;
    }

    public String getPendingLastName() {
        return pendingLastName;
    }

    public void setPendingLastName(String pendingLastName) {
        this.pendingLastName = pendingLastName;
    }

    public Instant getResentAt() {
        return resentAt;
    }

    public void setResentAt(Instant resentAt) {
        this.resentAt = resentAt;
    }

    public Integer getResentCount() {
        return resentCount;
    }

    public void setResentCount(Integer resentCount) {
        this.resentCount = resentCount;
    }
}
