package com.buurman.dto.response;

import java.time.LocalDate;
import java.util.List;

public record TerminationPreviewResponse(
    LocalDate computedEndDate,
    int noticeDays,
    boolean groundsRequired,
    List<String> groundsCodes,
    String source) {}
