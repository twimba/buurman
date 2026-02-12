package com.buurman.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
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

    public User(UUID id, String keycloakId, String email, String firstName, String lastName, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.keycloakId = keycloakId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
