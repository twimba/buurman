package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PAYMENT_PLANS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PaymentPlan;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.jooq.generated.tables.records.PaymentPlansRecord;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentPlanRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public List<PaymentPlan> findByContractIdAndTeamId(UUID contractId, UUID teamId) {
    return dsl.selectFrom(PAYMENT_PLANS)
        .where(
            PAYMENT_PLANS
                .CONTRACT_ID
                .eq(contractId)
                .and(PAYMENT_PLANS.TEAM_ID.eq(teamId))
                .and(PAYMENT_PLANS.DELETED_AT.isNull()))
        .orderBy(PAYMENT_PLANS.CREATED_AT.desc())
        .fetch()
        .map(this::toDomain);
  }

  public Optional<PaymentPlan> findByIdAndTeamId(UUID id, UUID teamId) {
    return dsl.selectFrom(PAYMENT_PLANS)
        .where(
            PAYMENT_PLANS
                .ID
                .eq(id)
                .and(PAYMENT_PLANS.TEAM_ID.eq(teamId))
                .and(PAYMENT_PLANS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public PaymentPlan getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(PAYMENT_PLANS)
        .where(
            PAYMENT_PLANS
                .IDENTIFIER
                .eq(identifier)
                .and(PAYMENT_PLANS.TEAM_ID.eq(teamId))
                .and(PAYMENT_PLANS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain)
        .orElseThrow(() -> new NotFoundException("Payment plan not found"));
  }

  public PaymentPlan save(PaymentPlan plan) {
    LocalDateTime now = LocalDateTime.now(clock);
    if (plan.getId() == null) {
      UUID id = UUID.randomUUID();
      dsl.insertInto(PAYMENT_PLANS)
          .set(PAYMENT_PLANS.ID, id)
          .set(PAYMENT_PLANS.IDENTIFIER, plan.getIdentifier().orElseThrow())
          .set(PAYMENT_PLANS.TEAM_ID, plan.getTeamId())
          .set(PAYMENT_PLANS.CONTRACT_ID, plan.getContractId())
          .set(PAYMENT_PLANS.CONTACT_ID, plan.getContactId().orElse(null))
          .set(PAYMENT_PLANS.TOTAL_AMOUNT, plan.getTotalAmount().value())
          .set(PAYMENT_PLANS.CURRENCY, plan.getTotalAmount().currency())
          .set(PAYMENT_PLANS.INSTALMENT_COUNT, plan.getInstalmentCount())
          .set(PAYMENT_PLANS.START_DATE, plan.getStartDate())
          .set(PAYMENT_PLANS.FREQUENCY, plan.getFrequency().name())
          .set(PAYMENT_PLANS.STATUS, plan.getStatus().name())
          .set(PAYMENT_PLANS.NOTES, plan.getNotes().orElse(null))
          .set(PAYMENT_PLANS.CREATED_AT, now)
          .set(PAYMENT_PLANS.UPDATED_AT, now)
          .set(PAYMENT_PLANS.CREATED_BY, plan.getCreatedBy())
          .set(PAYMENT_PLANS.UPDATED_BY, plan.getUpdatedBy())
          .execute();
      plan.setId(id);
      plan.setCreatedAt(now.toInstant(UTC));
      plan.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(PAYMENT_PLANS)
          .set(PAYMENT_PLANS.STATUS, plan.getStatus().name())
          .set(PAYMENT_PLANS.NOTES, plan.getNotes().orElse(null))
          .set(PAYMENT_PLANS.CANCEL_REASON, plan.getCancelReason().orElse(null))
          .set(PAYMENT_PLANS.UPDATED_AT, now)
          .set(PAYMENT_PLANS.UPDATED_BY, plan.getUpdatedBy())
          .where(PAYMENT_PLANS.ID.eq(plan.getId()).and(PAYMENT_PLANS.TEAM_ID.eq(plan.getTeamId())))
          .execute();
      plan.setUpdatedAt(now.toInstant(UTC));
    }
    return plan;
  }

  private PaymentPlan toDomain(PaymentPlansRecord r) {
    PaymentPlan p = new PaymentPlan();
    p.setId(r.getId());
    p.setIdentifier(Optional.of(r.getIdentifier()));
    p.setTeamId(r.getTeamId());
    p.setContractId(r.getContractId());
    p.setContactId(Optional.ofNullable(r.getContactId()));
    p.setTotalAmount(MoneyAmount.of(r.getTotalAmount(), r.getCurrency()));
    p.setInstalmentCount(r.getInstalmentCount());
    p.setStartDate(r.getStartDate());
    p.setFrequency(PaymentPlan.Frequency.valueOf(r.getFrequency()));
    p.setStatus(PaymentPlan.PlanStatus.valueOf(r.getStatus()));
    p.setNotes(Optional.ofNullable(r.getNotes()));
    p.setCancelReason(Optional.ofNullable(r.getCancelReason()));
    p.setCreatedAt(r.getCreatedAt().toInstant(UTC));
    p.setUpdatedAt(r.getUpdatedAt().toInstant(UTC));
    p.setCreatedBy(r.getCreatedBy());
    p.setUpdatedBy(r.getUpdatedBy());
    p.setDeletedAt(Optional.ofNullable(r.getDeletedAt()).map(dt -> dt.toInstant(UTC)));
    return p;
  }
}
