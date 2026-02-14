package com.buurman.controller;

import com.buurman.dto.request.CreateCalendarFeedRequest;
import com.buurman.dto.response.CalendarFeedResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.CalendarFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/calendar")
@Tag(name = "Calendar Feeds", description = "iCalendar feed management and serving")
public class CalendarFeedController {

    private final CalendarFeedService calendarFeedService;

    public CalendarFeedController(CalendarFeedService calendarFeedService) {
        this.calendarFeedService = calendarFeedService;
    }

    @Operation(summary = "Get iCalendar feed", description = "Public endpoint serving iCal feed content")
    @GetMapping(value = "/ical/{feedToken}", produces = "text/calendar; charset=utf-8")
    public ResponseEntity<String> getCalendarFeed(@PathVariable String feedToken) {
        String ical = calendarFeedService.generateICalFeed(feedToken);
        if (ical == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"buurman-payments.ics\"")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .body(ical);
    }

    @Operation(summary = "List calendar feeds", description = "Get all calendar feeds for the current user")
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/feeds")
    public List<CalendarFeedResponse> getUserFeeds(@AuthenticationPrincipal UserPrincipal principal) {
        return calendarFeedService.getUserFeeds(principal);
    }

    @Operation(summary = "Create calendar feed", description = "Create a new iCalendar feed subscription URL")
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping("/feeds")
    @ResponseStatus(CREATED)
    public CalendarFeedResponse createFeed(
            @Valid @RequestBody CreateCalendarFeedRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return calendarFeedService.createFeed(request, principal);
    }

    @Operation(summary = "Rotate feed token", description = "Generate a new URL token, invalidating the previous one")
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping("/feeds/{identifier}/rotate")
    public CalendarFeedResponse rotateFeedToken(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return calendarFeedService.rotateFeedToken(identifier, principal);
    }

    @Operation(summary = "Delete calendar feed", description = "Soft delete a calendar feed")
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/feeds/{identifier}")
    @ResponseStatus(NO_CONTENT)
    public void deleteFeed(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        calendarFeedService.deleteFeed(identifier, principal);
    }
}
