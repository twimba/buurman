package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractLeaseClause;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.TeamRole;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.UpdateContractLeaseClausesRequest;
import com.buurman.dto.request.UpdateContractLeaseClausesRequest.ClauseSelection;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.repository.ContractLeaseClauseRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitResidentialDetailsRepository;
import com.buurman.security.UserPrincipal;

class LeaseClauseServiceTest {

  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final ContractLeaseClauseRepository overrideRepository =
      mock(ContractLeaseClauseRepository.class);
  private final LeaseClauseResolver resolver = mock(LeaseClauseResolver.class);
  private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
  private final UnitResidentialDetailsRepository unitDetailsRepository =
      mock(UnitResidentialDetailsRepository.class);
  private final LeaseKindResolver leaseKindResolver = mock(LeaseKindResolver.class);

  private final LeaseClauseService service =
      new LeaseClauseService(
          contractRepository,
          overrideRepository,
          resolver,
          propertyRepository,
          unitDetailsRepository,
          leaseKindResolver);

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final ContractIdentifier CONTRACT_IDENTIFIER =
      ContractIdentifier.of("CNT0000000000000000000000001");

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

  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID UNIT_ID = UUID.randomUUID();

  @BeforeEach
  void stubKind() {
    Property property = Property.builder().id(PROPERTY_ID).build();
    when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(property);
    when(unitDetailsRepository.findByUnitIdAndTeamId(UNIT_ID, TEAM_ID))
        .thenReturn(Optional.empty());
    when(leaseKindResolver.resolve(any(), any(), any())).thenReturn(LeaseKind.RESIDENTIAL);
  }

  private Contract contract() {
    return Contract.builder()
        .id(CONTRACT_ID)
        .propertyId(PROPERTY_ID)
        .unitId(UNIT_ID)
        .teamId(TEAM_ID)
        .countryCode(Optional.of("NL"))
        .documentLanguages(List.of("en"))
        .build();
  }

