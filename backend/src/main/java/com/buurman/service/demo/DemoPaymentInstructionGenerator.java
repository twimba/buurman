package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.CONTRACT_PAYMENT_INSTRUCTIONS;
import static com.buurman.jooq.generated.Tables.PAYMENT_INSTRUCTIONS;
import static com.buurman.util.UlidGenerator.newContractPaymentInstructionId;
import static com.buurman.util.UlidGenerator.newPaymentInstructionId;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

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
      dsl.insertInto(PAYMENT_INSTRUCTIONS)
          .set(PAYMENT_INSTRUCTIONS.ID, piBank)
          .set(PAYMENT_INSTRUCTIONS.IDENTIFIER, newPaymentInstructionId().value())
          .set(PAYMENT_INSTRUCTIONS.TEAM_ID, teamId)
          .set(PAYMENT_INSTRUCTIONS.NAME, "Primary Bank Account")
          .set(PAYMENT_INSTRUCTIONS.DESCRIPTION, "Main bank account for rent collection")
          .set(PAYMENT_INSTRUCTIONS.PAYMENT_METHOD, "BANK_TRANSFER")
          .set(PAYMENT_INSTRUCTIONS.BANK_NAME, "ING Bank")
          .set(PAYMENT_INSTRUCTIONS.ACCOUNT_HOLDER_NAME, "Buurman Properties B.V.")
          .set(PAYMENT_INSTRUCTIONS.IBAN, "NL91INGB0417164300")
          .set(PAYMENT_INSTRUCTIONS.BIC_SWIFT, "INGBNL2A")
          .set(PAYMENT_INSTRUCTIONS.PAYMENT_REFERENCE, "Rent payment")
          .set(PAYMENT_INSTRUCTIONS.IS_DEFAULT, true)
          .set(PAYMENT_INSTRUCTIONS.CREATED_AT, now.minusDays(180))
          .set(PAYMENT_INSTRUCTIONS.UPDATED_AT, now.minusDays(180))
          .set(PAYMENT_INSTRUCTIONS.CREATED_BY, createdBy)
          .set(PAYMENT_INSTRUCTIONS.UPDATED_BY, createdBy)
          .execute();
      paymentInstructionIds.add(piBank);

      // 2. Secondary bank account (direct debit)
      UUID piDebit = UUID.randomUUID();
      dsl.insertInto(PAYMENT_INSTRUCTIONS)
          .set(PAYMENT_INSTRUCTIONS.ID, piDebit)
          .set(PAYMENT_INSTRUCTIONS.IDENTIFIER, newPaymentInstructionId().value())
          .set(PAYMENT_INSTRUCTIONS.TEAM_ID, teamId)
          .set(PAYMENT_INSTRUCTIONS.NAME, "Direct Debit Account")
          .set(PAYMENT_INSTRUCTIONS.DESCRIPTION, "SEPA direct debit for automatic rent collection")
          .set(PAYMENT_INSTRUCTIONS.PAYMENT_METHOD, "DIRECT_DEBIT")
          .set(PAYMENT_INSTRUCTIONS.BANK_NAME, "ABN AMRO")
          .set(PAYMENT_INSTRUCTIONS.ACCOUNT_HOLDER_NAME, "Buurman Properties B.V.")
          .set(PAYMENT_INSTRUCTIONS.IBAN, "NL02ABNA0457180536")
          .set(PAYMENT_INSTRUCTIONS.BIC_SWIFT, "ABNANL2A")
          .set(PAYMENT_INSTRUCTIONS.PAYMENT_REFERENCE, "SEPA Direct Debit")
          .set(PAYMENT_INSTRUCTIONS.IS_DEFAULT, false)
          .set(PAYMENT_INSTRUCTIONS.CREATED_AT, now.minusDays(120))
          .set(PAYMENT_INSTRUCTIONS.UPDATED_AT, now.minusDays(120))
          .set(PAYMENT_INSTRUCTIONS.CREATED_BY, createdBy)
          .set(PAYMENT_INSTRUCTIONS.UPDATED_BY, createdBy)
          .execute();
      paymentInstructionIds.add(piDebit);

      // 3. PayPal account
      UUID piPaypal = UUID.randomUUID();
      dsl.insertInto(PAYMENT_INSTRUCTIONS)
          .set(PAYMENT_INSTRUCTIONS.ID, piPaypal)
          .set(PAYMENT_INSTRUCTIONS.IDENTIFIER, newPaymentInstructionId().value())
          .set(PAYMENT_INSTRUCTIONS.TEAM_ID, teamId)
          .set(PAYMENT_INSTRUCTIONS.NAME, "PayPal")
          .set(PAYMENT_INSTRUCTIONS.DESCRIPTION, "PayPal for international tenants")
          .set(PAYMENT_INSTRUCTIONS.PAYMENT_METHOD, "PAYPAL")
          .set(PAYMENT_INSTRUCTIONS.ACCOUNT_HOLDER_NAME, "payments@demo.buurman.io")
          .set(
              PAYMENT_INSTRUCTIONS.ADDITIONAL_DETAILS,
              "Please use 'Rent + Property Address' as payment note")
          .set(PAYMENT_INSTRUCTIONS.IS_DEFAULT, false)
          .set(PAYMENT_INSTRUCTIONS.CREATED_AT, now.minusDays(90))
          .set(PAYMENT_INSTRUCTIONS.UPDATED_AT, now.minusDays(90))
          .set(PAYMENT_INSTRUCTIONS.CREATED_BY, createdBy)
          .set(PAYMENT_INSTRUCTIONS.UPDATED_BY, createdBy)
          .execute();
      paymentInstructionIds.add(piPaypal);

      ctx.getPaymentInstructionIdsByTeam().put(teamId, paymentInstructionIds);
      log.info(
          "Created {} payment instructions for team {}", paymentInstructionIds.size(), teamKey);

      // Link contracts to payment instructions
      List<UUID> contractIds = ctx.getContractIdsByTeam().get(teamId);
      if (contractIds == null || contractIds.isEmpty()) {
        continue;
      }

      int linked = 0;
      for (int i = 0; i < contractIds.size(); i++) {
        UUID contractId = contractIds.get(i);
        LocalDate effectiveFrom = LocalDate.now(clock).minusMonths(random.nextInt(6, 18));

        if (i % 3 == 2) {
          // Custom payment instruction (every 3rd contract)
          dsl.insertInto(CONTRACT_PAYMENT_INSTRUCTIONS)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.ID, UUID.randomUUID())
              .set(
                  CONTRACT_PAYMENT_INSTRUCTIONS.IDENTIFIER,
                  newContractPaymentInstructionId().value())
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.TEAM_ID, teamId)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.CONTRACT_ID, contractId)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.IS_CUSTOM, true)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.CUSTOM_NAME, "Cash Payment")
              .set(
                  CONTRACT_PAYMENT_INSTRUCTIONS.CUSTOM_DESCRIPTION,
                  "Monthly cash payment at landlord office")
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.CUSTOM_PAYMENT_METHOD, "CASH")
              .set(
                  CONTRACT_PAYMENT_INSTRUCTIONS.CUSTOM_ADDITIONAL_DETAILS,
                  "Payment due on 1st of each month at the management office. Receipt will be"
                      + " provided.")
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.EFFECTIVE_FROM, effectiveFrom)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.NOTES, "Tenant preferred cash payments")
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.CREATED_AT, now.minusDays(random.nextInt(30, 180)))
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.UPDATED_AT, now)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.CREATED_BY, createdBy)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.UPDATED_BY, createdBy)
              .execute();
        } else {
          // Template-based: alternate between bank transfer and direct debit
          UUID piId = (i % 2 == 0) ? piBank : piDebit;
          dsl.insertInto(CONTRACT_PAYMENT_INSTRUCTIONS)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.ID, UUID.randomUUID())
              .set(
                  CONTRACT_PAYMENT_INSTRUCTIONS.IDENTIFIER,
                  newContractPaymentInstructionId().value())
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.TEAM_ID, teamId)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.CONTRACT_ID, contractId)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.PAYMENT_INSTRUCTION_ID, piId)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.IS_CUSTOM, false)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.EFFECTIVE_FROM, effectiveFrom)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.CREATED_AT, now.minusDays(random.nextInt(30, 180)))
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.UPDATED_AT, now)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.CREATED_BY, createdBy)
              .set(CONTRACT_PAYMENT_INSTRUCTIONS.UPDATED_BY, createdBy)
              .execute();
        }
        linked++;
      }

      log.info("Linked {} contracts to payment instructions for team {}", linked, teamKey);
    }
  }
}
