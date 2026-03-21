package com.buurman.dto.request;

import com.buurman.util.Generated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Generated
public record VerifyEmailRequest(@NotBlank @Size(min = 6, max = 6) String code) {}
