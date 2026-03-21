package com.buurman.dto.response;

import java.time.LocalDate;
import java.util.Optional;

import com.buurman.util.Generated;

@Generated
public record DataDateRangeResponse(Optional<LocalDate> earliestDate) {}
