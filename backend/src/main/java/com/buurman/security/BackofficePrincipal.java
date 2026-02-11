package com.buurman.security;

import java.security.Principal;

public class BackofficePrincipal implements Principal {
    private final String keycloakId;
    private final String email;
    private final String name;
    private final String role;

    public BackofficePrincipal(String keycloakId, String email, String name, String role) {
        this.keycloakId = keycloakId;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    @Override
    public String getName() { return name; }
    public String getKeycloakId() { return keycloakId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
