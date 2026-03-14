package com.buurman.domain;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

@Getter
public enum NotificationType {
  WELCOME("Welcome", false, false),
  VERIFICATION_CODE("Verification Code", false, false),
  PHONE_VERIFICATION_CODE("Phone Verification Code", false, false),
  TEAM_INVITATION("Team Invitation", false, false),
  INVITATION_ACCEPTED("Invitation Accepted", false, true),
  PASSWORD_CHANGED("Password Changed", false, false),
  PAYMENT_REMINDER("Payment Reminders", true, true),
  CONTRACT_EXPIRY("Contract Expiry", true, true),
  PROPERTY_CREATED("Property Created", true, true),
  CONTRACT_CREATED("Contract Created", true, true),
  CONTRACT_STATUS_CHANGED("Contract Status Changed", true, true),
  CONTRACT_REOPENED("Contract Reopened", true, true),
  CONTRACT_RENT_ADJUSTED("Contract Rent Adjusted", true, true),
  PAYMENT_PAID("Payment Paid", true, true),
  PAYMENT_RECEIVAL("Payment Received", true, true),
  EXPENSE_CREATED("Expense Created", true, true);

  private final String displayName;
  private final boolean configurable;
  private final boolean consolidatable;

  NotificationType(String displayName, boolean configurable, boolean consolidatable) {
    this.displayName = displayName;
    this.configurable = configurable;
    this.consolidatable = consolidatable;
  }

  public static List<NotificationType> configurableTypes() {
    return Arrays.stream(values()).filter(NotificationType::isConfigurable).toList();
  }

  public static List<NotificationType> consolidatableTypes() {
    return Arrays.stream(values()).filter(NotificationType::isConsolidatable).toList();
  }
}
