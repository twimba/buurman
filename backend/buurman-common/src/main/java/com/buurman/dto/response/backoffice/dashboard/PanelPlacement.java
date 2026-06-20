package com.buurman.dto.response.backoffice.dashboard;

import com.buurman.util.SkipTestCoverage;

/** Position/size of a single panel in the bento grid. Shared by layout request + response. */
@SkipTestCoverage
public record PanelPlacement(String panel, int x, int y, int w, int h, boolean hidden) {}
