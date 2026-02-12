package com.buurman.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class Amenity {

    private UUID id;
    private String identifier;
    private String name;
    private String category;
    private String icon;
}
