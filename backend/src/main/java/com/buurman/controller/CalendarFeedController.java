package com.buurman.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.CreateCalendarFeedRequest;
import com.buurman.dto.response.CalendarFeedResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.CalendarFeedService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/calendar")
@Tag(name = "Calendar Feeds", description = "iCalendar feed management and serving")
@RequiredArgsConstructor
public class CalendarFeedController {

  private final CalendarFeedService calendarFeedService;

  @Operation(
      summary = "Get iCalendar feed",
      description = "Public endpoint serving iCal feed content")
  @GetMapping(value = "/ical/{feedToken}", produces = "text/calendar; charset=utf-8")
  public ResponseEntity<String> getCalendarFeed(@PathVariable String feedToken) {
    return calendarFeedService
        .generateICalFeed(feedToken)
        .map(
            ical ->
                ResponseEntity.ok()
                    .header("Content-Disposition", "inline; filename=\"buurman-payments.ics\"")
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .body(ical))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "List calendar feeds",
      description = "Get all calendar feeds for the current user")
  @SecurityRequirement(name = "bearer-jwt")
  @GetMapping("/feeds")
  public List<CalendarFeedResponse> getUserFeeds(@AuthenticationPrincipal UserPrincipal principal) {
    return calendarFeedService.getUserFeeds(principal);
  }

  @Operation(
      summary = "Create calendar feed",
      description = "Create a new iCalendar feed subscription URL")
  @SecurityRequirement(name = "bearer-jwt")
  @PostMapping("/feeds")
  @ResponseStatus(CREATED)
  public CalendarFeedResponse createFeed(
      @Valid @RequestBody CreateCalendarFeedRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return calendarFeedService.createFeed(request, principal);
  }

  @Operation(
      summary = "Rotate feed token",
      description = "Generate a new URL token, invalidating the previous one")
  @SecurityRequirement(name = "bearer-jwt")
  @PostMapping("/feeds/{identifier}/rotate")
  public CalendarFeedResponse rotateFeedToken(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return calendarFeedService.rotateFeedToken(identifier, principal);
  }

  @Operation(summary = "Delete calendar feed", description = "Soft delete a calendar feed")
  @SecurityRequirement(name = "bearer-jwt")
  @DeleteMapping("/feeds/{identifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteFeed(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    calendarFeedService.deleteFeed(identifier, principal);
  }
}
