package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.buurman.domain.Contract;
import com.buurman.domain.LeaseRegime;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.ContractPartyRequest;
import com.buurman.dto.request.CreateContractRequest;
import com.buurman.dto.request.UpdateContractRequest;
import com.buurman.util.MoneyAmount;

@DisplayName("ContractMapper lease regime")
class ContractMapperTest {

  private final ContractMapper mapper = createMapper();

  private static ContractMapper createMapper() {
    ContractMapperImpl impl = new ContractMapperImpl();
    ReflectionTestUtils.setField(impl, "optionalMappingConfig", new OptionalMappingConfig());
    return impl;
  }

  @Test
  @DisplayName("create without leaseRegime defaults to STANDARD")
  void createDefaultsToStandard() {
    Contract contract = mapper.toEntity(createRequest(Optional.empty()));

    assertThat(contract.getLeaseRegime()).isEqualTo(LeaseRegime.STANDARD);
  }

  @Test
  @DisplayName("create with leaseRegime uses it")
  void createUsesProvided() {
    Contract contract =
        mapper.toEntity(createRequest(Optional.of(LeaseRegime.STUDENT_OR_MOBILITY)));

    assertThat(contract.getLeaseRegime()).isEqualTo(LeaseRegime.STUDENT_OR_MOBILITY);
  }

  @Test
  @DisplayName("update with leaseRegime changes it")
  void updateChanges() {
    Contract contract = existing();

    mapper.updateEntity(contract, updateRequest(Optional.of(LeaseRegime.SHORT_TERM)));

    assertThat(contract.getLeaseRegime()).isEqualTo(LeaseRegime.SHORT_TERM);
  }

  @Test
  @DisplayName("update without leaseRegime keeps the existing value")
  void updateKeepsExisting() {
    Contract contract = existing();
    contract.setLeaseRegime(LeaseRegime.STUDENT_OR_MOBILITY);

    mapper.updateEntity(contract, updateRequest(Optional.empty()));

    assertThat(contract.getLeaseRegime()).isEqualTo(LeaseRegime.STUDENT_OR_MOBILITY);
  }

  private static Contract existing() {
    return Contract.builder()
        .contractType(Contract.ContractType.INDEFINITE)
        .startDate(LocalDate.of(2026, 1, 1))
        .rentAmount(MoneyAmount.of(new BigDecimal("1000"), "EUR"))
        .paymentFrequency(Contract.PaymentFrequency.MONTHLY)
        .build();
  }

  private static CreateContractRequest createRequest(Optional<LeaseRegime> leaseRegime) {
    return new CreateContractRequest(
        PropertyIdentifier.of("PRP01HQJK4B2X5M3N7P8Q9R0S1T2"),
        null,
        List.<ContractPartyRequest>of(),
        Contract.ContractType.INDEFINITE,
        LocalDate.of(2026, 1, 1),
        Optional.empty(),
        Optional.empty(),
        new BigDecimal("1000"),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Contract.PaymentFrequency.MONTHLY,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        leaseRegime,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        null,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  private static UpdateContractRequest updateRequest(Optional<LeaseRegime> leaseRegime) {
    return new UpdateContractRequest(
        PropertyIdentifier.of("PRP01HQJK4B2X5M3N7P8Q9R0S1T2"),
        Contract.ContractType.INDEFINITE,
        LocalDate.of(2026, 1, 1),
        Optional.empty(),
        Optional.empty(),
        new BigDecimal("1000"),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Contract.PaymentFrequency.MONTHLY,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        leaseRegime,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        null,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }
}
