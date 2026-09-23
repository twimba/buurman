package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactType;
import com.buurman.domain.Contract;
import com.buurman.domain.DataRetentionStatus;
import com.buurman.domain.Sid;
import com.buurman.domain.TeamRole;
import com.buurman.dto.response.ContactSummary;
import com.buurman.dto.response.PaymentArrearsResponse;
import com.buurman.dto.response.PaymentArrearsResponse.AgeingBucket;
import com.buurman.mapper.ContactMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentReminderRepository;
import com.buurman.repository.PaymentReminderRepository.ReminderSummary;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PaymentRepository.OverduePaymentRow;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class PaymentArrearsServiceTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final LocalDate TODAY = LocalDate.of(2026, 3, 1);

  @Mock private PaymentRepository paymentRepository;
  @Mock private PaymentReminderRepository paymentReminderRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private ContractPartyService contractPartyService;
  @Mock private ContactMapper contactMapper;
  @Mock private PropertyMapper propertyMapper;

  private final Clock clock =
      Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  private PaymentArrearsService service;
  private UserPrincipal principal;

  @BeforeEach
  void setUp() {
    service =
        new PaymentArrearsService(
            paymentRepository,
            paymentReminderRepository,
            contractRepository,
            propertyRepository,
            contractPartyService,
            contactMapper,
            propertyMapper,
            clock);
    principal =
        new UserPrincipal(
            USER_ID,
            "usr_test",
            "kc-id",
            "test@example.com",
            "Test User",
            TEAM_ID,
            "team_test",
            TeamRole.TEAM_ADMIN,
            true);
  }

  private static OverduePaymentRow row(UUID contractId, String amount, LocalDate due) {
    return new OverduePaymentRow(
        UUID.randomUUID(),
        Sid.of("pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 22)),
        contractId,
        Optional.empty(),
        "EUR",
        new BigDecimal(amount),
        due);
  }

  @Nested
  @DisplayName("ageing buckets")
  class Buckets {

    @Test
    @DisplayName("bands are 1-30, 31-60, 61-90 and 91+ days and each row lands in exactly one")
    void bandsAreExclusive() {
      UUID contractId = UUID.randomUUID();
      List<OverduePaymentRow> rows =
          List.of(
              row(contractId, "100.00", TODAY.minusDays(1)),
              row(contractId, "200.00", TODAY.minusDays(30)),
              row(contractId, "300.00", TODAY.minusDays(31)),
              row(contractId, "400.00", TODAY.minusDays(60)),
              row(contractId, "500.00", TODAY.minusDays(61)),
              row(contractId, "600.00", TODAY.minusDays(90)),
              row(contractId, "700.00", TODAY.minusDays(91)),
              row(contractId, "800.00", TODAY.minusDays(400)));

      List<AgeingBucket> buckets = PaymentArrearsService.buildBuckets(rows, TODAY);

      assertThat(buckets)
          .extracting(AgeingBucket::key)
          .containsExactly("1-30", "31-60", "61-90", "91+");
      assertThat(buckets.get(0).amount()).isEqualByComparingTo("300.00");
      assertThat(buckets.get(0).count()).isEqualTo(2);
      assertThat(buckets.get(1).amount()).isEqualByComparingTo("700.00");
      assertThat(buckets.get(2).amount()).isEqualByComparingTo("1100.00");
      assertThat(buckets.get(3).amount()).isEqualByComparingTo("1500.00");
      assertThat(buckets.get(3).toDays()).isEmpty();
      assertThat(buckets.stream().mapToInt(AgeingBucket::count).sum()).isEqualTo(rows.size());
    }

    @Test
    @DisplayName("empty input yields four zero buckets")
    void emptyBuckets() {
      List<AgeingBucket> buckets = PaymentArrearsService.buildBuckets(List.of(), TODAY);
      assertThat(buckets).hasSize(4);
      assertThat(buckets).allMatch(b -> b.count() == 0 && b.amount().signum() == 0);
    }
  }

  @Nested
  @DisplayName("getArrears")
  class GetArrears {

    @Test
    @DisplayName("no overdue rows returns an empty picture with the team currency")
    void emptyArrears() {
      when(paymentRepository.findOverdueRows(TEAM_ID)).thenReturn(List.of());
      when(paymentRepository.findCurrencyByTeamId(TEAM_ID)).thenReturn(Optional.of("EUR"));

      PaymentArrearsResponse response = service.getArrears(principal);

      assertThat(response.currency()).contains("EUR");
      assertThat(response.totalOutstanding()).isEqualByComparingTo("0");
      assertThat(response.paymentCount()).isZero();
      assertThat(response.contactCount()).isZero();
      assertThat(response.buckets()).hasSize(4);
      assertThat(response.contacts()).isEmpty();
    }

    @Test
    @DisplayName("groups payments by tenant, sorts by outstanding and reports oldest days")
    void groupsByTenant() {
      UUID contractA = UUID.randomUUID();
      UUID contractB = UUID.randomUUID();
      Contact tenantA = new Contact();
      tenantA.setId(UUID.randomUUID());
      Contact tenantB = new Contact();
      tenantB.setId(UUID.randomUUID());

      OverduePaymentRow a1 = row(contractA, "500.00", TODAY.minusDays(45));
      OverduePaymentRow a2 = row(contractA, "500.00", TODAY.minusDays(15));
      OverduePaymentRow b1 = row(contractB, "1500.00", TODAY.minusDays(5));

      when(paymentRepository.findOverdueRows(TEAM_ID)).thenReturn(List.of(a1, a2, b1));
      when(contractRepository.findByIdsAndTeamId(anyCollection(), eq(TEAM_ID)))
          .thenReturn(
              List.of(
                  Contract.builder().id(contractA).propertyId(UUID.randomUUID()).build(),
                  Contract.builder().id(contractB).propertyId(UUID.randomUUID()).build()));
      when(contractPartyService.getPrimaryContactsForContracts(anyCollection(), eq(TEAM_ID)))
          .thenReturn(Map.of(contractA, tenantA, contractB, tenantB));
      when(paymentReminderRepository.summarizeByPaymentIds(anyCollection(), eq(TEAM_ID)))
          .thenReturn(
              Map.of(
                  a1.paymentId(),
                  new ReminderSummary(Optional.of(Instant.parse("2026-02-20T10:00:00Z")), 2)));
      when(contactMapper.toSummary(any(Contact.class)))
          .thenAnswer(
              inv -> {
                Contact c = inv.getArgument(0);
                return new ContactSummary(
                    Sid.of("cnt_" + c.getId().toString().replace("-", "").substring(0, 22)),
                    ContactType.INDIVIDUAL,
                    "Tenant",
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    DataRetentionStatus.ACTIVE);
              });
      when(propertyRepository.findByIdAndTeamId(any(), eq(TEAM_ID))).thenReturn(Optional.empty());

      PaymentArrearsResponse response = service.getArrears(principal);

      assertThat(response.totalOutstanding()).isEqualByComparingTo("2500.00");
      assertThat(response.paymentCount()).isEqualTo(3);
      assertThat(response.contactCount()).isEqualTo(2);
      assertThat(response.oldestDaysOverdue()).isEqualTo(45);
      // Sorted by outstanding desc: tenant B (1500) first, tenant A (1000) second
      assertThat(response.contacts().get(0).outstanding()).isEqualByComparingTo("1500.00");
      assertThat(response.contacts().get(1).outstanding()).isEqualByComparingTo("1000.00");
      var tenantARow = response.contacts().get(1);
      assertThat(tenantARow.paymentCount()).isEqualTo(2);
      assertThat(tenantARow.daysOverdue()).isEqualTo(45);
      assertThat(tenantARow.oldestDueDate()).isEqualTo(TODAY.minusDays(45));
      assertThat(tenantARow.reminderCount()).isEqualTo(2);
      assertThat(tenantARow.lastReminderAt()).contains(Instant.parse("2026-02-20T10:00:00Z"));
      assertThat(tenantARow.paymentIdentifiers()).containsExactly(a1.identifier(), a2.identifier());
    }
  }
}
