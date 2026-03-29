package com.buurman.dto.response;

import java.util.List;
import java.util.Map;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record PhoneNumberPolicyResponse(Map<String, List<String>> policyMatrix) {}
