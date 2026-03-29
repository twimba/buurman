package com.buurman.dto.response;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record CreateImpersonationResponse(Sid sessionIdentifier, String redirectUrl) {}
