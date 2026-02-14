package com.buurman.service;

import com.buurman.config.models.KeycloakProperties;
import com.buurman.exception.ExternalServiceException;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

@Service
public class KeycloakService {

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakService(Keycloak keycloak, KeycloakProperties keycloakProperties) {
        this.keycloak = keycloak;
        this.realm = keycloakProperties.realm();
    }

    public String createUser(String email, String firstName, String lastName, String password) {
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        // Create user representation
        UserRepresentation user = new UserRepresentation();
        user.setEmail(email);
        user.setUsername(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(false);

        // Set password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(PASSWORD);
        credential.setValue(password);
        user.setCredentials(List.of(credential));

        // Create user
        Response response = usersResource.create(user);

        if (response.getStatus() != 201) {
            throw new ExternalServiceException("Failed to create user in Keycloak: " + response.getStatusInfo());
        }

        // Extract user ID from location header
        String location = response.getLocation().getPath();
        String userId = location.substring(location.lastIndexOf('/') + 1);

        response.close();
        return userId; // This is the Keycloak user ID
    }

    public void deleteUser(String keycloakUserId) {
        RealmResource realmResource = keycloak.realm(realm);
        realmResource.users().delete(keycloakUserId);
    }

    public void disableUser(String keycloakUserId) {
        RealmResource realmResource = keycloak.realm(realm);
        UserRepresentation user = realmResource.users().get(keycloakUserId).toRepresentation();
        user.setEnabled(false);
        realmResource.users().get(keycloakUserId).update(user);
    }

    public void enableUser(String keycloakUserId) {
        RealmResource realmResource = keycloak.realm(realm);
        UserRepresentation user = realmResource.users().get(keycloakUserId).toRepresentation();
        user.setEnabled(true);
        realmResource.users().get(keycloakUserId).update(user);
    }

    public void sendPasswordResetEmail(String keycloakUserId) {
        RealmResource realmResource = keycloak.realm(realm);
        realmResource.users().get(keycloakUserId).executeActionsEmail(List.of("UPDATE_PASSWORD"));
    }
}
