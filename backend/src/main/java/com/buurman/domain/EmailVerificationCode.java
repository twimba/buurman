package com.buurman.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
public class EmailVerificationCode {

    private UUID id;
    private UUID userId;
    private String code;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant createdAt;
}
