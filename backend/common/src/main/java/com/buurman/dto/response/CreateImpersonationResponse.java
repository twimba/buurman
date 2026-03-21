package com.buurman.dto.response;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record CreateImpersonationResponse(Sid sessionIdentifier, String redirectUrl) {}
