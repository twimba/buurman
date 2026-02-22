package com.buurman.domain;

import java.util.List;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@NoArgsConstructor
public class Amenity {

  private UUID id;
  private String identifier;
  private String name;
  private String category;
  private String icon;
  private List<String> applicableCategories;
}
