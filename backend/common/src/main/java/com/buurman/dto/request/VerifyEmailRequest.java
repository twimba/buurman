package com.buurman.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.buurman.util.Generated;

@Generated
public record VerifyEmailRequest(@NotBlank @Size(min = 6, max = 6) String code) {}
