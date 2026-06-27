package com.buurman.dto.response.backoffice.dashboard;

import java.util.Optional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.util.SkipTestCoverage;

/**
 * Shared envelope for panels whose data source is not wired yet (business, cost-watch,
 * latency-heatmap, geo, live-tail). Carries no data — the frontend renders a generic preview
 * placeholder from {@code previewCta} / {@code docsLink}.
 */
@SkipTestCoverage
public record PreviewPanelResponse(
    PanelStatus status, String panel, Optional<String> previewCta, Optional<String> docsLink) {}
