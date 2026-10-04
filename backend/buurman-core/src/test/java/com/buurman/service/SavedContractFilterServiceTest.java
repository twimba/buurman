package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.buurman.domain.SavedContractFilter;
import com.buurman.domain.TeamRole;
import com.buurman.dto.request.CreateSavedContractFilterRequest;
import com.buurman.exception.BadRequestException;
import com.buurman.repository.SavedContractFilterRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.SidGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

class SavedContractFilterServiceTest {

  private final SavedContractFilterRepository repository =
      mock(SavedContractFilterRepository.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final SavedContractFilterService service =
      new SavedContractFilterService(repository, objectMapper);

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  private final UserPrincipal principal =
      new UserPrincipal(
          USER_ID,
          "USR0000000000000000000000001",
          "keycloak-id",
          "landlord@example.com",
          "Landlord",
          TEAM_ID,
          "TEM0000000000000000000000001",
          TeamRole.TEAM_ADMIN);

  @Test
  void rejectsCreationBeyondPerUserLimitAndNeverSaves() {
    SavedContractFilter existingFilter =
        SavedContractFilter.builder().teamId(TEAM_ID).userId(USER_ID).build();
    List<SavedContractFilter> existing = java.util.Collections.nCopies(20, existingFilter);
    when(repository.findByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(existing);

    CreateSavedContractFilterRequest request =
        new CreateSavedContractFilterRequest("One too many", Map.of("status", "ACTIVE"));

    assertThatThrownBy(() -> service.create(request, principal))
        .isInstanceOf(BadRequestException.class);

    verify(repository, never()).save(any());
  }

  @Test
  void rejectsOversizedCriteriaAndNeverSaves() {
    when(repository.findByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(List.of());

    // A single oversized value is enough to exceed the serialized-JSON cap.
    Map<String, Object> hugeCriteria = Map.of("notes", "x".repeat(10_000));
    CreateSavedContractFilterRequest request =
        new CreateSavedContractFilterRequest("Too big", hugeCriteria);

    assertThatThrownBy(() -> service.create(request, principal))
        .isInstanceOf(BadRequestException.class);

    verify(repository, never()).save(any());
  }

  @Test
  void acceptsReasonableRequestAndSaves() {
    when(repository.findByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(List.of());
    SavedContractFilter saved =
        SavedContractFilter.builder()
            .teamId(TEAM_ID)
            .userId(USER_ID)
            .name("Ending soon")
            .criteria(Map.of("endingWithinDays", 90))
            .identifier(Optional.of(SidGenerator.newSavedContractFilterId()))
            .createdAt(Instant.now())
            .build();
    when(repository.save(any())).thenReturn(saved);

    CreateSavedContractFilterRequest request =
        new CreateSavedContractFilterRequest("Ending soon", Map.of("endingWithinDays", 90));

    assertThat(service.create(request, principal)).isNotNull();
  }
}
