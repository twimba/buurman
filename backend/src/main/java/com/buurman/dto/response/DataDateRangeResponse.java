package com.buurman.dto.response;

import java.time.LocalDate;
import java.util.Optional;

public record DataDateRangeResponse(Optional<LocalDate> earliestDate) {}
