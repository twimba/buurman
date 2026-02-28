package com.buurman.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Role assigned to a team member determining access level")
public enum TeamRole {
  TEAM_ADMIN("Administrator"),
  TEAM_EDITOR("Editor"),
  TEAM_VIEWER("Viewer");

  private final String displayName;

  TeamRole(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  /** Returns the Spring Security role string (with ROLE_ prefix). */
  public String toSpringRole() {
    return "ROLE_" + name();
  }

  /** Returns the role hierarchy string for Spring Security configuration. */
  public static String hierarchy() {
    return TEAM_ADMIN.toSpringRole()
        + " > "
        + TEAM_EDITOR.toSpringRole()
        + " > "
        + TEAM_VIEWER.toSpringRole();
  }
}
