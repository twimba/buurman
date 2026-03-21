package com.buurman.dto.response;

import java.math.BigDecimal;
import com.buurman.util.Generated;

@Generated
public record WwsCategoryBreakdown(
    String key, String name, String nameNl, BigDecimal points, String explanation) {}
