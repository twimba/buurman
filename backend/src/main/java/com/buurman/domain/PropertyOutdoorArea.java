package com.buurman.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
public class PropertyOutdoorArea {

    private UUID id;
    private String identifier;
    private UUID propertyId;
    private UUID teamId;
    private String type;
    private BigDecimal areaValue;
    private String areaUnit;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant deletedAt;
}
