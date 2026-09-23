package com.buurman.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.dto.response.PaymentArrearsResponse;
import com.buurman.dto.response.PaymentArrearsResponse.AgeingBucket;
import com.buurman.dto.response.PaymentArrearsResponse.ContactArrears;
import com.buurman.mapper.ContactMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentReminderRepository;
import com.buurman.repository.PaymentReminderRepository.ReminderSummary;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PaymentRepository.OverduePaymentRow;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * Builds the team's arrears picture: every past-due payment with an open balance, aged into
 * standard buckets and grouped by the tenant who owes it.
 */
@Service
@RequiredArgsConstructor
public class PaymentArrearsService {

  /** Standard ageing bands in days overdue. The last band is open-ended. */
  static final int[] BUCKET_STARTS = {1, 31, 61, 91};

  private final PaymentRepository paymentRepository;
  private final PaymentReminderRepository paymentReminderRepository;
  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final ContractPartyService contractPartyService;
  private final ContactMapper contactMapper;
  private final PropertyMapper propertyMapper;
  private final Clock clock;

  @Transactional(readOnly = true)
  public PaymentArrearsResponse getArrears(UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    LocalDate today = LocalDate.now(clock);
    List<OverduePaymentRow> rows = paymentRepository.findOverdueRows(teamId);

    Optional<String> currency =
        rows.stream()
            .map(OverduePaymentRow::currency)
            .findFirst()
            .or(() -> paymentRepository.findCurrencyByTeamId(teamId));

    if (rows.isEmpty()) {
      return new PaymentArrearsResponse(
          currency, BigDecimal.ZERO, 0, 0, 0, emptyBuckets(), List.of());
    }

    List<UUID> contractIds = rows.stream().map(OverduePaymentRow::contractId).distinct().toList();
    Map<UUID, Contract> contracts =
        contractRepository.findByIdsAndTeamId(contractIds, teamId).stream()
            .collect(Collectors.toMap(Contract::getId, Function.identity()));
    Map<UUID, Contact> primaryContacts =
        contractPartyService.getPrimaryContactsForContracts(contractIds, teamId);
    Map<UUID, ReminderSummary> reminders =
        paymentReminderRepository.summarizeByPaymentIds(
            rows.stream().map(OverduePaymentRow::paymentId).toList(), teamId);

    // Group rows by tenant; a payment without any linked contact groups under its contract.
    Map<String, List<OverduePaymentRow>> byTenant = new LinkedHashMap<>();
    Map<String, Optional<Contact>> tenantContacts = new LinkedHashMap<>();
    for (OverduePaymentRow row : rows) {
      Optional<Contact> contact =
          Optional.ofNullable(primaryContacts.get(row.contractId()))
              .filter(c -> row.contactId().map(id -> id.equals(c.getId())).orElse(true))
              .or(() -> Optional.ofNullable(primaryContacts.get(row.contractId())));
      String key = contact.map(c -> "contact:" + c.getId()).orElse("contract:" + row.contractId());
      byTenant.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
      tenantContacts.putIfAbsent(key, contact);
    }

    List<ContactArrears> contactArrears = new ArrayList<>();
    for (Map.Entry<String, List<OverduePaymentRow>> entry : byTenant.entrySet()) {
      List<OverduePaymentRow> group = entry.getValue();
      OverduePaymentRow oldest =
          group.stream().min(Comparator.comparing(OverduePaymentRow::dueDate)).orElseThrow();
      BigDecimal outstanding =
          group.stream()
              .map(OverduePaymentRow::outstanding)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      Optional<Contract> contract = Optional.ofNullable(contracts.get(oldest.contractId()));
      Optional<Instant> lastReminderAt =
          group.stream()
              .map(r -> reminders.get(r.paymentId()))
              .filter(Objects::nonNull)
              .flatMap(s -> s.lastSentAt().stream())
              .max(Comparator.naturalOrder());
      int reminderCount =
          group.stream()
              .map(r -> reminders.get(r.paymentId()))
              .filter(Objects::nonNull)
              .mapToInt(ReminderSummary::count)
              .sum();

      contactArrears.add(
          new ContactArrears(
              Optional.ofNullable(tenantContacts.get(entry.getKey()))
                  .flatMap(c -> c)
                  .map(contactMapper::toSummary),
              contract.flatMap(
                  c ->
                      propertyRepository
                          .findByIdAndTeamId(c.getPropertyId(), teamId)
                          .map(propertyMapper::toSummary)),
              contract.flatMap(Contract::getIdentifier),
              outstanding,
              group.size(),
              oldest.dueDate(),
              daysOverdue(oldest.dueDate(), today),
              lastReminderAt,
              reminderCount,
              group.stream().map(OverduePaymentRow::identifier).toList(),
              contract
                  .map(
                      c ->
                          PaymentReminderService.remindersEnabled(
                              c,
                              tenantContacts.getOrDefault(entry.getKey(), Optional.empty()),
                              today))
                  .orElse(false)));
    }
    contactArrears.sort(Comparator.comparing(ContactArrears::outstanding).reversed());

    BigDecimal total =
        rows.stream().map(OverduePaymentRow::outstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
    int oldestDays = rows.stream().mapToInt(r -> daysOverdue(r.dueDate(), today)).max().orElse(0);

    return new PaymentArrearsResponse(
        currency,
        total,
        rows.size(),
        contactArrears.size(),
        oldestDays,
        buildBuckets(rows, today),
        contactArrears);
  }

  static int daysOverdue(LocalDate dueDate, LocalDate today) {
    return (int) Math.max(0, ChronoUnit.DAYS.between(dueDate, today));
  }

  static List<AgeingBucket> buildBuckets(List<OverduePaymentRow> rows, LocalDate today) {
    List<AgeingBucket> buckets = new ArrayList<>();
    for (int i = 0; i < BUCKET_STARTS.length; i++) {
      int from = BUCKET_STARTS[i];
      Optional<Integer> to =
          i + 1 < BUCKET_STARTS.length ? Optional.of(BUCKET_STARTS[i + 1] - 1) : Optional.empty();
      BigDecimal amount = BigDecimal.ZERO;
      int count = 0;
      for (OverduePaymentRow row : rows) {
        int days = daysOverdue(row.dueDate(), today);
        boolean inBand = days >= from && to.map(t -> days <= t).orElse(true);
        if (inBand) {
          amount = amount.add(row.outstanding());
          count++;
        }
      }
      buckets.add(new AgeingBucket(bucketKey(from, to), from, to, amount, count));
    }
    return buckets;
  }

  private static List<AgeingBucket> emptyBuckets() {
    return buildBuckets(List.of(), LocalDate.EPOCH);
  }

  private static String bucketKey(int from, Optional<Integer> to) {
    return to.map(t -> from + "-" + t).orElse(from + "+");
  }
}
