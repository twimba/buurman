package com.buurman.service.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.config.models.KeycloakProperties;
import com.buurman.service.KeycloakService;

@DisplayName("DemoKeycloakSetup")
@ExtendWith(MockitoExtension.class)
class DemoKeycloakSetupTest {

  private static final String REALM = "buurman";

  @Mock private KeycloakService keycloakService;
  @Mock private KeycloakProperties keycloakProperties;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Keycloak keycloak;

  private DemoKeycloakSetup setup;

  @BeforeEach
  void setUp() {
    when(keycloakProperties.realm()).thenReturn(REALM);
    setup = new DemoKeycloakSetup(keycloakService, keycloak, keycloakProperties);
  }

  @Test
  @DisplayName("createUsers reuses existing Keycloak users and keeps their ids")
  void reusesExistingKeycloakUsers() {
    when(keycloak.realm(REALM).users().searchByEmail(anyString(), eq(true)))
        .thenAnswer(
            invocation -> {
              UserRepresentation existing = new UserRepresentation();
              existing.setId("kc-" + invocation.getArgument(0));
              return List.of(existing);
            });
    DemoDataContext ctx = new DemoDataContext();

    setup.createUsers(ctx);

    verify(keycloakService, never()).createUser(anyString(), anyString(), anyString(), anyString());
    verify(keycloakService, never()).deleteUser(anyString());
    for (DemoUsers.DemoUser user : DemoUsers.ALL_USERS) {
      assertThat(ctx.getKeycloakIds()).containsEntry(user.email(), "kc-" + user.email());
    }
  }
}
