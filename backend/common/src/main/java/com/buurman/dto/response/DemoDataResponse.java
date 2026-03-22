package com.buurman.dto.response;

import com.buurman.util.Generated;

@Generated
public record DemoDataResponse(
    int teamsCreated,
    int usersCreated,
    int propertiesCreated,
    int contactsCreated,
    int contractsCreated,
    int paymentsCreated,
    int expensesCreated,
    int notificationsCreated,
    int documentsCreated,
    long durationMs) {}
