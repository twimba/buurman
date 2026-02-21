package com.buurman.service;

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

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.config.models.AppProperties;
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
        calendarFeedRepository.findByUserIdAndTeamId(principal.getUserId(), principal.getTeamId());
    return feeds.stream().map(f -> toResponse(f, principal.getTeamId())).toList();
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public CalendarFeedResponse createFeed(
      CreateCalendarFeedRequest request, UserPrincipal principal) {
    UUID contractId = null;
    UUID propertyId = null;
    UUID tenantId = null;

    switch (request.feedType()) {
      case CONTRACT -> {
        if (request.contractIdentifier() == null || request.contractIdentifier().isBlank()) {
          throw new IllegalArgumentException(
              "contractIdentifier is required for CONTRACT feed type");
        }
        Contract contract =
            contractRepository.getByIdentifierAndTeamId(
                request.contractIdentifier(), principal.getTeamId());
        contractId = contract.getId();
      }
      case PROPERTY_PAYMENTS -> {
        if (request.propertyIdentifier() == null || request.propertyIdentifier().isBlank()) {
          throw new IllegalArgumentException(
              "propertyIdentifier is required for PROPERTY_PAYMENTS feed type");
        }
        Property property =
            propertyRepository.getByIdentifierAndTeamId(
                request.propertyIdentifier(), principal.getTeamId());
        propertyId = property.getId();
      }
      case TENANT_PAYMENTS -> {
        if (request.tenantIdentifier() == null || request.tenantIdentifier().isBlank()) {
          throw new IllegalArgumentException(
              "tenantIdentifier is required for TENANT_PAYMENTS feed type");
        }
        Tenant tenant =
            tenantRepository.getByIdentifierAndTeamId(
                request.tenantIdentifier(), principal.getTeamId());
        tenantId = tenant.getId();
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
            principal.getTeamId());
    if (existing.isPresent()) {
      return toResponse(existing.get(), principal.getTeamId());
    }

    CalendarFeed feed = new CalendarFeed();
    feed.setIdentifier(newCalendarFeedId().value());
    feed.setTeamId(principal.getTeamId());
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
    return toResponse(saved, principal.getTeamId());
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public CalendarFeedResponse rotateFeedToken(String identifier, UserPrincipal principal) {
    CalendarFeed feed =
        calendarFeedRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    verifyOwnership(feed, principal);

    feed.setFeedToken(newToken().value() + newToken().value());
    feed.setUpdatedBy(principal.getUserId());
    feed.setUpdatedAt(null); // let repository set current time

    CalendarFeed saved = calendarFeedRepository.save(feed);
    return toResponse(saved, principal.getTeamId());
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void deleteFeed(String identifier, UserPrincipal principal) {
    CalendarFeed feed =
        calendarFeedRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    verifyOwnership(feed, principal);

    calendarFeedRepository.softDeleteByIdAndTeamId(feed.getId(), feed.getTeamId());
  }

  public String generateICalFeed(String feedToken) {
    Optional<CalendarFeed> feedOpt = calendarFeedRepository.findByFeedToken(feedToken);
    if (feedOpt.isEmpty()) {
      return null;
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
      case CONTRACT -> {
        if (feed.getContractId() != null) {
          contractIds.add(feed.getContractId());
          contractRepository
              .findByIdAndTeamId(feed.getContractId(), teamId)
              .ifPresent(milestoneContracts::add);
        }
      }
      case PROPERTY_PAYMENTS -> {
        if (feed.getPropertyId() != null) {
          List<Contract> propContracts =
              contractRepository.findByPropertyId(feed.getPropertyId(), teamId);
          milestoneContracts.addAll(propContracts);
          propContracts.forEach(c -> contractIds.add(c.getId()));
        }
      }
      case TENANT_PAYMENTS -> {
        if (feed.getTenantId() != null) {
          List<Contract> tenContracts =
              contractRepository.findByTenantIdViaParties(feed.getTenantId(), teamId);
          milestoneContracts.addAll(tenContracts);
          tenContracts.forEach(c -> contractIds.add(c.getId()));
        }
      }
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
      String propertyLabel =
          property != null && property.getStreet() != null ? property.getStreet() : "Contract";

      if (contract.getStartDate() != null) {
        sb.append("BEGIN:VEVENT\r\n");
        appendFolded(sb, "UID:contract-start-" + contract.getIdentifier() + "@buurman.app");
        sb.append("DTSTART;VALUE=DATE:")
            .append(contract.getStartDate().format(DATE_FORMAT))
            .append("\r\n");
        appendFolded(sb, "SUMMARY:" + escapeText("Contract Start - " + propertyLabel));
        appendFolded(
            sb, "DESCRIPTION:" + escapeText("Contract #" + contract.getIdentifier() + " starts"));
        sb.append("STATUS:CONFIRMED\r\n");
        sb.append("TRANSP:TRANSPARENT\r\n");
        sb.append("END:VEVENT\r\n");
      }

      if (contract.getEndDate() != null) {
        sb.append("BEGIN:VEVENT\r\n");
        appendFolded(sb, "UID:contract-end-" + contract.getIdentifier() + "@buurman.app");
        sb.append("DTSTART;VALUE=DATE:")
            .append(contract.getEndDate().format(DATE_FORMAT))
            .append("\r\n");
        appendFolded(sb, "SUMMARY:" + escapeText("Contract End - " + propertyLabel));
        appendFolded(
            sb, "DESCRIPTION:" + escapeText("Contract #" + contract.getIdentifier() + " ends"));
        sb.append("STATUS:CONFIRMED\r\n");
        sb.append("TRANSP:TRANSPARENT\r\n");
        sb.append("END:VEVENT\r\n");
      }

      if (contract.getSignedDate() != null) {
        sb.append("BEGIN:VEVENT\r\n");
        appendFolded(sb, "UID:contract-signed-" + contract.getIdentifier() + "@buurman.app");
        sb.append("DTSTART;VALUE=DATE:")
            .append(contract.getSignedDate().format(DATE_FORMAT))
            .append("\r\n");
        appendFolded(sb, "SUMMARY:" + escapeText("Contract Signed - " + propertyLabel));
        appendFolded(
            sb, "DESCRIPTION:" + escapeText("Contract #" + contract.getIdentifier() + " signed"));
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
      appendFolded(sb, "UID:" + payment.getIdentifier() + "@buurman.app");
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
    return sb.toString();
  }

  private List<Payment> loadPayments(CalendarFeed feed, UUID teamId) {
    return switch (feed.getFeedType()) {
      case ALL_PAYMENTS -> paymentRepository.findAllByTeamId(teamId);
      case CONTRACT -> paymentRepository.findByContractId(feed.getContractId(), teamId);
      case PROPERTY_PAYMENTS -> {
        List<Contract> contracts =
            contractRepository.findByPropertyId(feed.getPropertyId(), teamId);
        List<Payment> result = new ArrayList<>();
        for (Contract c : contracts) {
          result.addAll(paymentRepository.findByContractId(c.getId(), teamId));
        }
        yield result;
      }
      case TENANT_PAYMENTS -> {
        List<Contract> contracts =
            contractRepository.findByTenantIdViaParties(feed.getTenantId(), teamId);
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
        Property p =
            feed.getPropertyId() != null
                ? propertyMap.values().stream()
                    .filter(prop -> prop.getId().equals(feed.getPropertyId()))
                    .findFirst()
                    .orElse(null)
                : null;
        if (p == null) {
          p = propertyRepository.findByIdAndTeamId(feed.getPropertyId(), teamId).orElse(null);
        }
        String name = p != null && p.getStreet() != null ? p.getStreet() : "Property";
        yield "Buurman - " + name + " Payments";
      }
      case TENANT_PAYMENTS -> {
        Tenant t =
            feed.getTenantId() != null
                ? tenantMap.values().stream()
                    .filter(tn -> tn.getId().equals(feed.getTenantId()))
                    .findFirst()
                    .orElse(null)
                : null;
        if (t == null) {
          t = tenantRepository.findByIdAndTeamId(feed.getTenantId(), teamId).orElse(null);
        }
        String name = t != null ? t.getFirstName() + " " + t.getLastName() : "Tenant";
        yield "Buurman - " + name + " Payments";
      }
    };
  }

  private String buildSummary(Property property) {
    if (property != null && property.getStreet() != null) {
      String summary = "Rent Due - " + property.getStreet();
      if (property.getCity() != null) {
        summary += ", " + property.getCity();
      }
      return summary;
    }
    return "Rent Payment Due";
  }

  private String buildDescription(
      Payment payment, Contract contract, Property property, Tenant tenant) {
    StringBuilder desc = new StringBuilder();
    if (payment.getAmount() != null) {
      String currency = payment.getCurrency();
      desc.append("Amount: ")
          .append(currency)
          .append(" ")
          .append(payment.getAmount().toPlainString());
    }
    if (tenant != null) {
      desc.append("\\nTenant: ")
          .append(tenant.getFirstName())
          .append(" ")
          .append(tenant.getLastName());
    }
    if (property != null && property.getStreet() != null) {
      desc.append("\\nProperty: ").append(property.getStreet());
    }
    if (contract != null) {
      desc.append("\\nContract: #").append(contract.getIdentifier());
    }
    desc.append("\\nStatus: ").append(payment.getStatus().name());
    return desc.toString();
  }

  private CalendarFeedResponse toResponse(CalendarFeed feed, UUID teamId) {
    String contractIdentifier = null;
    String propertyIdentifier = null;
    String tenantIdentifier = null;
    String entityLabel = null;

    if (feed.getContractId() != null) {
      Optional<Contract> contractOpt =
          contractRepository.findByIdAndTeamId(feed.getContractId(), teamId);
      if (contractOpt.isPresent()) {
        Contract contract = contractOpt.get();
        contractIdentifier = contract.getIdentifier();

        Property property =
            propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId).orElse(null);
        Tenant tenant =
            contractPartyService
                .findPrimaryTenantForContract(contract.getId(), teamId)
                .orElse(null);

        StringBuilder label = new StringBuilder();
        if (property != null && property.getStreet() != null) {
          label.append(property.getStreet());
        }
        if (tenant != null) {
          if (label.length() > 0) {
            label.append(" - ");
          }
          label.append(tenant.getFirstName()).append(" ").append(tenant.getLastName());
        }
        entityLabel = label.length() > 0 ? label.toString() : contract.getIdentifier();
      }
    }

    if (feed.getPropertyId() != null) {
      Optional<Property> propOpt =
          propertyRepository.findByIdAndTeamId(feed.getPropertyId(), teamId);
      if (propOpt.isPresent()) {
        Property property = propOpt.get();
        propertyIdentifier = property.getIdentifier();
        entityLabel =
            property.getStreet() != null ? property.getStreet() : property.getIdentifier();
        if (property.getCity() != null) {
          entityLabel += ", " + property.getCity();
        }
      }
    }

    if (feed.getTenantId() != null) {
      Optional<Tenant> tenantOpt = tenantRepository.findByIdAndTeamId(feed.getTenantId(), teamId);
      if (tenantOpt.isPresent()) {
        Tenant tenant = tenantOpt.get();
        tenantIdentifier = tenant.getIdentifier();
        entityLabel = tenant.getFirstName() + " " + tenant.getLastName();
      }
    }

    if (feed.getFeedType() == CalendarFeed.FeedType.ALL_PAYMENTS) {
      entityLabel = "All Payment Due Dates";
    }

    String feedUrl = appProperties.api().baseUrl() + "/calendar/ical/" + feed.getFeedToken();

    return new CalendarFeedResponse(
        feed.getIdentifier(),
        feed.getFeedType(),
        contractIdentifier,
        propertyIdentifier,
        tenantIdentifier,
        entityLabel,
        feed.getEnabled(),
        feedUrl,
        feed.getCreatedAt(),
        feed.getUpdatedAt());
  }

  private void verifyOwnership(CalendarFeed feed, UserPrincipal principal) {
    boolean isOwner = feed.getUserId().equals(principal.getUserId());
    boolean isAdmin = "TEAM_ADMIN".equals(principal.getRole());
    if (!isOwner && !isAdmin) {
      throw new IllegalArgumentException("You do not have permission to modify this calendar feed");
    }
  }

  private static String escapeText(String text) {
    if (text == null) {
      return "";
    }
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
