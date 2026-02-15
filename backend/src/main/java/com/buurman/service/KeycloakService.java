package com.buurman.service;

import com.buurman.config.models.KeycloakProperties;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.ExternalServiceException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

@Service
public class KeycloakService {

    private final Keycloak keycloak;
    private final String realm;
    private final String backofficeRealm;

    public KeycloakService(Keycloak keycloak, KeycloakProperties keycloakProperties) {
        this.keycloak = keycloak;
        this.realm = keycloakProperties.realm();
        this.backofficeRealm = keycloakProperties.backofficeRealm();
    }

    // --- App realm user management ---

    public String createUser(String email, String firstName, String lastName, String password) {
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        UserRepresentation user = new UserRepresentation();
        user.setEmail(email);
        user.setUsername(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(false);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(PASSWORD);
        credential.setValue(password);
        user.setCredentials(List.of(credential));

        Response response = usersResource.create(user);

        if (response.getStatus() == HttpStatus.CONFLICT.value()) {
            response.close();
            throw new BusinessRuleException("A user with this email or username already exists");
        }

        if (response.getStatus() != HttpStatus.CREATED.value()) {
            response.close();
            throw new ExternalServiceException("Failed to create user in Keycloak: " + response.getStatusInfo());
        }

        String location = response.getLocation().getPath();
        String userId = location.substring(location.lastIndexOf('/') + 1);

        response.close();
        return userId;
    }

    public void deleteUser(String keycloakUserId) {
        deleteUserInRealm(keycloakUserId, realm);
    }

    public void disableUser(String keycloakUserId) {
        setUserEnabled(keycloakUserId, realm, false);
    }

    public void enableUser(String keycloakUserId) {
        setUserEnabled(keycloakUserId, realm, true);
    }

    public void sendPasswordResetEmail(String keycloakUserId) {
        try {
            keycloak.realm(realm).users().get(keycloakUserId).executeActionsEmail(List.of("UPDATE_PASSWORD"));
        } catch (NotFoundException e) {
            throw new com.buurman.exception.NotFoundException("Keycloak user not found: " + keycloakUserId);
        }
    }

    // --- Backoffice realm user management (Buurmies) ---

    public List<UserRepresentation> listRealmUsers(String search, int first, int max) {
        UsersResource usersResource = keycloak.realm(backofficeRealm).users();
        if (search != null && !search.isBlank()) {
            return usersResource.search(search, first, max);
        }
        return usersResource.list(first, max);
    }

    public int countRealmUsers(String search) {
        UsersResource usersResource = keycloak.realm(backofficeRealm).users();
        if (search != null && !search.isBlank()) {
            return usersResource.count(search);
        }
        return usersResource.count();
    }

    public UserRepresentation getRealmUser(String keycloakUserId) {
        try {
            return keycloak.realm(backofficeRealm).users().get(keycloakUserId).toRepresentation();
        } catch (NotFoundException e) {
            throw new com.buurman.exception.NotFoundException("Keycloak user not found: " + keycloakUserId);
        }
    }

    public List<UserSessionRepresentation> getUserSessions(String keycloakUserId) {
        try {
            return keycloak.realm(backofficeRealm).users().get(keycloakUserId).getUserSessions();
        } catch (NotFoundException e) {
            return List.of();
        }
    }

    public void addRequiredUserAction(String keycloakUserId, String action) {
        try {
            var userResource = keycloak.realm(backofficeRealm).users().get(keycloakUserId);
            UserRepresentation user = userResource.toRepresentation();
            List<String> actions = new ArrayList<>(user.getRequiredActions() != null ? user.getRequiredActions() : List.of());
            if (!actions.contains(action)) {
                actions.add(action);
            }
            user.setRequiredActions(actions);
            userResource.update(user);
        } catch (NotFoundException e) {
            throw new com.buurman.exception.NotFoundException("Keycloak user not found: " + keycloakUserId);
        }
    }

    public void removeRequiredUserAction(String keycloakUserId, String action) {
        try {
            var userResource = keycloak.realm(backofficeRealm).users().get(keycloakUserId);
            UserRepresentation user = userResource.toRepresentation();
            List<String> actions = new ArrayList<>(user.getRequiredActions() != null ? user.getRequiredActions() : List.of());
            actions.remove(action);
            user.setRequiredActions(actions);
            userResource.update(user);
        } catch (NotFoundException e) {
            throw new com.buurman.exception.NotFoundException("Keycloak user not found: " + keycloakUserId);
        }
    }

    public String createRealmUser(String email, String username, String firstName, String lastName,
                                  String password, boolean temporary) {
        UsersResource usersResource = keycloak.realm(backofficeRealm).users();

        UserRepresentation user = new UserRepresentation();
        user.setEmail(email);
        user.setUsername(username != null && !username.isBlank() ? username : email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(false);

        if (password != null && !password.isBlank()) {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setTemporary(temporary);
            credential.setType(PASSWORD);
            credential.setValue(password);
            user.setCredentials(List.of(credential));
        }

        Response response = usersResource.create(user);

        if (response.getStatus() == HttpStatus.CONFLICT.value()) {
            response.close();
            throw new BusinessRuleException("A user with this email or username already exists");
        }

        if (response.getStatus() != HttpStatus.CREATED.value()) {
            response.close();
            throw new ExternalServiceException("Failed to create user in Keycloak: " + response.getStatusInfo());
        }

        String location = response.getLocation().getPath();
        String userId = location.substring(location.lastIndexOf('/') + 1);
        response.close();
        return userId;
    }

    public void disableBackofficeUser(String keycloakUserId) {
        setUserEnabled(keycloakUserId, backofficeRealm, false);
    }

    public void enableBackofficeUser(String keycloakUserId) {
        setUserEnabled(keycloakUserId, backofficeRealm, true);
    }

    public void deleteBackofficeUser(String keycloakUserId) {
        deleteUserInRealm(keycloakUserId, backofficeRealm);
    }

    public void verifyBackofficeUser(String keycloakUserId) {
        setEmailVerified(keycloakUserId, backofficeRealm, true);
    }

    public void unverifyBackofficeUser(String keycloakUserId) {
        setEmailVerified(keycloakUserId, backofficeRealm, false);
    }

    public void logoutRealmUser(String keycloakUserId) {
        try {
            keycloak.realm(backofficeRealm).users().get(keycloakUserId).logout();
        } catch (NotFoundException e) {
            throw new com.buurman.exception.NotFoundException("Keycloak user not found: " + keycloakUserId);
        }
    }

    // --- Private helpers ---

    private void setUserEnabled(String keycloakUserId, String targetRealm, boolean enabled) {
        try {
            RealmResource realmResource = keycloak.realm(targetRealm);
            UserRepresentation user = realmResource.users().get(keycloakUserId).toRepresentation();
            user.setEnabled(enabled);
            realmResource.users().get(keycloakUserId).update(user);
        } catch (NotFoundException e) {
            throw new com.buurman.exception.NotFoundException("Keycloak user not found: " + keycloakUserId);
        }
    }

    private void setEmailVerified(String keycloakUserId, String targetRealm, boolean verified) {
        try {
            RealmResource realmResource = keycloak.realm(targetRealm);
            UserRepresentation user = realmResource.users().get(keycloakUserId).toRepresentation();
            user.setEmailVerified(verified);
            realmResource.users().get(keycloakUserId).update(user);
        } catch (NotFoundException e) {
            throw new com.buurman.exception.NotFoundException("Keycloak user not found: " + keycloakUserId);
        }
    }

    private void deleteUserInRealm(String keycloakUserId, String targetRealm) {
        try {
            keycloak.realm(targetRealm).users().delete(keycloakUserId);
        } catch (NotFoundException e) {
            throw new com.buurman.exception.NotFoundException("Keycloak user not found: " + keycloakUserId);
        }
    }
}
