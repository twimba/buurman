package com.buurman.service;

import static com.buurman.domain.TeamRole.TEAM_ADMIN;
import static com.buurman.util.UlidGenerator.newCalendarFeedId;
import static com.buurman.util.UlidGenerator.newToken;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Ulid;
import com.buurman.domain.CalendarFeed;
import com.buurman.domain.Contract;
import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.domain.Tenant;
import com.buurman.dto.request.CreateCalendarFeedRequest;
import com.buurman.dto.response.CalendarFeedResponse;
import com.buurman.repository.CalendarFeedRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TenantRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CalendarFeedService {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final CalendarFeedRepository calendarFeedRepository;
  private final PaymentRepository paymentRepository;
  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final TenantRepository tenantRepository;
  private final ContractPartyService contractPartyService;
  private final AppProperties appProperties;

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public List<CalendarFeedResponse> getUserFeeds(UserPrincipal principal) {
    List<CalendarFeed> feeds =
        calendarFeedRepository.findByUserIdAndTeamId(
            principal.getUserId(), principal.requireTeamId());
    return feeds.stream().map(f -> toResponse(f, principal.requireTeamId())).toList();
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public CalendarFeedResponse createFeed(
      CreateCalendarFeedRequest request, UserPrincipal principal) {
    Optional<UUID> contractId = Optional.empty();
    Optional<UUID> propertyId = Optional.empty();
    Optional<UUID> tenantId = Optional.empty();

    switch (request.feedType()) {
      case CONTRACT -> {
        Ulid contractUlid =
            request
                .contractIdentifier()
                .filter(s -> !s.isBlank())
                .map(Ulid::of)
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "contractIdentifier is required for CONTRACT feed type"));
        Contract contract =
            contractRepository.getByIdentifierAndTeamId(contractUlid, principal.requireTeamId());
        contractId = Optional.of(contract.getId());
      }
      case PROPERTY_PAYMENTS -> {
        Ulid propertyUlid =
            request
                .propertyIdentifier()
                .filter(s -> !s.isBlank())
                .map(Ulid::of)
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "propertyIdentifier is required for PROPERTY_PAYMENTS feed type"));
        Property property =
            propertyRepository.getByIdentifierAndTeamId(propertyUlid, principal.requireTeamId());
        propertyId = Optional.of(property.getId());
      }
      case TENANT_PAYMENTS -> {
        Ulid tenantUlid =
            request
                .tenantIdentifier()
                .filter(s -> !s.isBlank())
                .map(Ulid::of)
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "tenantIdentifier is required for TENANT_PAYMENTS feed type"));
        Tenant tenant =
            tenantRepository.getByIdentifierAndTeamId(tenantUlid, principal.requireTeamId());
        tenantId = Optional.of(tenant.getId());
      }
      case ALL_PAYMENTS -> {
        /* no entity needed */
      }
    }

    // Idempotent: return existing feed if same type+entity already exists
    Optional<CalendarFeed> existing =
        calendarFeedRepository.findExistingFeed(
            request.feedType(),
            contractId,
            propertyId,
            tenantId,
            principal.getUserId(),
            principal.requireTeamId());
    if (existing.isPresent()) {
      return toResponse(existing.get(), principal.requireTeamId());
    }

    CalendarFeed feed = new CalendarFeed();
    feed.setIdentifier(Optional.of(newCalendarFeedId()));
    feed.setTeamId(principal.requireTeamId());
    feed.setUserId(principal.getUserId());
    feed.setFeedToken(newToken().value() + newToken().value());
    feed.setFeedType(request.feedType());
    feed.setContractId(contractId);
    feed.setPropertyId(propertyId);
    feed.setTenantId(tenantId);
    feed.setEnabled(true);
    feed.setCreatedBy(principal.getUserId());
    feed.setUpdatedBy(principal.getUserId());

    CalendarFeed saved = calendarFeedRepository.save(feed);
    return toResponse(saved, principal.requireTeamId());
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public CalendarFeedResponse rotateFeedToken(Ulid identifier, UserPrincipal principal) {
    CalendarFeed feed =
        calendarFeedRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    verifyOwnership(feed, principal);

    feed.setFeedToken(newToken().value() + newToken().value());
    feed.setUpdatedBy(principal.getUserId());
    feed.setUpdatedAt(java.time.Instant.now()); // repository will override with DB timestamp

    CalendarFeed saved = calendarFeedRepository.save(feed);
    return toResponse(saved, principal.requireTeamId());
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void deleteFeed(Ulid identifier, UserPrincipal principal) {
    CalendarFeed feed =
        calendarFeedRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    verifyOwnership(feed, principal);

    calendarFeedRepository.softDeleteByIdAndTeamId(feed.getId(), feed.getTeamId());
  }

  public Optional<String> generateICalFeed(String feedToken) {
    Optional<CalendarFeed> feedOpt = calendarFeedRepository.findByFeedToken(feedToken);
    if (feedOpt.isEmpty()) {
      return Optional.empty();
    }

    CalendarFeed feed = feedOpt.get();
    UUID teamId = feed.getTeamId();

    List<Payment> payments = loadPayments(feed, teamId);

    // Filter out cancelled payments
    payments =
        payments.stream().filter(p -> p.getStatus() != Payment.PaymentStatus.CANCELLED).toList();

    // Batch load related entities to avoid N+1
    Set<UUID> contractIds = payments.stream().map(Payment::getContractId).collect(toSet());

    // Load contracts whose milestones should appear in the feed
    List<Contract> milestoneContracts = new ArrayList<>();
    switch (feed.getFeedType()) {
      case CONTRACT ->
          feed.getContractId()
              .ifPresent(
                  cId -> {
                    contractIds.add(cId);
                    contractRepository
                        .findByIdAndTeamId(cId, teamId)
                        .ifPresent(milestoneContracts::add);
                  });
      case PROPERTY_PAYMENTS ->
          feed.getPropertyId()
              .ifPresent(
                  pId -> {
                    List<Contract> propContracts = contractRepository.findByPropertyId(pId, teamId);
                    milestoneContracts.addAll(propContracts);
                    propContracts.forEach(c -> contractIds.add(c.getId()));
                  });
      case TENANT_PAYMENTS ->
          feed.getTenantId()
              .ifPresent(
                  tId -> {
                    List<Contract> tenContracts =
                        contractRepository.findByTenantIdViaParties(tId, teamId);
                    milestoneContracts.addAll(tenContracts);
                    tenContracts.forEach(c -> contractIds.add(c.getId()));
                  });
      case ALL_PAYMENTS -> {
        /* no milestones */
      }
    }

    Map<UUID, Contract> contractMap =
        contractRepository.findByIdsAndTeamId(contractIds, teamId).stream()
            .collect(toMap(Contract::getId, identity()));

    Set<UUID> propertyIds =
        contractMap.values().stream().map(Contract::getPropertyId).collect(toSet());
    Map<UUID, Property> propertyMap =
        propertyRepository.findByIdsAndTeamId(propertyIds, teamId).stream()
            .collect(toMap(Property::getId, identity()));

    Map<UUID, Tenant> primaryTenantByContract =
        contractPartyService.getPrimaryTenantsForContracts(contractMap.keySet(), teamId);
    // Build tenantMap (tenantId → Tenant) for calendar name building
    Map<UUID, Tenant> tenantMap = new HashMap<>();
    primaryTenantByContract.values().forEach(t -> tenantMap.put(t.getId(), t));

    String calName = buildCalendarName(feed, teamId, propertyMap, tenantMap);

    StringBuilder sb = new StringBuilder();
    sb.append("BEGIN:VCALENDAR\r\n");
    sb.append("VERSION:2.0\r\n");
    sb.append("PRODID:-//Buurman//Calendar//EN\r\n");
    sb.append("CALSCALE:GREGORIAN\r\n");
    sb.append("METHOD:PUBLISH\r\n");
    appendFolded(sb, "X-WR-CALNAME:" + calName);
    sb.append("X-WR-TIMEZONE:UTC\r\n");

    // Emit contract milestone events (start, end, signed dates)
    for (Contract contract : milestoneContracts) {
      Property property = propertyMap.get(contract.getPropertyId());
      String propertyLabel = property != null ? property.getStreet() : "Contract";

      sb.append("BEGIN:VEVENT\r\n");
      appendFolded(sb, "UID:contract-start-" + contract.getIdentifier().orElseThrow() + "@buurman.app");
      sb.append("DTSTART;VALUE=DATE:")
          .append(contract.getStartDate().format(DATE_FORMAT))
          .append("\r\n");
      appendFolded(sb, "SUMMARY:" + escapeText("Contract Start - " + propertyLabel));
      appendFolded(
          sb, "DESCRIPTION:" + escapeText("Contract #" + contract.getIdentifier().orElseThrow() + " starts"));
      sb.append("STATUS:CONFIRMED\r\n");
      sb.append("TRANSP:TRANSPARENT\r\n");
      sb.append("END:VEVENT\r\n");

      if (contract.getEndDate().isPresent()) {
        sb.append("BEGIN:VEVENT\r\n");
        appendFolded(sb, "UID:contract-end-" + contract.getIdentifier().orElseThrow() + "@buurman.app");
        sb.append("DTSTART;VALUE=DATE:")
            .append(contract.getEndDate().get().format(DATE_FORMAT))
            .append("\r\n");
        appendFolded(sb, "SUMMARY:" + escapeText("Contract End - " + propertyLabel));
        appendFolded(
            sb, "DESCRIPTION:" + escapeText("Contract #" + contract.getIdentifier().orElseThrow() + " ends"));
        sb.append("STATUS:CONFIRMED\r\n");
        sb.append("TRANSP:TRANSPARENT\r\n");
        sb.append("END:VEVENT\r\n");
      }

      if (contract.getSignedDate().isPresent()) {
        sb.append("BEGIN:VEVENT\r\n");
        appendFolded(sb, "UID:contract-signed-" + contract.getIdentifier().orElseThrow() + "@buurman.app");
        sb.append("DTSTART;VALUE=DATE:")
            .append(contract.getSignedDate().get().format(DATE_FORMAT))
            .append("\r\n");
        appendFolded(sb, "SUMMARY:" + escapeText("Contract Signed - " + propertyLabel));
        appendFolded(
            sb, "DESCRIPTION:" + escapeText("Contract #" + contract.getIdentifier().orElseThrow() + " signed"));
        sb.append("STATUS:CONFIRMED\r\n");
        sb.append("TRANSP:TRANSPARENT\r\n");
        sb.append("END:VEVENT\r\n");
      }
    }

    // Emit payment events
    for (Payment payment : payments) {
      Contract contract = contractMap.get(payment.getContractId());
      Property property = contract != null ? propertyMap.get(contract.getPropertyId()) : null;
      Tenant tenant = contract != null ? primaryTenantByContract.get(contract.getId()) : null;

      String summary = buildSummary(property);
      String description = buildDescription(payment, contract, property, tenant);

      sb.append("BEGIN:VEVENT\r\n");
      appendFolded(sb, "UID:" + payment.getIdentifier().orElseThrow() + "@buurman.app");
      sb.append("DTSTART;VALUE=DATE:")
          .append(payment.getDueDate().format(DATE_FORMAT))
          .append("\r\n");
      appendFolded(sb, "SUMMARY:" + escapeText(summary));
      appendFolded(sb, "DESCRIPTION:" + escapeText(description));
      sb.append("STATUS:CONFIRMED\r\n");
      sb.append("TRANSP:TRANSPARENT\r\n");
      sb.append("END:VEVENT\r\n");
    }

    sb.append("END:VCALENDAR\r\n");
    return Optional.of(sb.toString());
  }

  private List<Payment> loadPayments(CalendarFeed feed, UUID teamId) {
    return switch (feed.getFeedType()) {
      case ALL_PAYMENTS -> paymentRepository.findAllByTeamId(teamId);
      case CONTRACT ->
          paymentRepository.findByContractId(
              feed.getContractId()
                  .orElseThrow(() -> new IllegalStateException("CONTRACT feed missing contractId")),
              teamId);
      case PROPERTY_PAYMENTS -> {
        List<Contract> contracts =
            contractRepository.findByPropertyId(
                feed.getPropertyId()
                    .orElseThrow(
                        () -> new IllegalStateException("PROPERTY feed missing propertyId")),
                teamId);
        List<Payment> result = new ArrayList<>();
        for (Contract c : contracts) {
          result.addAll(paymentRepository.findByContractId(c.getId(), teamId));
        }
        yield result;
      }
      case TENANT_PAYMENTS -> {
        List<Contract> contracts =
            contractRepository.findByTenantIdViaParties(
                feed.getTenantId()
                    .orElseThrow(() -> new IllegalStateException("TENANT feed missing tenantId")),
                teamId);
        List<Payment> result = new ArrayList<>();
        for (Contract c : contracts) {
          result.addAll(paymentRepository.findByContractId(c.getId(), teamId));
        }
        yield result;
      }
    };
  }

  private String buildCalendarName(
      CalendarFeed feed,
      UUID teamId,
      Map<UUID, Property> propertyMap,
      Map<UUID, Tenant> tenantMap) {
    return switch (feed.getFeedType()) {
      case ALL_PAYMENTS -> "Buurman - All Payment Due Dates";
      case CONTRACT -> "Buurman - Contract Calendar";
      case PROPERTY_PAYMENTS -> {
        UUID pId =
            feed.getPropertyId()
                .orElseThrow(() -> new IllegalStateException("PROPERTY feed missing propertyId"));
        Property p =
            propertyMap.values().stream()
                .filter(prop -> prop.getId().equals(pId))
                .findFirst()
                .orElse(null);
        if (p == null) {
          p = propertyRepository.findByIdAndTeamId(pId, teamId).orElse(null);
        }
        String name = p != null ? p.getStreet() : "Property";
        yield "Buurman - " + name + " Payments";
      }
      case TENANT_PAYMENTS -> {
        UUID tId =
            feed.getTenantId()
                .orElseThrow(() -> new IllegalStateException("TENANT feed missing tenantId"));
        Tenant tenant =
            tenantMap.values().stream()
                .filter(tn -> tn.getId().equals(tId))
                .findFirst()
                .orElse(null);
        if (tenant == null) {
          tenant = tenantRepository.findByIdAndTeamId(tId, teamId).orElse(null);
        }
        String name =
            tenant != null
                ? tenant.getFirstName() + tenant.getLastName().map(n -> " " + n).orElse("")
                : "Tenant";
        yield "Buurman - " + name + " Payments";
      }
    };
  }

  private String buildSummary(@Nullable Property property) {
    if (property != null) {
      return "Rent Due - %s, %s".formatted(property.getStreet(), property.getCity());
    }
    return "Rent Payment Due";
  }

  private String buildDescription(
      Payment payment,
      @Nullable Contract contract,
      @Nullable Property property,
      @Nullable Tenant tenant) {
    StringBuilder desc = new StringBuilder();
    String currency = payment.getCurrency();
    desc.append("Amount: ")
        .append(currency)
        .append(" ")
        .append(payment.getAmount().toPlainString());
    if (tenant != null) {
      desc.append("\\nTenant: ").append(tenant.getFirstName());
      tenant.getLastName().ifPresent(n -> desc.append(" ").append(n));
    }
    if (property != null) {
      desc.append("\\nProperty: ").append(property.getStreet());
    }
    if (contract != null) {
      desc.append("\\nContract: #").append(contract.getIdentifier().orElseThrow());
    }
    desc.append("\\nStatus: ").append(payment.getStatus().name());
    return desc.toString();
  }

  private CalendarFeedResponse toResponse(CalendarFeed feed, UUID teamId) {
    Ulid contractIdentifier = null;
    Ulid propertyIdentifier = null;
    Ulid tenantIdentifier = null;
    String entityLabel = null;

    if (feed.getContractId().isPresent()) {
      UUID cId = feed.getContractId().get();
      Optional<Contract> contractOpt = contractRepository.findByIdAndTeamId(cId, teamId);
      if (contractOpt.isPresent()) {
        Contract contract = contractOpt.get();
        contractIdentifier = contract.getIdentifier().orElseThrow();

        Property property =
            propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId).orElse(null);
        Tenant tenant =
            contractPartyService
                .findPrimaryTenantForContract(contract.getId(), teamId)
                .orElse(null);

        StringBuilder label = new StringBuilder();
        if (property != null) {
          label.append(property.getStreet());
        }
        if (tenant != null) {
          if (!label.isEmpty()) {
            label.append(" - ");
          }
          label.append(tenant.getFirstName());
          tenant.getLastName().ifPresent(n -> label.append(" ").append(n));
        }
        entityLabel = !label.isEmpty() ? label.toString() : contract.getIdentifier().orElseThrow().toString();
      }
    }

    if (feed.getPropertyId().isPresent()) {
      UUID pId = feed.getPropertyId().get();
      Optional<Property> propOpt = propertyRepository.findByIdAndTeamId(pId, teamId);
      if (propOpt.isPresent()) {
        Property property = propOpt.get();
        propertyIdentifier = property.getIdentifier().orElseThrow();
        entityLabel = property.getStreet() + ", " + property.getCity();
      }
    }

    if (feed.getTenantId().isPresent()) {
      UUID tId = feed.getTenantId().get();
      Optional<Tenant> tenantOpt = tenantRepository.findByIdAndTeamId(tId, teamId);
      if (tenantOpt.isPresent()) {
        Tenant tenant = tenantOpt.get();
        tenantIdentifier = tenant.getIdentifier().orElseThrow();
        entityLabel = tenant.getFirstName() + tenant.getLastName().map(n -> " " + n).orElse("");
      }
    }

    if (feed.getFeedType() == CalendarFeed.FeedType.ALL_PAYMENTS) {
      entityLabel = "All Payment Due Dates";
    }

    String feedUrl = appProperties.api().baseUrl() + "/calendar/ical/" + feed.getFeedToken();

    return new CalendarFeedResponse(
        feed.getIdentifier().orElseThrow(),
        feed.getFeedType(),
        Optional.ofNullable(contractIdentifier),
        Optional.ofNullable(propertyIdentifier),
        Optional.ofNullable(tenantIdentifier),
        Optional.ofNullable(entityLabel),
        feed.getEnabled(),
        feedUrl,
        feed.getCreatedAt(),
        Optional.of(feed.getUpdatedAt()));
  }

  private void verifyOwnership(CalendarFeed feed, UserPrincipal principal) {
    boolean isOwner = feed.getUserId().equals(principal.getUserId());
    boolean isAdmin = principal.getRole().map(r -> r == TEAM_ADMIN).orElse(false);
    if (!isOwner && !isAdmin) {
      throw new IllegalArgumentException("You do not have permission to modify this calendar feed");
    }
  }

  private static String escapeText(String text) {
    return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,");
  }

  private static void appendFolded(StringBuilder sb, String line) {
    // RFC 5545: lines should be no longer than 75 octets
    byte[] bytes = line.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    if (bytes.length <= 75) {
      sb.append(line).append("\r\n");
      return;
    }

    int offset = 0;
    boolean firstLine = true;
    while (offset < bytes.length) {
      int maxLen = firstLine ? 75 : 74; // continuation lines start with a space
      int end = Math.min(offset + maxLen, bytes.length);

      // Avoid splitting multibyte UTF-8 characters
      while (end > offset && end < bytes.length && (bytes[end] & 0xC0) == 0x80) {
        end--;
      }

      String segment =
          new String(bytes, offset, end - offset, java.nio.charset.StandardCharsets.UTF_8);
      if (!firstLine) {
        sb.append(" ");
      }
      sb.append(segment).append("\r\n");
      offset = end;
      firstLine = false;
    }
  }
}
