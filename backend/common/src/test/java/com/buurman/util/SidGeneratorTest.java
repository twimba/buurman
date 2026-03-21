package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.buurman.domain.Sid;
import com.buurman.domain.identifier.AmenityIdentifier;
import com.buurman.domain.identifier.BroadcastMessageIdentifier;
import com.buurman.domain.identifier.CalendarFeedIdentifier;
import com.buurman.domain.identifier.ContactAddressIdentifier;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContactNoteIdentifier;
import com.buurman.domain.identifier.ContactRelationshipIdentifier;
import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractPartyIdentifier;
import com.buurman.domain.identifier.ContractPaymentInstructionIdentifier;
import com.buurman.domain.identifier.ContractRentPeriodIdentifier;
import com.buurman.domain.identifier.CountryRequestIdentifier;
import com.buurman.domain.identifier.DataTakeoutIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.ExpenseIdentifier;
import com.buurman.domain.identifier.FinancingPaymentIdentifier;
import com.buurman.domain.identifier.GeneratedReportIdentifier;
import com.buurman.domain.identifier.ImpersonationSessionIdentifier;
import com.buurman.domain.identifier.NotificationIdentifier;
import com.buurman.domain.identifier.OccupancyPeriodIdentifier;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.domain.identifier.PaymentInstructionIdentifier;
import com.buurman.domain.identifier.PaymentReceivalIdentifier;
import com.buurman.domain.identifier.PhotoIdentifier;
import com.buurman.domain.identifier.PropertyAcquisitionIdentifier;
import com.buurman.domain.identifier.PropertyFeeIdentifier;
import com.buurman.domain.identifier.PropertyFinancingIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.PropertyInsuranceIdentifier;
import com.buurman.domain.identifier.PropertyOutdoorAreaIdentifier;
import com.buurman.domain.identifier.PropertyTaxIdentifier;
import com.buurman.domain.identifier.PropertyValuationIdentifier;
import com.buurman.domain.identifier.RegistrationInvitationIdentifier;
import com.buurman.domain.identifier.RentComponentIdentifier;
import com.buurman.domain.identifier.RentRegulationCountryIdentifier;
import com.buurman.domain.identifier.RentRegulationRegionIdentifier;
import com.buurman.domain.identifier.RentRegulationRuleIdentifier;
import com.buurman.domain.identifier.TeamIdentifier;
import com.buurman.domain.identifier.UserIdentifier;
import com.buurman.domain.identifier.WwsCalculationIdentifier;

class SidGeneratorTest {

  @Nested
  @DisplayName("newToken")
  class NewToken {

    @Test
    @DisplayName("returns Sid with 26 characters")
    void generatesToken() {
      Sid token = SidGenerator.newToken();

      assertThat(token.value()).hasSize(26);
    }

    @Test
    @DisplayName("generates unique tokens")
    void uniqueTokens() {
      List<String> tokens =
          IntStream.range(0, 100).mapToObj(i -> SidGenerator.newToken().value()).toList();

      assertThat(tokens).doesNotHaveDuplicates();
    }
  }

  @Nested
  @DisplayName("typed ID generators")
  class TypedIdGenerators {

