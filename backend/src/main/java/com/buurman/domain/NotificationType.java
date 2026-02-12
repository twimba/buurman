package com.buurman.domain;

import java.util.Arrays;
import java.util.List;

public enum NotificationType {
    WELCOME("Welcome", false),
    VERIFICATION_CODE("Verification Code", false),
    PHONE_VERIFICATION_CODE("Phone Verification Code", false),
    TEAM_INVITATION("Team Invitation", false),
    INVITATION_ACCEPTED("Invitation Accepted", false),
    PASSWORD_CHANGED("Password Changed", false),
    PAYMENT_REMINDER("Payment Reminders", true),
    CONTRACT_EXPIRY("Contract Expiry", true),
    PROPERTY_CREATED("Property Created", true),
    CONTRACT_CREATED("Contract Created", true),
    CONTRACT_STATUS_CHANGED("Contract Status Changed", true),
    CONTRACT_REOPENED("Contract Reopened", true),
    PAYMENT_PAID("Payment Paid", true),
    PAYMENT_RECEIVAL("Payment Received", true),
    EXPENSE_CREATED("Expense Created", true);

    private final String displayName;
    private final boolean configurable;

    NotificationType(String displayName, boolean configurable) {
        this.displayName = displayName;
        this.configurable = configurable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isConfigurable() {
        return configurable;
    }

    public static List<NotificationType> configurableTypes() {
        return Arrays.stream(values()).filter(NotificationType::isConfigurable).toList();
    }
}
