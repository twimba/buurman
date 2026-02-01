package com.buurman.security;

import java.security.Principal;
import java.util.UUID;

public class UserPrincipal implements Principal {
    private final UUID userId;
    private final String keycloakId;
    private final String email;
    private final String name;
    private final UUID teamId;
    private final String role;

    public UserPrincipal(UUID userId, String keycloakId, String email, String name,
                        UUID teamId, String role) {
        this.userId = userId;
        this.keycloakId = keycloakId;
        this.email = email;
        this.name = name;
        this.teamId = teamId;
        this.role = role;
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
}
