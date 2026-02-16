package com.buurman.domain;

import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Amenity {

  private UUID id;
  private String identifier;
  private String name;
  private String category;
  private String icon;
}
