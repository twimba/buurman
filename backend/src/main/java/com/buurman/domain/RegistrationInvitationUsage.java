package com.buurman.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
public class RegistrationInvitationUsage {
    private UUID id;
    private UUID invitationId;
    private UUID userId;
    private String userEmail;
    private String userName;
    private Instant usedAt;
}
