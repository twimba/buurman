package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.TEAMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import com.buurman.config.models.DemoDataProperties;
import com.buurman.service.S3StorageService;

@DisplayName("DemoDataService")
@ExtendWith(MockitoExtension.class)
class DemoDataServiceTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DSLContext dsl;

  @Mock private DemoDataProperties properties;
  @Mock private DemoKeycloakSetup keycloakSetup;
  @Mock private DemoTeamGenerator teamGenerator;
  @Mock private DemoUserGenerator userGenerator;
  @Mock private DemoTeamMemberGenerator teamMemberGenerator;
  @Mock private DemoPropertyGenerator propertyGenerator;
  @Mock private DemoUnitGenerator unitGenerator;
  @Mock private DemoContactGenerator contactGenerator;
  @Mock private DemoContactNoteGenerator contactNoteGenerator;
  @Mock private DemoContactRelationshipGenerator contactRelationshipGenerator;
  @Mock private DemoContactTagGenerator contactTagGenerator;
  @Mock private DemoContractGenerator contractGenerator;
  @Mock private ContractExtensionDemoDataGenerator contractExtensionGenerator;
  @Mock private DemoPaymentGenerator paymentGenerator;
  @Mock private DemoRentCollectionGenerator rentCollectionGenerator;
  @Mock private DemoExpenseGenerator expenseGenerator;
  @Mock private DemoPaymentInstructionGenerator paymentInstructionGenerator;
  @Mock private DemoPhotoGenerator photoGenerator;
  @Mock private DemoNotificationGenerator notificationGenerator;
  @Mock private DemoDocumentGenerator documentGenerator;
  @Mock private DemoFinancingPaymentGenerator financingPaymentGenerator;
  @Mock private DemoAuditLogGenerator auditLogGenerator;
  @Mock private S3StorageService s3StorageService;
  @Mock private PlatformTransactionManager transactionManager;
  @Mock private Clock clock;

  @InjectMocks private DemoDataService service;

  @BeforeEach
  void noExistingDemoTeams() {
    List<UUID> none = List.of();
    when(dsl.select(TEAMS.ID).from(TEAMS).where(any(Condition.class)).fetch(TEAMS.ID))
        .thenReturn(none);
  }

  @Test
  @DisplayName("regeneration keeps Keycloak users and reuses their ids for the DB users")
  void regenerationKeepsKeycloakUsers() {
    String existingKeycloakId = "kc-existing-demo-user";
    doAnswer(
            invocation -> {
              DemoDataContext ctx = invocation.getArgument(0);
              ctx.getKeycloakIds().put(DemoUsers.DEMO_USER.email(), existingKeycloakId);
              return null;
            })
        .when(keycloakSetup)
        .createUsers(any());
    AtomicReference<String> idSeenByUserGenerator = new AtomicReference<>();
    doAnswer(
            invocation -> {
              DemoDataContext ctx = invocation.getArgument(0);
              idSeenByUserGenerator.set(ctx.getKeycloakIds().get(DemoUsers.DEMO_USER.email()));
              return null;
            })
        .when(userGenerator)
        .generate(any());

    service.generate();

    verify(keycloakSetup, never()).deleteUsers(any());
    InOrder order = inOrder(keycloakSetup, userGenerator);
    order.verify(keycloakSetup).createUsers(any());
    order.verify(userGenerator).generate(any());
    order.verify(keycloakSetup).logoutDemoUser();
    assertThat(idSeenByUserGenerator.get()).isEqualTo(existingKeycloakId);
  }

  @Test
  @DisplayName("scheduled regeneration does not delete Keycloak users")
  void scheduledRegenerationKeepsKeycloakUsers() {
    when(properties.enabled()).thenReturn(true);

    service.scheduledRegenerate();

    verify(keycloakSetup).createUsers(any());
    verify(keycloakSetup, never()).deleteUsers(any());
  }

  @Test
  @DisplayName("explicit cleanup still deletes the Keycloak users")
  void explicitCleanupDeletesKeycloakUsers() {
    service.cleanup();

    verify(keycloakSetup).deleteUsers(any());
    verify(keycloakSetup, never()).createUsers(any());
  }
}
