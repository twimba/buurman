package com.buurman.dto.response;

import java.util.Map;

public record ImportRevertResponse(
    int deletedContactCount,
    Map<String, Integer> deletedRelatedEntities) {}
