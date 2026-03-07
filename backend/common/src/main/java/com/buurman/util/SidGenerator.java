package com.buurman.util;

import com.buurman.domain.Sid;
import com.buurman.domain.identifier.AmenityIdentifier;
import com.buurman.domain.identifier.BroadcastMessageIdentifier;
import com.buurman.domain.identifier.CalendarFeedIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractPartyIdentifier;
import com.buurman.domain.identifier.ContractPaymentInstructionIdentifier;
import com.buurman.domain.identifier.ContractRentPeriodIdentifier;
import com.buurman.domain.identifier.DataTakeoutIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.ExpenseIdentifier;
import com.buurman.domain.identifier.FinancingPaymentIdentifier;
import com.buurman.domain.identifier.GeneratedReportIdentifier;
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
import com.buurman.domain.identifier.TeamIdentifier;
import com.buurman.domain.identifier.TenantAddressIdentifier;
import com.buurman.domain.identifier.TenantIdentifier;
import com.buurman.domain.identifier.UserIdentifier;
import com.buurman.domain.identifier.WwsCalculationIdentifier;

import de.huxhorn.sulky.ulid.ULID;

public class SidGenerator {

  private static final ULID ulid = new ULID();

  private SidGenerator() {}

  private static String generateRaw() {
    return ulid.nextULID();
  }

  private static String generateRaw(EntityPrefix prefix) {
    return prefix.getCode() + ulid.nextULID();
  }

  public static Sid newToken() {
    return Sid.of(generateRaw());
  }

  public static AmenityIdentifier newAmenityId() {
    return AmenityIdentifier.of(generateRaw(EntityPrefix.AMN));
  }

  public static BroadcastMessageIdentifier newBroadcastMessageId() {
    return BroadcastMessageIdentifier.of(generateRaw(EntityPrefix.BCM));
  }

  public static CalendarFeedIdentifier newCalendarFeedId() {
    return CalendarFeedIdentifier.of(generateRaw(EntityPrefix.CAL));
  }

  public static ContractIdentifier newContractId() {
    return ContractIdentifier.of(generateRaw(EntityPrefix.CON));
  }

  public static ContractPartyIdentifier newContractPartyId() {
    return ContractPartyIdentifier.of(generateRaw(EntityPrefix.CTP));
  }

  public static ContractRentPeriodIdentifier newContractRentPeriodId() {
    return ContractRentPeriodIdentifier.of(generateRaw(EntityPrefix.CRP));
  }

  public static ContractPaymentInstructionIdentifier newContractPaymentInstructionId() {
    return ContractPaymentInstructionIdentifier.of(generateRaw(EntityPrefix.CPI));
  }

  public static DocumentIdentifier newDocumentId() {
    return DocumentIdentifier.of(generateRaw(EntityPrefix.DOC));
  }

  public static ExpenseIdentifier newExpenseId() {
    return ExpenseIdentifier.of(generateRaw(EntityPrefix.EXP));
  }

  public static GeneratedReportIdentifier newGeneratedReportId() {
    return GeneratedReportIdentifier.of(generateRaw(EntityPrefix.GRP));
  }

  public static NotificationIdentifier newNotificationId() {
    return NotificationIdentifier.of(generateRaw(EntityPrefix.NTF));
  }

  public static PaymentIdentifier newPaymentId() {
    return PaymentIdentifier.of(generateRaw(EntityPrefix.PAY));
  }

  public static PaymentInstructionIdentifier newPaymentInstructionId() {
    return PaymentInstructionIdentifier.of(generateRaw(EntityPrefix.PIN));
  }

  public static PaymentReceivalIdentifier newPaymentReceivalId() {
    return PaymentReceivalIdentifier.of(generateRaw(EntityPrefix.PRE));
  }

  public static PhotoIdentifier newPhotoId() {
    return PhotoIdentifier.of(generateRaw(EntityPrefix.PHO));
  }

  public static PropertyIdentifier newPropertyId() {
    return PropertyIdentifier.of(generateRaw(EntityPrefix.PRO));
  }

  public static OccupancyPeriodIdentifier newOccupancyPeriodId() {
    return OccupancyPeriodIdentifier.of(generateRaw(EntityPrefix.OCP));
  }

  public static PropertyOutdoorAreaIdentifier newPropertyOutdoorAreaId() {
    return PropertyOutdoorAreaIdentifier.of(generateRaw(EntityPrefix.POA));
  }

  public static RegistrationInvitationIdentifier newRegistrationInvitationId() {
    return RegistrationInvitationIdentifier.of(generateRaw(EntityPrefix.RIN));
  }

  public static TeamIdentifier newTeamId() {
    return TeamIdentifier.of(generateRaw(EntityPrefix.TEA));
  }

  public static TenantIdentifier newTenantId() {
    return TenantIdentifier.of(generateRaw(EntityPrefix.TEN));
  }

  public static TenantAddressIdentifier newTenantAddressId() {
    return TenantAddressIdentifier.of(generateRaw(EntityPrefix.TAD));
  }

  public static UserIdentifier newUserId() {
    return UserIdentifier.of(generateRaw(EntityPrefix.USR));
  }

  public static PropertyAcquisitionIdentifier newAcquisitionId() {
    return PropertyAcquisitionIdentifier.of(generateRaw(EntityPrefix.ACQ));
  }

  public static PropertyValuationIdentifier newValuationId() {
    return PropertyValuationIdentifier.of(generateRaw(EntityPrefix.VAL));
  }

  public static PropertyFinancingIdentifier newFinancingId() {
    return PropertyFinancingIdentifier.of(generateRaw(EntityPrefix.FIN));
  }

  public static FinancingPaymentIdentifier newFinancingPaymentId() {
    return FinancingPaymentIdentifier.of(generateRaw(EntityPrefix.FPY));
  }

  public static PropertyInsuranceIdentifier newInsuranceId() {
    return PropertyInsuranceIdentifier.of(generateRaw(EntityPrefix.INS));
  }

  public static PropertyTaxIdentifier newPropertyTaxId() {
    return PropertyTaxIdentifier.of(generateRaw(EntityPrefix.PTX));
  }

  public static PropertyFeeIdentifier newPropertyFeeId() {
    return PropertyFeeIdentifier.of(generateRaw(EntityPrefix.FEE));
  }

  public static DataTakeoutIdentifier newTakeoutId() {
    return DataTakeoutIdentifier.of(generateRaw(EntityPrefix.TKO));
  }

  public static WwsCalculationIdentifier newWwsCalculationId() {
    return WwsCalculationIdentifier.of(generateRaw(EntityPrefix.WWS));
  }
}
