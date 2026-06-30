package com.buurman.dto.response.backoffice;

import java.util.List;
import java.util.Map;

import com.buurman.util.SkipTestCoverage;

/**
 * One inspected cache entry. {@code details} is intentionally an untyped map list: its shape varies
 * per cache (flag-definition rows, override rows, segment rows), so it's surfaced as-is for the
 * backoffice inspector rather than modelled per cache type.
 */
@SkipTestCoverage
public record CacheEntryResponse(String key, String summary, List<Map<String, Object>> details) {}
