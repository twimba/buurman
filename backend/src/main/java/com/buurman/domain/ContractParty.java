package com.buurman.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractParty {
    private UUID id;
    private String identifier;
    private UUID teamId;
    private UUID contractId;
    private UUID tenantId;
    private ContractPartyRole role;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant deletedAt;
}
