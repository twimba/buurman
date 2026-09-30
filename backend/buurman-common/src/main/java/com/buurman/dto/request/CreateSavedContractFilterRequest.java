package com.buurman.dto.request;

import java.util.Map;

public record CreateSavedContractFilterRequest(String name, Map<String, Object> criteria) {}
