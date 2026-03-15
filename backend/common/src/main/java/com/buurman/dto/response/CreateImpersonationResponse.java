package com.buurman.dto.response;

import com.buurman.domain.Sid;

public record CreateImpersonationResponse(Sid sessionIdentifier, String redirectUrl) {}
