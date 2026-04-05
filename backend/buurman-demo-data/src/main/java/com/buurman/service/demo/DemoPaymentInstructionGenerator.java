package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.CONTRACT_PAYMENT_INSTRUCTIONS;
import static com.buurman.jooq.generated.Tables.PAYMENT_INSTRUCTIONS;
import static com.buurman.util.SidGenerator.newContractPaymentInstructionId;
import static com.buurman.util.SidGenerator.newPaymentInstructionId;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import com.buurman.domain.Sid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class DemoPaymentInstructionGenerator {

  private final DSLContext dsl;
  private final Clock clock;
  private final Random random = new Random(42);

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);
      List<UUID> paymentInstructionIds = new ArrayList<>();

      // 1. Primary bank account (default)
      UUID piBank = UUID.randomUUID();
      // 2. Secondary bank account (direct debit)
      UUID piDebit = UUID.randomUUID();
      // 3. PayPal account
      UUID piPaypal = UUID.randomUUID();

      // Batch insert all 3 payment instructions
      List<Object[]> piRecords = new ArrayList<>();
      piRecords.add(
          new Object[] {
            piBank,
            newPaymentInstructionId(),
            teamId,
            "Primary Bank Account",
            "Main bank account for rent collection",
            "BANK_TRANSFER",
            "ING Bank",
            "Buurman Properties B.V.",
            "NL91INGB0417164300",
            "INGBNL2A",
            "Rent payment",
            null,
            true,
            now.minusDays(180),
            now.minusDays(180),
            createdBy,
            createdBy
          });
      piRecords.add(
          new Object[] {
            piDebit,
            newPaymentInstructionId(),
            teamId,
            "Direct Debit Account",
            "SEPA direct debit for automatic rent collection",
            "DIRECT_DEBIT",
            "ABN AMRO",
            "Buurman Properties B.V.",
            "NL02ABNA0457180536",
            "ABNANL2A",
            "SEPA Direct Debit",
            null,
            false,
            now.minusDays(120),
            now.minusDays(120),
            createdBy,
            createdBy
          });
      piRecords.add(
          new Object[] {
            piPaypal,
            newPaymentInstructionId(),
            teamId,
            "PayPal",
            "PayPal for international tenants",
            "PAYPAL",
            null,
            "payments@demo.buurman.io",
            null,
            null,
            null,
            "Please use 'Rent + Property Address' as payment note",
            false,
            now.minusDays(90),
            now.minusDays(90),
            createdBy,
            createdBy
          });

      var piInsert =
          dsl.insertInto(PAYMENT_INSTRUCTIONS)
              .columns(
                  PAYMENT_INSTRUCTIONS.ID,
                  PAYMENT_INSTRUCTIONS.IDENTIFIER,
                  PAYMENT_INSTRUCTIONS.TEAM_ID,
                  PAYMENT_INSTRUCTIONS.NAME,
                  PAYMENT_INSTRUCTIONS.DESCRIPTION,
                  PAYMENT_INSTRUCTIONS.PAYMENT_METHOD,
                  PAYMENT_INSTRUCTIONS.BANK_NAME,
                  PAYMENT_INSTRUCTIONS.ACCOUNT_HOLDER_NAME,
                  PAYMENT_INSTRUCTIONS.IBAN,
                  PAYMENT_INSTRUCTIONS.BIC_SWIFT,
                  PAYMENT_INSTRUCTIONS.PAYMENT_REFERENCE,
                  PAYMENT_INSTRUCTIONS.ADDITIONAL_DETAILS,
                  PAYMENT_INSTRUCTIONS.IS_DEFAULT,
                  PAYMENT_INSTRUCTIONS.CREATED_AT,
                  PAYMENT_INSTRUCTIONS.UPDATED_AT,
                  PAYMENT_INSTRUCTIONS.CREATED_BY,
                  PAYMENT_INSTRUCTIONS.UPDATED_BY)
              .values(
                  (UUID) null, (Sid) null, (UUID) null, (String) null, (String) null,
                  (String) null, (String) null, (String) null, (String) null, (String) null,
                  (String) null, (String) null, (Boolean) null, (LocalDateTime) null,
                  (LocalDateTime) null, (UUID) null, (UUID) null);
      var piBatch = dsl.batch(piInsert);
      for (Object[] r : piRecords) {
        piBatch = piBatch.bind(r);
      }
      piBatch.execute();

      paymentInstructionIds.add(piBank);
      paymentInstructionIds.add(piDebit);
      paymentInstructionIds.add(piPaypal);
      ctx.getPaymentInstructionIdsByTeam().put(teamId, paymentInstructionIds);
      log.info(
          "Created {} payment instructions for team {}", paymentInstructionIds.size(), teamKey);

      // Link contracts to payment instructions
      List<UUID> contractIds = ctx.getContractIdsByTeam().get(teamId);
      if (contractIds == null || contractIds.isEmpty()) {
        continue;
      }

      List<Object[]> cpiRecords = new ArrayList<>();
      int linked = 0;

      for (int i = 0; i < contractIds.size(); i++) {
        UUID contractId = contractIds.get(i);
        LocalDate effectiveFrom = LocalDate.now(clock).minusMonths(random.nextInt(6, 18));

        if (i % 3 == 2) {
          // Custom payment instruction (every 3rd contract)
          cpiRecords.add(
              new Object[] {
                UUID.randomUUID(),
                newContractPaymentInstructionId(),
                teamId,
                contractId,
                null,
                true,
                "Cash Payment",
                "Monthly cash payment at landlord office",
                "CASH",
                "Payment due on 1st of each month at the management office. Receipt will be"
                    + " provided.",
                effectiveFrom,
                "Tenant preferred cash payments",
                now.minusDays(random.nextInt(30, 180)),
                now,
                createdBy,
                createdBy
              });
        } else {
          // Template-based: alternate between bank transfer and direct debit
          UUID piId = (i % 2 == 0) ? piBank : piDebit;
          cpiRecords.add(
              new Object[] {
                UUID.randomUUID(),
                newContractPaymentInstructionId(),
                teamId,
                contractId,
                piId,
                false,
                null,
                null,
                null,
                null,
                effectiveFrom,
                null,
                now.minusDays(random.nextInt(30, 180)),
                now,
                createdBy,
                createdBy
              });
        }
        linked++;
      }

      if (!cpiRecords.isEmpty()) {
        var cpiInsert =
            dsl.insertInto(CONTRACT_PAYMENT_INSTRUCTIONS)
                .columns(
                    CONTRACT_PAYMENT_INSTRUCTIONS.ID,
                    CONTRACT_PAYMENT_INSTRUCTIONS.IDENTIFIER,
                    CONTRACT_PAYMENT_INSTRUCTIONS.TEAM_ID,
                    CONTRACT_PAYMENT_INSTRUCTIONS.CONTRACT_ID,
                    CONTRACT_PAYMENT_INSTRUCTIONS.PAYMENT_INSTRUCTION_ID,
                    CONTRACT_PAYMENT_INSTRUCTIONS.IS_CUSTOM,
                    CONTRACT_PAYMENT_INSTRUCTIONS.CUSTOM_NAME,
                    CONTRACT_PAYMENT_INSTRUCTIONS.CUSTOM_DESCRIPTION,
                    CONTRACT_PAYMENT_INSTRUCTIONS.CUSTOM_PAYMENT_METHOD,
                    CONTRACT_PAYMENT_INSTRUCTIONS.CUSTOM_ADDITIONAL_DETAILS,
                    CONTRACT_PAYMENT_INSTRUCTIONS.EFFECTIVE_FROM,
                    CONTRACT_PAYMENT_INSTRUCTIONS.NOTES,
                    CONTRACT_PAYMENT_INSTRUCTIONS.CREATED_AT,
                    CONTRACT_PAYMENT_INSTRUCTIONS.UPDATED_AT,
                    CONTRACT_PAYMENT_INSTRUCTIONS.CREATED_BY,
                    CONTRACT_PAYMENT_INSTRUCTIONS.UPDATED_BY)
                .values(
                    (UUID) null, (Sid) null, (UUID) null, (UUID) null, (UUID) null,
                    (Boolean) null, (String) null, (String) null, (String) null, (String) null,
                    (LocalDate) null, (String) null, (LocalDateTime) null, (LocalDateTime) null,
                    (UUID) null, (UUID) null);
        var cpiBatch = dsl.batch(cpiInsert);
        for (Object[] r : cpiRecords) {
          cpiBatch = cpiBatch.bind(r);
        }
        cpiBatch.execute();
      }

      log.info("Linked {} contracts to payment instructions for team {}", linked, teamKey);
    }
  }
}
