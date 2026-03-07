package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.CalendarFeedIdentifier;
import com.buurman.dto.request.CreateCalendarFeedRequest;
import com.buurman.dto.response.CalendarFeedResponse;
import com.buurman.exception.NotFoundException;
import com.buurman.generated.api.CalendarFeedsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.CalendarFeedService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CalendarFeedController implements CalendarFeedsApi {

  private final CalendarFeedService calendarFeedService;
  private final HttpServletResponse httpServletResponse;

  @Override
  public String getCalendarFeed(String feedToken) {
    return calendarFeedService
        .generateICalFeed(feedToken)
        .map(
            ical -> {
              httpServletResponse.setHeader(
                  "Content-Disposition", "inline; filename=\"buurman-payments.ics\"");
              httpServletResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
              return ical;
            })
        .orElseThrow(() -> new NotFoundException("Calendar feed not found"));
  }

  @Override
  public List<CalendarFeedResponse> getUserFeeds() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return calendarFeedService.getUserFeeds(principal);
  }

  @Override
  public CalendarFeedResponse createFeed(CreateCalendarFeedRequest createCalendarFeedRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return calendarFeedService.createFeed(createCalendarFeedRequest, principal);
  }

  @Override
  public CalendarFeedResponse rotateFeedToken(CalendarFeedIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return calendarFeedService.rotateFeedToken(identifier, principal);
  }

  @Override
  public void deleteFeed(CalendarFeedIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    calendarFeedService.deleteFeed(identifier, principal);
  }
}
