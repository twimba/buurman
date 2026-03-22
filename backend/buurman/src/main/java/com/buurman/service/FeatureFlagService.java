package com.buurman.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.buurman.repository.TeamRepository;
import com.buurman.security.UserPrincipal;
import com.flagsmith.FlagsmithClient;
import com.flagsmith.models.Flags;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FeatureFlagService {

  private final ObjectProvider<FlagsmithClient> flagsmithClientProvider;
  private final TeamRepository teamRepository;

  public FeatureFlagService(
      ObjectProvider<FlagsmithClient> flagsmithClientProvider, TeamRepository teamRepository) {
    this.flagsmithClientProvider = flagsmithClientProvider;
    this.teamRepository = teamRepository;
  }

  private Optional<FlagsmithClient> client() {
    return Optional.ofNullable(flagsmithClientProvider.getIfAvailable());
  }

  /** Global flag evaluation (no identity context). */
  public boolean isEnabled(String flagKey) {
    return client()
        .map(
            client -> {
              try {
                return client.getEnvironmentFlags().isFeatureEnabled(flagKey);
              } catch (Exception e) {
                log.warn("Failed to evaluate flag '{}', defaulting to false", flagKey, e);
                return false;
              }
            })
        .orElse(false);
  }

  public boolean isDisabled(String flagKey) {
    return !isEnabled(flagKey);
  }

  /**
   * Team-level flag evaluation. Builds a team-only identity with the team's demo trait so Flagsmith
   * can target flags by segment (e.g. "Demo accounts" segment where demo=true).
   */
  public boolean isEnabled(String flagKey, UUID teamId) {
    return teamRepository
        .findById(teamId)
        .map(
            team -> {
              String identity = "team:" + team.getIdentifier().orElseThrow();
              Map<String, Object> traits = Map.of("demo", team.isDemo());
              return client()
                  .map(
                      client -> {
                        try {
                          return client
                              .getIdentityFlags(identity, traits)
                              .isFeatureEnabled(flagKey);
                        } catch (Exception e) {
                          log.warn(
                              "Failed to evaluate flag '{}' for team {}, defaulting to false",
                              flagKey,
                              teamId,
                              e);
                          return false;
                        }
                      })
                  .orElse(false);
            })
        .orElse(false);
  }

  /** Identity-aware flag evaluation with user/team traits. */
  public boolean isEnabled(String flagKey, UserPrincipal principal) {
    return client()
        .map(
            client -> {
              try {
                String identity = buildIdentity(principal);
                Map<String, Object> traits = buildTraits(principal);
                return client.getIdentityFlags(identity, traits).isFeatureEnabled(flagKey);
              } catch (Exception e) {
                log.warn(
                    "Failed to evaluate flag '{}' for identity, defaulting to false", flagKey, e);
                return false;
              }
            })
        .orElse(false);
  }

  public boolean isDisabled(String flagKey, UserPrincipal principal) {
    return !isEnabled(flagKey, principal);
  }

  /** Get remote config value for a flag (identity-aware). */
  public Optional<Object> getValue(String flagKey, UserPrincipal principal) {
    return client().flatMap(
        client -> {
          try {
            String identity = buildIdentity(principal);
            Map<String, Object> traits = buildTraits(principal);
            Flags flags = client.getIdentityFlags(identity, traits);
            return Optional.ofNullable(flags.getFeatureValue(flagKey));
          } catch (Exception e) {
            log.warn("Failed to get value for flag '{}' for identity, returning empty", flagKey, e);
            return Optional.empty();
          }
        });
  }

  /** Get all evaluated flags for the current user (for the frontend endpoint). */
  public Map<String, Object> getAllFlags(UserPrincipal principal) {
    Map<String, Object> result = new HashMap<>();
    client().ifPresent(
        client -> {
          try {
            Flags flags = client.getIdentityFlags(buildIdentity(principal), buildTraits(principal));
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
        });
    return result;
  }

  /** Get all environment-level flags (no identity context). */
  public Map<String, Object> getAllEnvironmentFlags() {
    Map<String, Object> result = new HashMap<>();
    client().ifPresent(
        client -> {
          try {
            Flags flags = client.getEnvironmentFlags();
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
        });
    return result;
  }

  /** Get all flags for an arbitrary identity (for backoffice user inspection). */
  public Map<String, Object> getAllFlagsForIdentity(String identity, Map<String, Object> traits) {
    Map<String, Object> result = new HashMap<>();
    client().ifPresent(
        client -> {
          try {
            Flags flags = client.getIdentityFlags(identity, traits);
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
        });
    return result;
  }

  private String buildIdentity(UserPrincipal principal) {
    return buildIdentity(
        principal
            .getTeamIdentifier()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Team identifier required for feature flag evaluation")),
        principal.getUserIdentifier());
  }

  public static String buildIdentity(String teamIdentifier, String userIdentifier) {
    return "team:%s_user:%s".formatted(teamIdentifier, userIdentifier);
  }

  private Map<String, Object> buildTraits(UserPrincipal principal) {
    Map<String, Object> traits = new HashMap<>();
    traits.put("email", principal.getEmail());
    traits.put("is_owner", principal.isOwner());
    principal.getTeamIdentifier().ifPresent(team -> traits.put("team", team));
    principal.getRole().ifPresent(r -> traits.put("role", r.name()));
    principal
        .getTeamId()
        .flatMap(teamRepository::findById)
        .ifPresent(team -> traits.put("demo", team.isDemo()));
    return traits;
  }
}
