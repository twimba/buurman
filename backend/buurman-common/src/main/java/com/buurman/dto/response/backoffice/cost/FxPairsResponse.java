package com.buurman.dto.response.backoffice.cost;

import java.util.List;

/** All tracked currency pairs, anchored on EUR. */
public record FxPairsResponse(List<FxPair> pairs) {}
