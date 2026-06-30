package com.buurman.dto.response.backoffice;

import java.util.List;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record RateLimitBucketPageResponse(
    List<RateLimitBucketResponse> content, int page, int size, long totalElements) {}
