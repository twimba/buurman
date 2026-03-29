package com.buurman.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Deprecated(forRemoval = true)
public enum Segment {
  DEMO_ACCOUNTS("demo_accounts", "Demo team accounts", 0),
  TEAM_ADMINS("team_admins", "Users with TEAM_ADMIN role", 10),
  TEAM_EDITORS("team_editors", "Users with TEAM_EDITOR role", 10),
  TEAM_VIEWERS("team_viewers", "Users with TEAM_VIEWER role", 10),
  OWNERS("owners", "Team owners", 5);

  private final String key;
  private final String description;
  private final int priority;

  Segment(String key, String description, int priority) {
    this.key = key;
    this.description = description;
    this.priority = priority;
  }

  public String getKey() {
    return key;
  }

  public String getDescription() {
    return description;
  }

  public int getPriority() {
    return priority;
  }

  public static List<Segment> all() {
    return List.of(values());
  }

  public static Optional<Segment> fromKey(String key) {
    return Arrays.stream(values()).filter(s -> s.key.equals(key)).findFirst();
  }
}
