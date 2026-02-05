package com.buurman.dto.response;

public record DemoDataResponse(
        int teamsCreated,
        int usersCreated,
        int propertiesCreated,
        int tenantsCreated,
        int contractsCreated,
        int paymentsCreated,
        int expensesCreated,
        long durationMs
) {}
