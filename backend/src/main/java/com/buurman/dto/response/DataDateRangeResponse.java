package com.buurman.dto.response;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

public record DataDateRangeResponse(@Nullable LocalDate earliestDate) {}
