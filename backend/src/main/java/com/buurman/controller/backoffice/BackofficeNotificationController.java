package com.buurman.controller.backoffice;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import java.time.LocalDateTime;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeNotificationResponse;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.backoffice.BackofficeNotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/backoffice/notifications")
@Tag(name = "Backoffice - Notifications", description = "Platform-wide notification management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class BackofficeNotificationController {

  private final BackofficeNotificationService backofficeNotificationService;

  @Operation(
      summary = "List notifications",
      description = "Get all notifications with filtering and pagination")
  @GetMapping
  public PageResponse<BackofficeNotificationResponse> listNotifications(
      @RequestParam(required = false) @Nullable String teamIdentifier,
      @RequestParam(required = false) @Nullable String type,
      @RequestParam(required = false) @Nullable String channel,
      @RequestParam(required = false) @Nullable String status,
      @RequestParam(required = false) @Nullable String recipientEmail,
      @RequestParam(required = false) @Nullable String dateFrom,
      @RequestParam(required = false) @Nullable String dateTo,
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "25") Integer size,
      @RequestParam(required = false) @Nullable String sort,
      @RequestParam(defaultValue = "DESC") SortDirection direction) {

    LocalDateTime from = parseDateTime(dateFrom);
    LocalDateTime to = parseDateTime(dateTo);

    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    return backofficeNotificationService.listNotifications(
        pageRequest, teamIdentifier, type, channel, status, recipientEmail, from, to);
  }

  @Operation(summary = "Get notification", description = "Get notification details by identifier")
  @GetMapping("/{identifier}")
  public BackofficeNotificationResponse getNotification(@PathVariable String identifier) {
    return backofficeNotificationService.getNotification(identifier);
  }

  @Operation(summary = "Resend notification", description = "Resend a notification")
  @PostMapping("/{identifier}/resend")
  public BackofficeNotificationResponse resendNotification(
      @PathVariable String identifier, @AuthenticationPrincipal BackofficePrincipal principal) {
    return backofficeNotificationService.resendNotification(identifier, principal);
  }

  @Operation(
      summary = "Get notification stats",
      description = "Get aggregated notification statistics")
  @GetMapping("/stats")
  public Map<String, Object> getStats() {
    return backofficeNotificationService.getStats();
  }

  private @Nullable LocalDateTime parseDateTime(@Nullable String dateTimeStr) {
    if (dateTimeStr == null || dateTimeStr.isBlank()) {
      return null;
    }
    try {
      return LocalDateTime.parse(dateTimeStr, ISO_DATE_TIME);
    } catch (Exception e) {
      try {
        return LocalDateTime.parse(dateTimeStr + "T00:00:00", ISO_DATE_TIME);
      } catch (Exception e2) {
        return null;
      }
    }
  }
}