    static Stream<Arguments> allTypedIdGenerators() {
      return Stream.of(
          Arguments.of("AMN", SidGenerator.newAmenityId(), AmenityIdentifier.class),
          Arguments.of(
              "BCM", SidGenerator.newBroadcastMessageId(), BroadcastMessageIdentifier.class),
          Arguments.of("CAL", SidGenerator.newCalendarFeedId(), CalendarFeedIdentifier.class),
          Arguments.of("CON", SidGenerator.newContractId(), ContractIdentifier.class),
          Arguments.of("CTP", SidGenerator.newContractPartyId(), ContractPartyIdentifier.class),
          Arguments.of(
              "CRP", SidGenerator.newContractRentPeriodId(), ContractRentPeriodIdentifier.class),
          Arguments.of(
              "CPI",
              SidGenerator.newContractPaymentInstructionId(),
              ContractPaymentInstructionIdentifier.class),
          Arguments.of("DOC", SidGenerator.newDocumentId(), DocumentIdentifier.class),
          Arguments.of("EXP", SidGenerator.newExpenseId(), ExpenseIdentifier.class),
          Arguments.of("GRP", SidGenerator.newGeneratedReportId(), GeneratedReportIdentifier.class),
          Arguments.of("NTF", SidGenerator.newNotificationId(), NotificationIdentifier.class),
          Arguments.of("PAY", SidGenerator.newPaymentId(), PaymentIdentifier.class),
          Arguments.of(
              "PIN", SidGenerator.newPaymentInstructionId(), PaymentInstructionIdentifier.class),
          Arguments.of("PRE", SidGenerator.newPaymentReceivalId(), PaymentReceivalIdentifier.class),
          Arguments.of("PHO", SidGenerator.newPhotoId(), PhotoIdentifier.class),
          Arguments.of("PRO", SidGenerator.newPropertyId(), PropertyIdentifier.class),
          Arguments.of("OCP", SidGenerator.newOccupancyPeriodId(), OccupancyPeriodIdentifier.class),
          Arguments.of(
              "POA", SidGenerator.newPropertyOutdoorAreaId(), PropertyOutdoorAreaIdentifier.class),
          Arguments.of(
              "RIN",
              SidGenerator.newRegistrationInvitationId(),
              RegistrationInvitationIdentifier.class),
          Arguments.of("TEA", SidGenerator.newTeamId(), TeamIdentifier.class),
          Arguments.of("CTC", SidGenerator.newContactId(), ContactIdentifier.class),
          Arguments.of("CAD", SidGenerator.newContactAddressId(), ContactAddressIdentifier.class),
          Arguments.of("CNT", SidGenerator.newContactNoteId(), ContactNoteIdentifier.class),
          Arguments.of(
              "CRL",
              SidGenerator.newContactRelationshipId(),
              ContactRelationshipIdentifier.class),
          Arguments.of("USR", SidGenerator.newUserId(), UserIdentifier.class),
          Arguments.of("ACQ", SidGenerator.newAcquisitionId(), PropertyAcquisitionIdentifier.class),
          Arguments.of("VAL", SidGenerator.newValuationId(), PropertyValuationIdentifier.class),
          Arguments.of("FIN", SidGenerator.newFinancingId(), PropertyFinancingIdentifier.class),
          Arguments.of(
              "FPY", SidGenerator.newFinancingPaymentId(), FinancingPaymentIdentifier.class),
          Arguments.of("INS", SidGenerator.newInsuranceId(), PropertyInsuranceIdentifier.class),
          Arguments.of("PTX", SidGenerator.newPropertyTaxId(), PropertyTaxIdentifier.class),
          Arguments.of("FEE", SidGenerator.newPropertyFeeId(), PropertyFeeIdentifier.class),
          Arguments.of("TKO", SidGenerator.newTakeoutId(), DataTakeoutIdentifier.class),
          Arguments.of("WWS", SidGenerator.newWwsCalculationId(), WwsCalculationIdentifier.class),
          Arguments.of(
              "RRC",
              SidGenerator.newRentRegulationCountryId(),
              RentRegulationCountryIdentifier.class),
          Arguments.of(
              "RRG",
              SidGenerator.newRentRegulationRegionId(),
              RentRegulationRegionIdentifier.class),
          Arguments.of(
              "RRL", SidGenerator.newRentRegulationRuleId(), RentRegulationRuleIdentifier.class),
          Arguments.of("CRQ", SidGenerator.newCountryRequestId(), CountryRequestIdentifier.class),
          Arguments.of(
              "IMS",
              SidGenerator.newImpersonationSessionId(),
              ImpersonationSessionIdentifier.class),
          Arguments.of(
              "CEX", SidGenerator.newContractExtensionId(), ContractExtensionIdentifier.class),
          Arguments.of("RCO", SidGenerator.newRentComponentId(), RentComponentIdentifier.class));
    }

    @ParameterizedTest(name = "{0} → {2}")
    @MethodSource("allTypedIdGenerators")
    @DisplayName("generates correct prefix, length, and type")
    void generatesCorrectId(String prefix, Sid id, Class<? extends Sid> expectedType) {
      assertThat(id.value()).startsWith(prefix);
      assertThat(id.value()).hasSize(prefix.length() + 26);
      assertThat(id).isInstanceOf(expectedType);
    }
  }

  @Nested
  @DisplayName("uniqueness")
  class Uniqueness {

    @Test
    @DisplayName("generates 100 unique IDs")
    void allDistinct() {
      List<String> ids =
          IntStream.range(0, 100).mapToObj(i -> SidGenerator.newPropertyId().value()).toList();

      assertThat(ids).doesNotHaveDuplicates();
    }
  }

  @Nested
  @DisplayName("sortability")
  class Sortability {

    @Test
    @DisplayName("ULID timestamp prefix is non-decreasing across IDs")
    void timestampNonDecreasing() {
      // ULID spec guarantees timestamp component (first 10 chars of ULID) is non-decreasing.
      // Within the same millisecond all IDs share the same timestamp prefix.
      // This test verifies the structural property rather than cross-millisecond ordering.
      List<String> timestamps =
          IntStream.range(0, 10)
              .mapToObj(i -> SidGenerator.newPropertyId().value().substring(3, 13))
              .toList();

      for (int i = 1; i < timestamps.size(); i++) {
        assertThat(timestamps.get(i))
            .as("timestamp at index %d should be >= timestamp at index %d", i, i - 1)
            .isGreaterThanOrEqualTo(timestamps.get(i - 1));
      }
    }
  }
}
