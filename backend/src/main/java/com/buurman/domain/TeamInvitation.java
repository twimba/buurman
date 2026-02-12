package com.buurman.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
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
}
