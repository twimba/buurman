package com.buurman.security;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.ImpersonationMode;
import com.buurman.domain.Sid;
import com.buurman.domain.TeamRole;

import lombok.Getter;

@Getter
public class ImpersonationPrincipal extends UserPrincipal {

  private final Sid impersonationSessionId;
  private final UUID impersonationSessionUuid;
  private final String impersonatedByEmail;
  private final String impersonatedByName;
  private final ImpersonationMode mode;

  @SuppressWarnings("ParameterNumber")
  public ImpersonationPrincipal(
      UUID userId,
      String userIdentifier,
      String keycloakId,
      String email,
      String name,
      @Nullable UUID teamId,
      @Nullable String teamIdentifier,
      @Nullable TeamRole role,
      boolean isOwner,
      boolean emailVerified,
      Sid impersonationSessionId,
      UUID impersonationSessionUuid,
      String impersonatedByEmail,
      String impersonatedByName,
      ImpersonationMode mode) {
    super(
        userId,
        userIdentifier,
        keycloakId,
        email,
        name,
        teamId,
        teamIdentifier,
        role,
        isOwner,
        emailVerified);
    this.impersonationSessionId = impersonationSessionId;
    this.impersonationSessionUuid = impersonationSessionUuid;
    this.impersonatedByEmail = impersonatedByEmail;
    this.impersonatedByName = impersonatedByName;
    this.mode = mode;
  }

  public boolean isReadOnly() {
    return mode == ImpersonationMode.READ_ONLY;
  }

  public String getAuditDisplayName() {
    return impersonatedByName + " on behalf of " + getName();
  }
}
