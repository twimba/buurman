package com.buurman.service.demo;

import com.buurman.util.UlidGenerator;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.util.EntityPrefix.CON;

@Component
public class DemoContractGenerator {

    private static final Logger log = LoggerFactory.getLogger(DemoContractGenerator.class);

    private final DSLContext dsl;
    private final Clock clock;
    private final Random random = new Random(42);

    private static final String[] CONTRACT_TYPES = {"FIXED_TERM", "INDEFINITE", "FURNISHED", "UNFURNISHED"};

    public DemoContractGenerator(DSLContext dsl, Clock clock) {
        this.dsl = dsl;
        this.clock = clock;
    }

    public void generate(DemoDataContext ctx) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = LocalDate.now(clock);

        for (var teamEntry : ctx.getTeamIds().entrySet()) {
            String teamKey = teamEntry.getKey();
            UUID teamId = teamEntry.getValue();
            UUID createdBy = ctx.getAdminUserForTeam(teamKey);
            List<UUID> propertyIds = ctx.getPropertyIdsByTeam().get(teamId);
            List<UUID> tenantIds = ctx.getTenantIdsByTeam().get(teamId);
            List<UUID> contractIds = new ArrayList<>();

            if (propertyIds == null || tenantIds == null) continue;

            int contractCount = Math.min(propertyIds.size(), tenantIds.size());

            for (int i = 0; i < contractCount; i++) {
                UUID contractId = UUID.randomUUID();
                UUID propertyId = propertyIds.get(i);
                UUID tenantId = tenantIds.get(i);

                // Determine contract scenario
                String status;
                LocalDate startDate;
                LocalDate endDate;
                LocalDate signedDate;

                if (i < 2) {
                    // EXPIRED contracts (past)
                    status = "EXPIRED";
                    startDate = today.minusYears(3).minusMonths(random.nextInt(0, 6));
                    endDate = today.minusYears(1).minusMonths(random.nextInt(0, 12));
                    signedDate = startDate.minusDays(random.nextInt(7, 30));
                } else if (i < contractCount - 2) {
                    // ACTIVE contracts (current)
                    status = "ACTIVE";
                    startDate = today.minusMonths(random.nextInt(6, 18));
                    if (random.nextBoolean()) {
                        // Fixed term
                        endDate = today.plusMonths(random.nextInt(6, 24));
                    } else {
                        // Indefinite
                        endDate = null;
                    }
                    signedDate = startDate.minusDays(random.nextInt(7, 30));

                    // Mark property as occupied
                    dsl.update(PROPERTIES)
                            .set(PROPERTIES.STATUS, "OCCUPIED")
                            .where(PROPERTIES.ID.eq(propertyId))
                            .execute();
                } else if (i == contractCount - 2) {
                    // TERMINATED
                    status = "TERMINATED";
                    startDate = today.minusYears(2);
                    endDate = today.minusMonths(random.nextInt(1, 6));
                    signedDate = startDate.minusDays(14);
                } else {
                    // DRAFT (future)
                    status = "DRAFT";
                    startDate = today.plusMonths(1);
                    endDate = today.plusYears(1).plusMonths(1);
                    signedDate = null;
                }

                String contractType = CONTRACT_TYPES[i % CONTRACT_TYPES.length];
                if (endDate == null) {
                    contractType = "INDEFINITE";
                }

                // Realistic Dutch rent amounts
                BigDecimal rentAmount = BigDecimal.valueOf(switch (i % 4) {
                    case 0 -> random.nextInt(800, 1300);   // Studio/small apt
                    case 1 -> random.nextInt(1200, 1800);  // Apartment
                    case 2 -> random.nextInt(1500, 2500);  // House
                    default -> random.nextInt(1000, 3000);  // Commercial/varied
                });

                BigDecimal deposit = rentAmount.multiply(BigDecimal.valueOf(2));
                BigDecimal securityDeposit = rentAmount;

                String contractIdentifier = UlidGenerator.generate(CON).value();
                dsl.insertInto(CONTRACTS)
                        .set(CONTRACTS.ID, contractId)
                        .set(CONTRACTS.IDENTIFIER, contractIdentifier)
                        .set(CONTRACTS.TEAM_ID, teamId)
                        .set(CONTRACTS.PROPERTY_ID, propertyId)
                        .set(CONTRACTS.TENANT_ID, tenantId)
                        .set(CONTRACTS.CONTRACT_TYPE, contractType)
                        .set(CONTRACTS.START_DATE, startDate)
                        .set(CONTRACTS.END_DATE, endDate)
                        .set(CONTRACTS.SIGNED_DATE, signedDate)
                        .set(CONTRACTS.RENT_AMOUNT, rentAmount)
                        .set(CONTRACTS.DEPOSIT_AMOUNT, deposit)
                        .set(CONTRACTS.SECURITY_DEPOSIT, securityDeposit)
                        .set(CONTRACTS.CURRENCY, "EUR")
                        .set(CONTRACTS.PAYMENT_FREQUENCY, "MONTHLY")
                        .set(CONTRACTS.PAYMENT_DUE_DAY, 1)
                        .set(CONTRACTS.AUTO_RENEWAL, "INDEFINITE".equals(contractType))
                        .set(CONTRACTS.RENEWAL_NOTICE_DAYS, 30)
                        .set(CONTRACTS.TERMINATION_NOTICE_DAYS, 30)
                        .set(CONTRACTS.LATE_FEE_PERCENTAGE, BigDecimal.valueOf(2))
                        .set(CONTRACTS.STATUS, status)
                        .set(CONTRACTS.NOTES, "Demo contract for testing purposes")
                        .set(CONTRACTS.CREATED_AT, now.minusDays(random.nextInt(30, 365)))
                        .set(CONTRACTS.UPDATED_AT, now)
                        .set(CONTRACTS.CREATED_BY, createdBy)
                        .set(CONTRACTS.UPDATED_BY, createdBy)
                        .execute();

                contractIds.add(contractId);
                ctx.putIdentifier(contractId, contractIdentifier);
                ctx.incrementContracts();
            }

            ctx.getContractIdsByTeam().put(teamId, contractIds);
            log.info("Created {} contracts for team {}", contractCount, teamKey);
        }
    }
}
