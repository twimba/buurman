package com.buurman.service;

import static com.buurman.util.SidGenerator.newContactCreditId;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactCredit;
import com.buurman.domain.ContactCredit.CreditSource;
import com.buurman.domain.Contract;
import com.buurman.domain.Payment;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContactCreditIdentifier;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.dto.request.CreateContactCreditRequest;
import com.buurman.dto.request.RefundContactCreditRequest;
import com.buurman.dto.response.ContactCreditResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContactCreditRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Credits owed to a tenant: overpayments captured automatically, plus manual credit notes. */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContactCreditService {

  private final ContactCreditRepository creditRepository;
  private final ContactRepository contactRepository;
  private final ContractRepository contractRepository;
  private final PaymentRepository paymentRepository;
  private final TeamService teamService;
  private final AuditService auditService;
  private final MetricsService metricsService;
  private final Clock clock;

  @Transactional(readOnly = true)
  public List<ContactCreditResponse> getCredits(
      ContactIdentifier contactIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contact contact = contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId);
    List<ContactCredit> credits =
        creditRepository.findByContactIdAndTeamId(contact.getId(), teamId);
    Map<UUID, Sid> contracts =
        contractRepository
            .findByIdsAndTeamId(
                credits.stream()
                    .map(ContactCredit::getContractId)
                    .flatMap(Optional::stream)
                    .toList(),
                teamId)
            .stream()
            .filter(c -> c.getIdentifier().isPresent())
            .collect(Collectors.toMap(Contract::getId, c -> c.getIdentifier().orElseThrow()));
    Map<UUID, Sid> payments =
        paymentRepository
            .findByIdsAndTeamId(
                credits.stream()
                    .map(ContactCredit::getSourcePaymentId)
                    .flatMap(Optional::stream)
                    .toList(),
                teamId)
            .stream()
            .filter(p -> p.getIdentifier().isPresent())
            .collect(Collectors.toMap(Payment::getId, p -> p.getIdentifier().orElseThrow()));
    return credits.stream()
        .map(
            c ->
                toResponse(
                    c,
                    contact,
                    c.getContractId().map(contracts::get),
                    c.getSourcePaymentId().map(payments::get)))
        .toList();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContactCreditResponse createCreditNote(
      ContactIdentifier contactIdentifier,
      CreateContactCreditRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contact contact = contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId);
    Optional<Contract> contract =
        request
            .contractIdentifier()
            .map(cid -> contractRepository.getByIdentifierAndTeamId(cid, teamId));
    String currency =
        contract
            .map(c -> c.getRentAmount().currency())
            .orElseGet(() -> teamService.getDefaultCurrency(teamId));

    ContactCredit credit =
        ContactCredit.builder()
            .identifier(Optional.of(newContactCreditId()))
            .teamId(teamId)
            .contactId(contact.getId())
            .contractId(contract.map(Contract::getId))
            .amount(MoneyAmount.of(request.amount(), currency))
            .remainingAmount(request.amount())
            .source(CreditSource.CREDIT_NOTE)
            .reason(Optional.of(request.reason()))
            .createdBy(principal.getUserId())
            .updatedBy(principal.getUserId())
            .build();
    ContactCredit saved = creditRepository.save(credit);
    auditService.logCreate(teamId, "CONTACT_CREDIT", saved.getId(), principal.getUserId(), saved);
    metricsService.incrementCounter("contact.credit.created.total", "source", "CREDIT_NOTE");
    log.info(
        "Created credit note {} of {} {} for contact {} by user {}",
        saved.getIdentifier().orElseThrow(),
        request.amount(),
        currency,
        contactIdentifier,
        principal.getUserId());
    return toResponse(saved, contact);
  }

  /** Records that the remaining balance of a credit was paid back to the tenant. */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContactCreditResponse refund(
      ContactIdentifier contactIdentifier,
      ContactCreditIdentifier creditIdentifier,
      RefundContactCreditRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contact contact = contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId);
    ContactCredit credit = creditRepository.getByIdentifierAndTeamId(creditIdentifier, teamId);
    if (!credit.getContactId().equals(contact.getId())) {
      throw new BusinessRuleException("Credit does not belong to this contact");
    }
    if (credit.getRemainingAmount().signum() <= 0) {
      throw new BusinessRuleException("Credit has no remaining balance to refund");
    }
    ContactCredit before = credit.toBuilder().build();
    BigDecimal refunded = credit.getRemainingAmount();
    credit.setRemainingAmount(BigDecimal.ZERO);
    credit.setRefundedAt(
        Optional.of(request.refundDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC)));
    credit.setRefundNotes(request.notes());
    credit.setUpdatedBy(principal.getUserId());
    credit.setUpdatedAt(clock.instant());
    ContactCredit saved = creditRepository.save(credit);
    auditService.logUpdate(
        teamId,
        "CONTACT_CREDIT",
        saved.getId(),
        principal.getUserId(),
        before,
        saved,
        java.util.Map.of("refunded", refunded + " " + saved.getAmount().currency()));
    metricsService.incrementCounter("contact.credit.refunded.total");
    return toResponse(saved, contact);
  }

  ContactCreditResponse toResponse(ContactCredit credit, Contact contact) {
    return toResponse(
        credit,
        contact,
        credit
            .getContractId()
            .flatMap(id -> contractRepository.findByIdAndTeamId(id, credit.getTeamId()))
            .flatMap(Contract::getIdentifier),
        credit
            .getSourcePaymentId()
            .flatMap(id -> paymentRepository.findByIdAndTeamId(id, credit.getTeamId()))
            .flatMap(Payment::getIdentifier));
  }

  private static ContactCreditResponse toResponse(
      ContactCredit credit,
      Contact contact,
      Optional<Sid> contractIdentifier,
      Optional<Sid> sourcePayment) {
    return new ContactCreditResponse(
        credit.getIdentifier().orElseThrow(),
        contact.getIdentifier().orElseThrow(),
        contractIdentifier,
        credit.getAmount().value(),
        credit.getRemainingAmount(),
        credit.getAmount().currency(),
        credit.getSource(),
        credit.getReason(),
        sourcePayment,
        credit.getRefundedAt(),
        credit.getRefundNotes(),
        credit.getCreatedAt());
  }
}
