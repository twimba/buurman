package com.buurman.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.NotificationChannel;
import com.buurman.domain.RegistrationInvitation;
import com.buurman.domain.RegistrationInvitationUsage;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.backoffice.CreateRegistrationInvitationRequest;
import com.buurman.dto.request.backoffice.SendRegistrationInvitationRequest;
import com.buurman.dto.request.backoffice.UpdateRegistrationInvitationNoteRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.ValidateInvitationCodeResponse;
import com.buurman.dto.response.backoffice.RegistrationInvitationDetailResponse;
import com.buurman.dto.response.backoffice.RegistrationInvitationResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.RegistrationInvitationRepository;
import com.buurman.repository.RegistrationInvitationUsageRepository;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.notification.NotificationChannelSender;
import com.buurman.service.notification.NotificationSendRequest;
import com.buurman.service.notification.RenderedContent;
import com.buurman.util.HumanReadableIdGenerator;
import com.buurman.util.PaginationHelper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RegistrationInvitationService {

  private final RegistrationInvitationRepository invitationRepository;
  private final RegistrationInvitationUsageRepository usageRepository;
  private final Map<NotificationChannel, NotificationChannelSender> channelSenders;
  private final MetricsService metricsService;
  private final String appBaseUrl;

  public RegistrationInvitationService(
      RegistrationInvitationRepository invitationRepository,
      RegistrationInvitationUsageRepository usageRepository,
      List<NotificationChannelSender> senders,
      MetricsService metricsService,
      @Value("${app.email.base-url}") String appBaseUrl) {
    this.invitationRepository = invitationRepository;
    this.usageRepository = usageRepository;
    this.channelSenders = new java.util.HashMap<>();
    senders.forEach(s -> this.channelSenders.put(s.getChannel(), s));
    this.metricsService = metricsService;
    this.appBaseUrl = appBaseUrl;
  }

  @Transactional
  public RegistrationInvitationResponse create(
      CreateRegistrationInvitationRequest request, BackofficePrincipal principal) {
    String code =
        request
            .code()
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(c -> !c.isBlank())
            .map(
                c -> {
                  if (invitationRepository.existsByCode(c)) {
                    throw new BadRequestException("Invitation code already exists: " + c);
                  }
                  return c;
                })
            .orElseGet(
                () -> HumanReadableIdGenerator.generateUnique(invitationRepository::existsByCode));

    RegistrationInvitation invitation = new RegistrationInvitation();
    invitation.setCode(code);
    invitation.setMaxUsages(request.maxUsages());
    invitation.setExpiresAt(request.expiresAt());
    invitation.setNote(request.note());
    String principalEmail = principal.getEmail().orElse("unknown");
    invitation.setCreatedBy(principalEmail);

    invitation = invitationRepository.save(invitation);

    metricsService.incrementCounter("registration.invitation.created.total");
    log.info("Registration invitation created: code={}, by={}", code, principalEmail);

    return toResponse(invitation);
  }

  @Transactional(readOnly = true)
  public PageResponse<RegistrationInvitationResponse> list(
      PageRequest pageRequest, @Nullable String search) {
    PaginationHelper.PaginatedResult<RegistrationInvitation> result =
        invitationRepository.findAllPaginated(pageRequest, search);

    List<RegistrationInvitationResponse> items =
        result.items().stream().map(this::toResponse).toList();

    return PageResponse.of(items, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  @Transactional(readOnly = true)
  public RegistrationInvitationDetailResponse getByIdentifier(String identifier) {
    RegistrationInvitation invitation = invitationRepository.getByIdentifier(identifier);

    List<RegistrationInvitationUsage> usages =
        usageRepository.findByInvitationId(invitation.getId());

    return toDetailResponse(invitation, usages);
  }

  @Transactional
  public void revoke(String identifier, BackofficePrincipal principal) {
    RegistrationInvitation invitation = invitationRepository.getByIdentifier(identifier);

    if (invitation.getRevokedAt().isPresent()) {
      throw new BusinessRuleException("Invitation is already revoked");
    }

    String revokerEmail = principal.getEmail().orElse("unknown");
    invitationRepository.revoke(invitation.getId(), revokerEmail);
    metricsService.incrementCounter("registration.invitation.revoked.total");
    log.info("Registration invitation revoked: code={}, by={}", invitation.getCode(), revokerEmail);
  }

  public ValidateInvitationCodeResponse validateCode(String code) {
    if (code == null || code.isBlank()) {
      return new ValidateInvitationCodeResponse(false);
    }

    boolean valid =
        invitationRepository
            .findByCode(code.trim().toLowerCase())
            .map(RegistrationInvitation::isValid)
            .orElse(false);

    metricsService.incrementCounter(
        "registration.invitation.validation.total", "result", valid ? "valid" : "invalid");

    return new ValidateInvitationCodeResponse(valid);
  }

  @Transactional
  public void recordUsage(String code, UUID userId) {
    RegistrationInvitation invitation =
        invitationRepository
            .findByCode(code.trim().toLowerCase())
            .orElseThrow(() -> new BusinessRuleException("Invalid invitation code"));

    int affected = invitationRepository.incrementUsageAtomically(invitation.getId());
    if (affected == 0) {
      throw new BusinessRuleException("Invitation code is no longer valid");
    }

    usageRepository.save(invitation.getId(), userId);
    metricsService.incrementCounter("registration.invitation.used.total");
    log.info("Registration invitation used: code={}, userId={}", code, userId);
  }

  @Transactional
  public void sendInvitation(
      String identifier, SendRegistrationInvitationRequest request, BackofficePrincipal principal) {
    RegistrationInvitation invitation = invitationRepository.getByIdentifier(identifier);

    if (!invitation.isValid()) {
      throw new BusinessRuleException("Cannot send an invalid invitation");
    }

    String registerUrl = appBaseUrl + "/register?code=" + invitation.getCode();

    if ("EMAIL".equalsIgnoreCase(request.channel())) {
      sendViaEmail(invitation, request.recipient(), registerUrl, principal);
    } else if ("SMS".equalsIgnoreCase(request.channel())) {
      sendViaSms(invitation, request.recipient(), registerUrl);
    } else {
      throw new BadRequestException("Invalid channel: " + request.channel());
    }

    log.info(
        "Registration invitation sent: code={}, channel={}, to={}, by={}",
        invitation.getCode(),
        request.channel(),
        request.recipient(),
        principal.getEmail().orElse("unknown"));
  }

  @Transactional
  public RegistrationInvitationDetailResponse updateNote(
      String identifier,
      UpdateRegistrationInvitationNoteRequest request,
      BackofficePrincipal principal) {
    RegistrationInvitation invitation = invitationRepository.getByIdentifier(identifier);
    String note = (request.note() != null && !request.note().isBlank()) ? request.note() : null;
    invitationRepository.updateNote(invitation.getId(), note);

    log.info(
        "Registration invitation note updated: code={}, by={}",
        invitation.getCode(),
        principal.getEmail().orElse("unknown"));

    return getByIdentifier(identifier);
  }

  public String suggestCode() {
    return HumanReadableIdGenerator.generateUnique(invitationRepository::existsByCode);
  }

  private void sendViaEmail(
      RegistrationInvitation invitation,
      String recipientEmail,
      String registerUrl,
      BackofficePrincipal principal) {
    @Nullable NotificationChannelSender emailSender = channelSenders.get(NotificationChannel.EMAIL);
    if (emailSender == null) {
      throw new BusinessRuleException("Email sending is not configured");
    }

    Map<String, Object> variables =
        Map.of(
            "invitationCode", invitation.getCode(),
            "registerUrl", registerUrl,
            "senderName", principal.getName());

    RenderedContent rendered = emailSender.render("registration-invitation", variables);
    try {
      emailSender.send(
          new NotificationSendRequest(
              Optional.empty(),
              Optional.of(recipientEmail),
              Optional.empty(),
              rendered.subject(),
              rendered.body(),
              Optional.empty(),
              Optional.empty()));
    } catch (com.buurman.service.notification.NotificationSendException e) {
      throw new BusinessRuleException("Failed to send invitation email: " + e.getMessage());
    }
  }

  private void sendViaSms(
      RegistrationInvitation invitation, String recipientPhone, String registerUrl) {
    @Nullable NotificationChannelSender smsSender = channelSenders.get(NotificationChannel.SMS);
    if (smsSender == null) {
      throw new BusinessRuleException("SMS sending is not configured");
    }

    String body =
        "You've been invited to join Buurman! Use code: "
            + invitation.getCode()
            + " or register at: "
            + registerUrl;

    try {
      smsSender.send(
          new NotificationSendRequest(
              Optional.empty(),
              Optional.empty(),
              Optional.of(recipientPhone),
              Optional.of("Buurman Invitation"),
              body,
              Optional.empty(),
              Optional.empty()));
    } catch (com.buurman.service.notification.NotificationSendException e) {
      throw new BusinessRuleException("Failed to send invitation SMS: " + e.getMessage());
    }
  }

  private RegistrationInvitationResponse toResponse(RegistrationInvitation inv) {
    return new RegistrationInvitationResponse(
        inv.getIdentifier(),
        inv.getCode(),
        inv.getMaxUsages(),
        inv.getUsageCount(),
        inv.getExpiresAt(),
        inv.getRevokedAt().isPresent(),
        inv.getStatus(),
        inv.getCreatedBy(),
        inv.getCreatedAt(),
        inv.getNote().filter(n -> !n.isBlank()).isPresent());
  }

  private RegistrationInvitationDetailResponse toDetailResponse(
      RegistrationInvitation inv, List<RegistrationInvitationUsage> usages) {

    List<RegistrationInvitationDetailResponse.UsageRecord> usageRecords =
        usages.stream()
            .map(
                u ->
                    new RegistrationInvitationDetailResponse.UsageRecord(
                        u.getUserEmail(), Optional.ofNullable(u.getUserName()), u.getUsedAt()))
            .toList();

    return new RegistrationInvitationDetailResponse(
        inv.getIdentifier(),
        inv.getCode(),
        inv.getMaxUsages(),
        inv.getUsageCount(),
        inv.getExpiresAt(),
        inv.getRevokedAt().isPresent(),
        inv.getRevokedBy(),
        inv.getRevokedAt(),
        inv.getStatus(),
        inv.getCreatedBy(),
        inv.getCreatedAt(),
        Optional.of(inv.getUpdatedAt()),
        inv.getNote(),
        usageRecords);
  }
}