  private LeaseClauseTemplate template(
      Sid identifier, String key, boolean optional, int sortOrder) {
    return LeaseClauseTemplate.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(identifier))
        .countryCode("NL")
        .clauseKey(key)
        .titleI18nKey("lease." + key + ".title")
        .bodyI18nKey("lease." + key + ".body")
        .defaultIncluded(true)
        .optional(optional)
        .sortOrder(sortOrder)
        .version(1)
        .build();
  }

  @Test
  void excludingNonOptionalClauseThrowsBeforeAnyWrite() {
    Sid templateIdentifier = Sid.of("LCT0000000000000000000000001");
    LeaseClauseTemplate mandatoryClause =
        template(templateIdentifier, "parties", /* optional= */ false, 1);

    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());
    when(resolver.templatesFor(any(), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(List.of(mandatoryClause));

    UpdateContractLeaseClausesRequest request =
        new UpdateContractLeaseClausesRequest(
            List.of(new ClauseSelection(templateIdentifier.value(), false, 1)));

    assertThatThrownBy(() -> service.updateClauses(CONTRACT_IDENTIFIER, request, principal))
        .isInstanceOf(BadRequestException.class);

    // The review-focus assertion: the rejection must happen before any write, not merely before
    // the method returns.
    verify(overrideRepository, never()).replaceForContract(any(), any(), any(), any());
  }

  @Test
  void duplicateTemplateIdentifierThrowsBeforeAnyWrite() {
    Sid templateIdentifier = Sid.of("LCT0000000000000000000000001");

    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());

    UpdateContractLeaseClausesRequest request =
        new UpdateContractLeaseClausesRequest(
            List.of(
                new ClauseSelection(templateIdentifier.value(), true, 1),
                new ClauseSelection(templateIdentifier.value(), false, 2)));

    assertThatThrownBy(() -> service.updateClauses(CONTRACT_IDENTIFIER, request, principal))
        .isInstanceOf(BadRequestException.class);

    // The review-focus assertion: the rejection must happen before any write, not merely before
    // the method returns — and before the template lookup that would otherwise follow.
    verify(resolver, never()).templatesFor(any(), any());
    verify(overrideRepository, never()).replaceForContract(any(), any(), any(), any());
  }

  @Test
  void unknownTemplateIdentifierThrowsBadRequest() {
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());
    when(resolver.templatesFor(any(), eq(LeaseKind.RESIDENTIAL))).thenReturn(List.of());

    UpdateContractLeaseClausesRequest request =
        new UpdateContractLeaseClausesRequest(
            List.of(new ClauseSelection("LCT0000000000000000000000099", true, 1)));

    assertThatThrownBy(() -> service.updateClauses(CONTRACT_IDENTIFIER, request, principal))
        .isInstanceOf(BadRequestException.class);

    verify(overrideRepository, never()).replaceForContract(any(), any(), any(), any());
  }

  @Test
  void getClausesResolvesWithDerivedKind() {
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());
    when(resolver.resolve(any(), any(), eq(LeaseKind.RESIDENTIAL))).thenReturn(List.of());

    assertThat(service.getClauses(CONTRACT_IDENTIFIER, principal)).isEmpty();

    verify(resolver).resolve(any(), any(), eq(LeaseKind.RESIDENTIAL));
  }

  @Test
  void pinnedClauseSortOrderInRequestIsStoredButIgnoredByResolver() {
    Sid templateIdentifier = Sid.of("LCT0000000000000000000000001");
    LeaseClauseTemplate pinned = template(templateIdentifier, "parties", false, 1);
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());
    when(resolver.templatesFor(any(), eq(LeaseKind.RESIDENTIAL))).thenReturn(List.of(pinned));
    when(resolver.resolve(any(), any(), anyList())).thenReturn(List.of());

    service.updateClauses(
        CONTRACT_IDENTIFIER,
        new UpdateContractLeaseClausesRequest(
            List.of(new ClauseSelection(templateIdentifier.value(), true, 99))),
        principal);

    verify(overrideRepository).replaceForContract(any(), any(), any(), any());
  }

  @Test
  void validRequestSucceedsAndReplacesOverrides() {
    Sid templateIdentifier = Sid.of("LCT0000000000000000000000001");
    LeaseClauseTemplate optionalClause =
        template(templateIdentifier, "house-rules", /* optional= */ true, 1);

    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());
    when(resolver.templatesFor(any(), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(List.of(optionalClause));

    List<ResolvedLeaseClauseResponse> resolved =
        List.of(
            new ResolvedLeaseClauseResponse(
                templateIdentifier, "house-rules", "Title", "Body", true, true, 1, false, 1));
    when(resolver.resolve(any(), any(), anyList())).thenReturn(resolved);

    UpdateContractLeaseClausesRequest request =
        new UpdateContractLeaseClausesRequest(
            List.of(new ClauseSelection(templateIdentifier.value(), true, 1)));

    List<ResolvedLeaseClauseResponse> result =
        service.updateClauses(CONTRACT_IDENTIFIER, request, principal);

    assertThat(result).isEqualTo(resolved);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ContractLeaseClause>> captor = ArgumentCaptor.forClass(List.class);
    verify(overrideRepository)
        .replaceForContract(eq(CONTRACT_ID), eq(TEAM_ID), eq(USER_ID), captor.capture());

    List<ContractLeaseClause> saved = captor.getValue();
    assertThat(saved).hasSize(1);
    assertThat(saved.get(0).getClauseTemplateId()).isEqualTo(optionalClause.getId());
    assertThat(saved.get(0).isIncluded()).isTrue();
    assertThat(saved.get(0).getSortOrder()).isEqualTo(1);

    // The already-fetched template list is passed into resolve(), not re-fetched by it.
    verify(resolver, times(1)).templatesFor(any(), eq(LeaseKind.RESIDENTIAL));
    verify(resolver).resolve(any(), any(), eq(List.of(optionalClause)));
  }
}
