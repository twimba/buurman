package com.buurman.dto.response.backoffice.dashboard;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.util.SkipTestCoverage;

/** The support/safety action queue: things a Buurmy may need to act on right now. */
@SkipTestCoverage
public record ActionQueueResponse(
    PanelStatus status,
    Optional<String> previewCta,
    Optional<String> docsLink,
    List<ActionItem> items) {

  @SkipTestCoverage
  public record ActionItem(
      String key,
      String severity,
      String category,
      String title,
      String deeplink,
      long ageMinutes,
      Optional<String> snoozedUntil) {}
}
