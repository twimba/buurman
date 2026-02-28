package com.buurman.controller.backoffice;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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
import io.swagger.v3.oas.annotations.Parameter;
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
      @Parameter(description = "Filter by team ULID identifier") @RequestParam
          Optional<String> teamIdentifier,
      @Parameter(description = "Filter by notification type") @RequestParam Optional<String> type,
      @Parameter(description = "Filter by delivery channel") @RequestParam Optional<String> channel,
      @Parameter(description = "Filter by status") @RequestParam Optional<String> status,
      @Parameter(description = "Filter by recipient email") @RequestParam
          Optional<String> recipientEmail,
      @Parameter(description = "Filter from date (ISO-8601)", example = "2026-01-01T00:00:00")
          @RequestParam
          Optional<String> dateFrom,
      @Parameter(description = "Filter to date (ISO-8601)", example = "2026-12-31T23:59:59")
          @RequestParam
          Optional<String> dateTo,
      @Parameter(description = "Page number (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          Integer page,
      @Parameter(description = "Page size", example = "25") @RequestParam(defaultValue = "25")
          Integer size,
      @Parameter(description = "Sort field name", example = "createdAt") @RequestParam
          Optional<String> sort,
      @Parameter(description = "Sort direction", example = "DESC")
          @RequestParam(defaultValue = "DESC")
          SortDirection direction) {

    LocalDateTime from = parseDateTime(dateFrom.orElse(null));
    LocalDateTime to = parseDateTime(dateTo.orElse(null));

    PageRequest pageRequest = PageRequest.of(page, size, sort.orElse(null), direction);
    return backofficeNotificationService.listNotifications(
        pageRequest,
        teamIdentifier.orElse(null),
        type.orElse(null),
        channel.orElse(null),
        status.orElse(null),
        recipientEmail.orElse(null),
        from,
        to);
  }

  @Operation(summary = "Get notification", description = "Get notification details by identifier")
  @GetMapping("/{identifier}")
  public BackofficeNotificationResponse getNotification(
      @Parameter(description = "Notification ULID identifier") @PathVariable String identifier) {
    return backofficeNotificationService.getNotification(identifier);
  }

  @Operation(summary = "Resend notification", description = "Resend a notification")
  @PostMapping("/{identifier}/resend")
  public BackofficeNotificationResponse resendNotification(
      @Parameter(description = "Notification ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal BackofficePrincipal principal) {
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
