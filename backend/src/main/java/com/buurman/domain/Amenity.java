package com.buurman.domain;

import java.util.UUID;

public class Amenity {

    private UUID id;
    private String identifier;
    private String name;
    private String category;
    private String icon;

    public Amenity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
