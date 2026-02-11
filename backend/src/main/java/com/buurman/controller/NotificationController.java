package com.buurman.controller;

import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.NotificationResponse;
import com.buurman.dto.response.NotificationStatsResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationCenterService;
import com.buurman.util.PaginationHelper.PaginatedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Notification center and management")
@SecurityRequirement(name = "bearer-jwt")
public class NotificationController {

    private final NotificationCenterService centerService;

    public NotificationController(NotificationCenterService centerService) {
        this.centerService = centerService;
    }

    @Operation(summary = "List notifications", description = "Paginated list of all team notifications (Admin only)")
    @GetMapping
    public PageResponse<NotificationResponse> getNotifications(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String recipientEmail,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal principal) {

        PageRequest pageRequest = new PageRequest(page, size, sort, direction);
        PaginatedResult<NotificationResponse> result = centerService.getNotifications(
                principal, type, channel, status, recipientEmail, dateFrom, dateTo, pageRequest);

        return PageResponse.of(result.items(), page, size, result.totalElements());
    }

    @Operation(summary = "Get notification details")
    @GetMapping("/{identifier}")
    public NotificationResponse getNotification(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return centerService.getNotification(principal, identifier);
    }

    @Operation(summary = "Get notification statistics")
    @GetMapping("/stats")
    public NotificationStatsResponse getStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        return centerService.getStats(principal);
    }

    @Operation(summary = "Resend notification", description = "Resend a failed notification (Admin only)")
    @PostMapping("/{identifier}/resend")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse resendNotification(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return centerService.resendNotification(principal, identifier);
    }

    @Operation(summary = "Refresh notification status", description = "Fetch latest delivery status from provider (Admin only)")
    @PostMapping("/{identifier}/refresh-status")
    public NotificationResponse refreshStatus(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return centerService.refreshNotificationStatus(principal, identifier);
    }
}
