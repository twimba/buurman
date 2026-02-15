package com.buurman.service.demo;

import java.util.List;
import java.util.Map;

/**
 * Static definition of all demo users and their team memberships.
 */
public final class DemoUsers {

    private DemoUsers() {}

    public record DemoUser(
            String email,
            String firstName,
            String lastName,
            String password,
            Map<String, TeamRole> teamRoles // teamKey -> role
    ) {}

    public record TeamRole(String role, boolean isOwner) {}

    public static final DemoUser DEMO_USER = new DemoUser(
            "demo.user@demo.buurman.io", "Demo", "User", "buurman",
            Map.of(
                    "demo-team", new TeamRole("TEAM_ADMIN", true),
                    "team-alpha", new TeamRole("TEAM_EDITOR", false),
                    "team-beta", new TeamRole("TEAM_VIEWER", false)
            ));

    public static final DemoUser ADMIN_TEAM1 = new DemoUser(
            "admin@demo.buurman.io", "John", "Smith", "admin@demo.buurman.io",
            Map.of("team-alpha", new TeamRole("TEAM_ADMIN", true)));

    public static final DemoUser EDITOR_TEAM1 = new DemoUser(
            "editor@demo.buurman.io", "Jane", "Doe", "editor@demo.buurman.io",
            Map.of("team-alpha", new TeamRole("TEAM_EDITOR", false)));

    public static final DemoUser VIEWER_TEAM1 = new DemoUser(
            "viewer@demo.buurman.io", "Bob", "Wilson", "viewer@demo.buurman.io",
            Map.of("team-alpha", new TeamRole("TEAM_VIEWER", false)));

    public static final DemoUser ADMIN_TEAM2 = new DemoUser(
            "admin.team2@demo.buurman.io", "Alice", "Johnson", "admin.team2@demo.buurman.io",
            Map.of("team-beta", new TeamRole("TEAM_ADMIN", true)));

    public static final DemoUser EDITOR_TEAM2 = new DemoUser(
            "editor.team2@demo.buurman.io", "Charlie", "Brown", "editor.team2@demo.buurman.io",
            Map.of("team-beta", new TeamRole("TEAM_EDITOR", false)));

    public static final DemoUser VIEWER_TEAM2 = new DemoUser(
            "viewer.team2@demo.buurman.io", "Diana", "Prince", "viewer.team2@demo.buurman.io",
            Map.of("team-beta", new TeamRole("TEAM_VIEWER", false)));

    public static final List<DemoUser> ALL_USERS = List.of(
            DEMO_USER, ADMIN_TEAM1, EDITOR_TEAM1, VIEWER_TEAM1,
            ADMIN_TEAM2, EDITOR_TEAM2, VIEWER_TEAM2
    );

    public static final List<String> ALL_EMAILS = ALL_USERS.stream()
            .map(DemoUser::email)
            .toList();
}
