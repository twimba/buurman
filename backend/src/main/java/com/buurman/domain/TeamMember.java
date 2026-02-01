package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

public class TeamMember {

    private UUID id;
    private UUID teamId;
    private UUID userId;
    private String role;
    private Instant invitedAt;
    private UUID invitedBy;
    private Instant joinedAt;

    public TeamMember() {
    }

    public TeamMember(UUID id, UUID teamId, UUID userId, String role, Instant invitedAt, UUID invitedBy, Instant joinedAt) {
        this.id = id;
        this.teamId = teamId;
        this.userId = userId;
        this.role = role;
        this.invitedAt = invitedAt;
        this.invitedBy = invitedBy;
        this.joinedAt = joinedAt;
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(Instant invitedAt) {
        this.invitedAt = invitedAt;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(UUID invitedBy) {
        this.invitedBy = invitedBy;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}
