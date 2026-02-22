package com.buurman.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.buurman.security.UserPrincipal;
import com.flagsmith.FlagsmithClient;
import com.flagsmith.models.Flags;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FeatureFlagService {

  private final @Nullable FlagsmithClient flagsmithClient;

  public FeatureFlagService(Optional<FlagsmithClient> flagsmithClient) {
    this.flagsmithClient = flagsmithClient.orElse(null);
    if (this.flagsmithClient == null) {
      log.warn("Flagsmith client not configured — all feature flags will default to OFF");
    }
  }

  /** Global flag evaluation (no identity context). */
  public boolean isEnabled(String flagKey) {
    if (flagsmithClient == null) {
      return false;
    }
    try {
      Flags flags = flagsmithClient.getEnvironmentFlags();
      return flags.isFeatureEnabled(flagKey);
    } catch (Exception e) {
      log.warn("Failed to evaluate flag '{}', defaulting to false", flagKey, e);
      return false;
    }
  }

  public boolean isDisabled(String flagKey) {
    return !isEnabled(flagKey);
  }

  /** Identity-aware flag evaluation with user/team traits. */
  public boolean isEnabled(String flagKey, UserPrincipal principal) {
    if (flagsmithClient == null) {
      return false;
    }
    try {
      String identity = buildIdentity(principal);
      Map<String, Object> traits = buildTraits(principal);

      Flags flags = flagsmithClient.getIdentityFlags(identity, traits);

      return flags.isFeatureEnabled(flagKey);
    } catch (Exception e) {
      log.warn("Failed to evaluate flag '{}' for identity, defaulting to false", flagKey, e);
      return false;
    }
  }

  public boolean isDisabled(String flagKey, UserPrincipal principal) {
    return !isEnabled(flagKey, principal);
  }

  /** Get remote config value for a flag (global). */
  public @Nullable Object getValue(String flagKey) {
    if (flagsmithClient == null) {
      return null;
    }
    try {
      Flags flags = flagsmithClient.getEnvironmentFlags();
      return flags.getFeatureValue(flagKey);
    } catch (Exception e) {
      log.warn("Failed to get value for flag '{}', returning null", flagKey, e);
      return null;
    }
  }

  /** Get remote config value for a flag (identity-aware). */
  public @Nullable Object getValue(String flagKey, UserPrincipal principal) {
    if (flagsmithClient == null) {
      return null;
    }
    try {
      Flags flags =
          flagsmithClient.getIdentityFlags(buildIdentity(principal), buildTraits(principal));
      return flags.getFeatureValue(flagKey);
    } catch (Exception e) {
      log.warn("Failed to get value for flag '{}' for identity, returning null", flagKey, e);
      return null;
    }
  }

  /** Get all evaluated flags for the current user (for the frontend endpoint). */
  public Map<String, Object> getAllFlags(UserPrincipal principal) {
    Map<String, Object> result = new HashMap<>();
    if (flagsmithClient == null) {
      return result;
    }
    try {
      Flags flags =
          flagsmithClient.getIdentityFlags(buildIdentity(principal), buildTraits(principal));
      flags
          .getAllFlags()
          .forEach(
              flag -> {
                Map<String, Object> flagData = new HashMap<>();
                flagData.put("enabled", flag.getEnabled());
                flagData.put("value", flag.getValue());
                result.put(flag.getFeatureName(), flagData);
              });
    } catch (Exception e) {
      log.warn("Failed to get all flags for identity, returning empty map", e);
    }
    return result;
  }

  /** Get all environment-level flags (no identity context). */
  public Map<String, Object> getAllEnvironmentFlags() {
    Map<String, Object> result = new HashMap<>();
    if (flagsmithClient == null) {
      return result;
    }
    try {
      Flags flags = flagsmithClient.getEnvironmentFlags();
      flags
          .getAllFlags()
          .forEach(
              flag -> {
                Map<String, Object> flagData = new HashMap<>();
                flagData.put("enabled", flag.getEnabled());
                flagData.put("value", flag.getValue());
                result.put(flag.getFeatureName(), flagData);
              });
    } catch (Exception e) {
      log.warn("Failed to get environment flags, returning empty map", e);
    }
    return result;
  }

  /** Get all flags for an arbitrary identity (for backoffice user inspection). */
  public Map<String, Object> getAllFlagsForIdentity(String identity, Map<String, Object> traits) {
    Map<String, Object> result = new HashMap<>();
    if (flagsmithClient == null) {
      return result;
    }
    try {
      Flags flags = flagsmithClient.getIdentityFlags(identity, traits);
      flags
          .getAllFlags()
          .forEach(
              flag -> {
                Map<String, Object> flagData = new HashMap<>();
                flagData.put("enabled", flag.getEnabled());
                flagData.put("value", flag.getValue());
                result.put(flag.getFeatureName(), flagData);
              });
    } catch (Exception e) {
      log.warn("Failed to get flags for identity '{}', returning empty map", identity, e);
    }
    return result;
  }

  private String buildIdentity(UserPrincipal principal) {
    return "team:%s_user:%s"
        .formatted(principal.getTeamIdentifier(), principal.getUserIdentifier());
  }

  private Map<String, Object> buildTraits(UserPrincipal principal) {
    Map<String, Object> traits = new HashMap<>();
    traits.put("email", principal.getEmail());
    traits.put("is_owner", principal.isOwner());
    if (principal.getTeamIdentifier() != null) {
      traits.put("team", principal.getTeamIdentifier());
    }
    if (principal.getRole() != null) {
      traits.put("role", principal.getRole());
    }
    return traits;
  }
}
