package com.buurman.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    private UUID id;
    private String identifier;
    private UUID teamId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String taxNumber;
    private String idNumber;
    private String additionalInfo;
    private UUID currentPropertyId;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant deletedAt;
}
