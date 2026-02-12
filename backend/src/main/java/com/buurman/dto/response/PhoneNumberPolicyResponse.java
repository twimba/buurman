package com.buurman.dto.response;

import java.util.List;
import java.util.Map;

public record PhoneNumberPolicyResponse(
    Map<String, List<String>> policyMatrix
) {}
