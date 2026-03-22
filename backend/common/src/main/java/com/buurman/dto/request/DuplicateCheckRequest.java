package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.Generated;

/** Lightweight request for duplicate checking — no validation since it works with partial input. */
@Generated
public record DuplicateCheckRequest(
    Optional<String> firstName,
    Optional<String> lastName,
    Optional<String> companyName,
    Optional<String> email,
    Optional<String> phone) {}
