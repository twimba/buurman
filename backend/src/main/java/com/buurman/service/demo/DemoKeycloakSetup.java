package com.buurman.service.demo;

import com.buurman.config.KeycloakProperties;
import com.buurman.service.KeycloakService;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DemoKeycloakSetup {

    private static final Logger log = LoggerFactory.getLogger(DemoKeycloakSetup.class);
    private static final String API_ACCESS_ROLE = "API_ACCESS";

    private final KeycloakService keycloakService;
    private final Keycloak keycloak;
    private final String realm;

    public DemoKeycloakSetup(KeycloakService keycloakService, Keycloak keycloak,
                              KeycloakProperties keycloakProperties) {
        this.keycloakService = keycloakService;
        this.keycloak = keycloak;
        this.realm = keycloakProperties.realm();
    }

    public void createUsers(DemoDataContext ctx) {
        ensureRealmRoleExists(API_ACCESS_ROLE);

        for (DemoUsers.DemoUser user : DemoUsers.ALL_USERS) {
            try {
                String keycloakId = findExistingKeycloakUser(user.email());
                if (keycloakId == null) {
                    keycloakId = keycloakService.createUser(
                            user.email(), user.firstName(), user.lastName(), user.password());
                    log.info("Created Keycloak user: {}", user.email());
                } else {
                    log.info("Keycloak user already exists: {}", user.email());
                }
                ctx.getKeycloakIds().put(user.email(), keycloakId);

                // Assign API_ACCESS realm role to all demo users
                assignRealmRole(keycloakId, API_ACCESS_ROLE);

            } catch (Exception e) {
                log.error("Failed to create Keycloak user: {}", user.email(), e);
                throw new RuntimeException("Failed to create Keycloak user: " + user.email(), e);
            }
        }
    }

    public void deleteUsers(DemoDataContext ctx) {
        for (DemoUsers.DemoUser user : DemoUsers.ALL_USERS) {
            try {
                String keycloakId = findExistingKeycloakUser(user.email());
                if (keycloakId != null) {
                    keycloakService.deleteUser(keycloakId);
                    log.info("Deleted Keycloak user: {}", user.email());
                }
            } catch (Exception e) {
                log.warn("Failed to delete Keycloak user: {}", user.email(), e);
            }
        }
    }

    private void ensureRealmRoleExists(String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            realmResource.roles().get(roleName).toRepresentation();
            log.debug("Realm role '{}' already exists", roleName);
        } catch (Exception e) {
            try {
                RoleRepresentation role = new RoleRepresentation();
                role.setName(roleName);
                role.setDescription("Access to Swagger API documentation");
                keycloak.realm(realm).roles().create(role);
                log.info("Created realm role: {}", roleName);
            } catch (Exception createEx) {
                log.warn("Could not create realm role '{}': {}", roleName, createEx.getMessage());
            }
        }
    }

    private void assignRealmRole(String keycloakUserId, String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            realmResource.users().get(keycloakUserId).roles().realmLevel().add(List.of(role));
            log.debug("Assigned role '{}' to user {}", roleName, keycloakUserId);
        } catch (Exception e) {
            log.warn("Could not assign role '{}' to user {}: {}", roleName, keycloakUserId, e.getMessage());
        }
    }

    public void logoutDemoUser() {
        try {
            String keycloakId = findExistingKeycloakUser(DemoUsers.DEMO_USER.email());
            if (keycloakId != null) {
                keycloak.realm(realm).users().get(keycloakId).logout();
                log.info("Logged out all sessions for demo user: {}", DemoUsers.DEMO_USER.email());
            }
        } catch (Exception e) {
            log.warn("Failed to logout demo user: {}", e.getMessage());
        }
    }

    private String findExistingKeycloakUser(String email) {
        try {
            List<UserRepresentation> users = keycloak.realm(realm).users()
                    .searchByEmail(email, true);
            if (users != null && !users.isEmpty()) {
                return users.get(0).getId();
            }
        } catch (Exception e) {
            log.debug("Could not search for Keycloak user: {}", email, e);
        }
        return null;
    }
}
