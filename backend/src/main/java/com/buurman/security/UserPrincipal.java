package com.buurman.security;

import java.security.Principal;
import java.util.UUID;

public class UserPrincipal implements Principal {
    private final UUID userId;
    private final String keycloakId;
    private final String email;
    private final String name;
    private final UUID teamId;      // nullable for users without team membership
    private final String role;      // nullable for users without team membership
    private final boolean isOwner;
    private final boolean emailVerified;

    public UserPrincipal(UUID userId, String keycloakId, String email, String name,
                        UUID teamId, String role, boolean isOwner, boolean emailVerified) {
        this.userId = userId;
        this.keycloakId = keycloakId;
        this.email = email;
        this.name = name;
        this.teamId = teamId;
        this.role = role;
        this.isOwner = isOwner;
        this.emailVerified = emailVerified;
    }

    public UserPrincipal(UUID userId, String keycloakId, String email, String name,
                        UUID teamId, String role, boolean isOwner) {
        this(userId, keycloakId, email, name, teamId, role, isOwner, true);
    }

    public UserPrincipal(UUID userId, String keycloakId, String email, String name,
                        UUID teamId, String role) {
        this(userId, keycloakId, email, name, teamId, role, false, true);
    }

    public UUID getUserId() {
        return userId;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getName() {
        return name;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getRole() {
        return role;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean hasTeam() {
        return teamId != null;
    }
}
