package com.buurman.dto.response.backoffice.dashboard;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.util.SkipTestCoverage;

/** Recent log lines from the in-memory ring buffer (newest first). */
@SkipTestCoverage
public record LiveTailResponse(
    PanelStatus status,
    Optional<String> previewCta,
    Optional<String> docsLink,
    List<LogLine> lines) {

  @SkipTestCoverage
  public record LogLine(String timestamp, String level, String logger, String message) {}
}
