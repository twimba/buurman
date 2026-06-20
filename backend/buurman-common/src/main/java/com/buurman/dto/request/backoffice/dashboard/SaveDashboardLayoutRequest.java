package com.buurman.dto.request.backoffice.dashboard;

import java.util.List;

import com.buurman.dto.response.backoffice.dashboard.PanelPlacement;
import com.buurman.util.SkipTestCoverage;

/** Persist a user's bento layout. */
@SkipTestCoverage
public record SaveDashboardLayoutRequest(List<PanelPlacement> panels) {}
