package com.buurman.dto.response.backoffice.dashboard;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.util.SkipTestCoverage;

/** Quartz scheduler health: counts by state plus a per-job tile grid. */
@SkipTestCoverage
public record SchedulerHealthResponse(
    PanelStatus status,
    Optional<String> previewCta,
    Optional<String> docsLink,
    long total,
    long normal,
    long paused,
    long error,
    long blocked,
    List<JobTile> jobs) {

  @SkipTestCoverage
  public record JobTile(
      String jobName,
      String jobGroup,
      String state,
      Optional<String> nextFireTime,
      String deeplink) {}
}
