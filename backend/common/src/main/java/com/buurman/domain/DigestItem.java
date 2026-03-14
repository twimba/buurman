package com.buurman.domain;

import java.util.Map;

public record DigestItem(
    String itemTitle, String itemSummary, String itemLink, Map<String, Object> contentVariables) {}
