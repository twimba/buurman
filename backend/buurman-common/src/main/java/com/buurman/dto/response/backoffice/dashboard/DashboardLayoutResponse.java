package com.buurman.dto.response.backoffice.dashboard;

import java.util.List;

import com.buurman.util.SkipTestCoverage;

/** A user's persisted bento layout. Empty list means "use defaults". */
@SkipTestCoverage
public record DashboardLayoutResponse(List<PanelPlacement> panels) {}
