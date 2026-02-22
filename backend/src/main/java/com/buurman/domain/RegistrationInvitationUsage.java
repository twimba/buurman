package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
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
