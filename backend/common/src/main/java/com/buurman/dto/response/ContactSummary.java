package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.ContactType;
import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record ContactSummary(
    Sid identifier,
    ContactType contactType,
    String displayName,
    Optional<String> firstName,
    Optional<String> lastName,
    Optional<String> email,
    Optional<String> phone) {}
