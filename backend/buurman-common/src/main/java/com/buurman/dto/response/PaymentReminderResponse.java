package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.NotificationChannel;
import com.buurman.domain.PaymentReminder;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record PaymentReminderResponse(
    Sid identifier,
    PaymentReminder.ReminderType reminderType,
    NotificationChannel channel,
    Optional<String> recipientEmail,
    int daysOverdue,
    BigDecimal outstandingAmount,
    String currency,
    Optional<String> notes,
    Optional<String> sentByName,
    Instant sentAt) {}
