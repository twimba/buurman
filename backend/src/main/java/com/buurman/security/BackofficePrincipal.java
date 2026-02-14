package com.buurman.security;

import lombok.Getter;

import java.security.Principal;

@Getter
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
}
