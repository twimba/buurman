package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record ContractPartyResponse(
    Sid identifier, Optional<ContactSummary> contact, ContractPartyRole role) {}
