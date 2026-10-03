package com.buurman.service.export.tabular;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Contract;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.util.MoneyAmount;
import com.buurman.util.SidGenerator;

@DisplayName("ContractTabularExportBuilder")
@ExtendWith(MockitoExtension.class)
class ContractTabularExportBuilderTest {

  @Mock private ContractRepository contractRepository;
  @Mock private PropertyRepository propertyRepository;

  private ContractTabularExportBuilder builder;
  private UUID teamId;

  @BeforeEach
  void setUp() {
    builder = new ContractTabularExportBuilder(contractRepository, propertyRepository);
    teamId = UUID.randomUUID();
    when(propertyRepository.findAllByTeamId(any())).thenReturn(List.of());
  }

  @Test
  @DisplayName("build() with a status filter excludes contracts of other statuses")
  void buildRespectsStatusFilter() {
    Contract active = contract(Contract.ContractStatus.ACTIVE);
    Contract draft = contract(Contract.ContractStatus.DRAFT);
    // The repository (proven by Task 2's ContractRepositoryIntegrationTest) applies the status
    // filter at the DB level, so a status="ACTIVE" call never returns the DRAFT contract; this
    // stub simulates that so the builder's forwarding of the status/search/endingWithinDays
    // params can be verified in isolation, same as build(teamId) already forwards to
    // findAllByTeamId(teamId, null, null, null).
    when(contractRepository.findAllByTeamId(teamId, List.of("ACTIVE"), null, null))
        .thenReturn(List.of(active));

    TabularExport export = builder.build(teamId, List.of("ACTIVE"), null, null);

    List<Object[]> rows = export.sheets().getFirst().rows();
    assertThat(rows).hasSize(1);
    assertThat(rows.getFirst()[0]).isEqualTo(active.getIdentifier().orElseThrow().toString());
    assertThat(rows.getFirst()[0]).isNotEqualTo(draft.getIdentifier().orElseThrow().toString());
  }

  private Contract contract(Contract.ContractStatus status) {
    return Contract.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(SidGenerator.newContractId()))
        .teamId(teamId)
        .propertyId(UUID.randomUUID())
        .unitId(UUID.randomUUID())
        .contractType(Contract.ContractType.FIXED_TERM)
        .startDate(LocalDate.of(2026, 1, 1))
        .rentAmount(MoneyAmount.of(new BigDecimal("1500.00"), "EUR"))
        .paymentFrequency(Contract.PaymentFrequency.MONTHLY)
        .status(status)
        .build();
  }
}
