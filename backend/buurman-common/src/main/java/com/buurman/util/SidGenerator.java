package com.buurman.util;

import com.buurman.domain.Sid;
import com.buurman.domain.identifier.AmenityIdentifier;
import com.buurman.domain.identifier.BroadcastMessageIdentifier;
import com.buurman.domain.identifier.CalendarFeedIdentifier;
import com.buurman.domain.identifier.ContactAddressIdentifier;
import com.buurman.domain.identifier.ContactCreditIdentifier;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContactNoteIdentifier;
import com.buurman.domain.identifier.ContactRelationshipIdentifier;
import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractPartyIdentifier;
import com.buurman.domain.identifier.ContractPaymentInstructionIdentifier;
import com.buurman.domain.identifier.ContractRentPeriodIdentifier;
import com.buurman.domain.identifier.CountryRequestIdentifier;
import com.buurman.domain.identifier.DataImportIdentifier;
import com.buurman.domain.identifier.DataTakeoutIdentifier;
import com.buurman.domain.identifier.DepositDeductionIdentifier;
import com.buurman.domain.identifier.DepositIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.ExpenseIdentifier;
import com.buurman.domain.identifier.FinancingPaymentIdentifier;
import com.buurman.domain.identifier.GeneratedReportIdentifier;
import com.buurman.domain.identifier.ImpersonationSessionIdentifier;
import com.buurman.domain.identifier.NotificationIdentifier;
import com.buurman.domain.identifier.OccupancyPeriodIdentifier;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.domain.identifier.PaymentInstructionIdentifier;
import com.buurman.domain.identifier.PaymentPlanIdentifier;
import com.buurman.domain.identifier.PaymentReceivalIdentifier;
import com.buurman.domain.identifier.PaymentReminderIdentifier;
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

  public static DataImportIdentifier newDataImportId() {
    return DataImportIdentifier.of(generateRaw(EntityPrefix.DIM));
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

  public static PaymentReminderIdentifier newPaymentReminderId() {
    return PaymentReminderIdentifier.of(generateRaw(EntityPrefix.PRM));
  }

  public static ContactCreditIdentifier newContactCreditId() {
    return ContactCreditIdentifier.of(generateRaw(EntityPrefix.CCR));
  }

  public static DepositIdentifier newDepositId() {
    return DepositIdentifier.of(generateRaw(EntityPrefix.DEP));
  }

  public static DepositDeductionIdentifier newDepositDeductionId() {
    return DepositDeductionIdentifier.of(generateRaw(EntityPrefix.DDD));
  }

  public static PaymentPlanIdentifier newPaymentPlanId() {
    return PaymentPlanIdentifier.of(generateRaw(EntityPrefix.PPL));
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

  public static ContactIdentifier newContactId() {
    return ContactIdentifier.of(generateRaw(EntityPrefix.CTC));
  }

  public static ContactAddressIdentifier newContactAddressId() {
    return ContactAddressIdentifier.of(generateRaw(EntityPrefix.CAD));
  }

  public static ContactNoteIdentifier newContactNoteId() {
    return ContactNoteIdentifier.of(generateRaw(EntityPrefix.CNT));
  }

  public static ContactRelationshipIdentifier newContactRelationshipId() {
    return ContactRelationshipIdentifier.of(generateRaw(EntityPrefix.CRL));
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

  public static RentRegulationCountryIdentifier newRentRegulationCountryId() {
    return RentRegulationCountryIdentifier.of(generateRaw(EntityPrefix.RRC));
  }

  public static RentRegulationRegionIdentifier newRentRegulationRegionId() {
    return RentRegulationRegionIdentifier.of(generateRaw(EntityPrefix.RRG));
  }

  public static RentRegulationRuleIdentifier newRentRegulationRuleId() {
    return RentRegulationRuleIdentifier.of(generateRaw(EntityPrefix.RRL));
  }

  public static CountryRequestIdentifier newCountryRequestId() {
    return CountryRequestIdentifier.of(generateRaw(EntityPrefix.CRQ));
  }

  public static ImpersonationSessionIdentifier newImpersonationSessionId() {
    return ImpersonationSessionIdentifier.of(generateRaw(EntityPrefix.IMS));
  }

  public static ContractExtensionIdentifier newContractExtensionId() {
    return ContractExtensionIdentifier.of(generateRaw(EntityPrefix.CEX));
  }

  public static RentComponentIdentifier newRentComponentId() {
    return RentComponentIdentifier.of(generateRaw(EntityPrefix.RCO));
  }
}
