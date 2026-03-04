package com.buurman.util;

import com.buurman.domain.Ulid;

import de.huxhorn.sulky.ulid.ULID;

public class UlidGenerator {

  private static final ULID ulid = new ULID();

  private UlidGenerator() {}

  private static Ulid generate() {
    return Ulid.of(ulid.nextULID());
  }

  private static Ulid generate(EntityPrefix prefix) {
    return Ulid.of(prefix.getCode() + ulid.nextULID());
  }

  public static Ulid newToken() {
    return generate();
  }

  public static Ulid newAmenityId() {
    return generate(EntityPrefix.AMN);
  }

  public static Ulid newBroadcastMessageId() {
    return generate(EntityPrefix.BCM);
  }

  public static Ulid newCalendarFeedId() {
    return generate(EntityPrefix.CAL);
  }

  public static Ulid newContractId() {
    return generate(EntityPrefix.CON);
  }

  public static Ulid newContractPartyId() {
    return generate(EntityPrefix.CTP);
  }

  public static Ulid newContractRentPeriodId() {
    return generate(EntityPrefix.CRP);
  }

  public static Ulid newContractPaymentInstructionId() {
    return generate(EntityPrefix.CPI);
  }

  public static Ulid newDocumentId() {
    return generate(EntityPrefix.DOC);
  }

  public static Ulid newExpenseId() {
    return generate(EntityPrefix.EXP);
  }

  public static Ulid newGeneratedReportId() {
    return generate(EntityPrefix.GRP);
  }

  public static Ulid newNotificationId() {
    return generate(EntityPrefix.NTF);
  }

  public static Ulid newPaymentId() {
    return generate(EntityPrefix.PAY);
  }

  public static Ulid newPaymentInstructionId() {
    return generate(EntityPrefix.PIN);
  }

  public static Ulid newPaymentReceivalId() {
    return generate(EntityPrefix.PRE);
  }

  public static Ulid newPhotoId() {
    return generate(EntityPrefix.PHO);
  }

  public static Ulid newPropertyId() {
    return generate(EntityPrefix.PRO);
  }

  public static Ulid newOccupancyPeriodId() {
    return generate(EntityPrefix.OCP);
  }

  public static Ulid newPropertyOutdoorAreaId() {
    return generate(EntityPrefix.POA);
  }

  public static Ulid newRegistrationInvitationId() {
    return generate(EntityPrefix.RIN);
  }

  public static Ulid newTeamId() {
    return generate(EntityPrefix.TEA);
  }

  public static Ulid newTenantId() {
    return generate(EntityPrefix.TEN);
  }

  public static Ulid newTenantAddressId() {
    return generate(EntityPrefix.TAD);
  }

  public static Ulid newUserId() {
    return generate(EntityPrefix.USR);
  }

  public static Ulid newAcquisitionId() {
    return generate(EntityPrefix.ACQ);
  }

  public static Ulid newValuationId() {
    return generate(EntityPrefix.VAL);
  }

  public static Ulid newFinancingId() {
    return generate(EntityPrefix.FIN);
  }

  public static Ulid newFinancingPaymentId() {
    return generate(EntityPrefix.FPY);
  }

  public static Ulid newInsuranceId() {
    return generate(EntityPrefix.INS);
  }

  public static Ulid newPropertyTaxId() {
    return generate(EntityPrefix.PTX);
  }

  public static Ulid newPropertyFeeId() {
    return generate(EntityPrefix.FEE);
  }

  public static Ulid newTakeoutId() {
    return generate(EntityPrefix.TKO);
  }
}
