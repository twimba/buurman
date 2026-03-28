package com.buurman.dto.response;

import java.time.LocalDate;
import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record DataDateRangeResponse(Optional<LocalDate> earliestDate) {}
