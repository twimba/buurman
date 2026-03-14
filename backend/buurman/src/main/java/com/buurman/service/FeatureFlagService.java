package com.buurman.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.buurman.config.PostHogFlagClient;
import com.buurman.repository.TeamRepository;
import com.buurman.security.UserPrincipal;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FeatureFlagService {

  private static final String GLOBAL_IDENTITY = "__global__";

  private final Optional<PostHogFlagClient> postHogClient;
  private final TeamRepository teamRepository;

  public FeatureFlagService(
      Optional<PostHogFlagClient> postHogClient, TeamRepository teamRepository) {
    this.postHogClient = postHogClient;
    this.teamRepository = teamRepository;
    if (postHogClient.isEmpty()) {
      log.warn("PostHog client not configured — all feature flags will default to OFF");
    }
  }

  /** Global flag evaluation (no identity context). */
  public boolean isEnabled(String flagKey) {
    return postHogClient
        .map(
            client -> {
              try {
                return client.isEnabled(
                    flagKey, GLOBAL_IDENTITY, Collections.emptyMap(), Collections.emptyMap());
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
   * Team-level flag evaluation. Builds a team-only identity with the team's demo trait so PostHog
   * can target flags by cohort (e.g. "Demo Accounts" cohort where demo=true).
   */
  public boolean isEnabled(String flagKey, UUID teamId) {
    return teamRepository
        .findById(teamId)
        .map(
            team -> {
              String identity = "team:" + team.getIdentifier().orElseThrow();
              Map<String, Object> personProps = Map.of("demo", team.isDemo());
              return postHogClient
                  .map(
                      client -> {
                        try {
                          return client.isEnabled(
                              flagKey, identity, personProps, Collections.emptyMap());
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
    return postHogClient
        .map(
            client -> {
              try {
                String identity = buildIdentity(principal);
                Map<String, Object> personProps = buildPersonProperties(principal);
                Map<String, String> groups = buildGroups(principal);
                return client.isEnabled(flagKey, identity, personProps, groups);
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
    return postHogClient.flatMap(
        client -> {
          try {
            String identity = buildIdentity(principal);
            Map<String, Object> personProps = buildPersonProperties(principal);
            Map<String, String> groups = buildGroups(principal);
            return Optional.ofNullable(client.getPayload(flagKey, identity, personProps, groups));
          } catch (Exception e) {
            log.warn(
                "Failed to get value for flag '{}' for identity, returning empty", flagKey, e);
            return Optional.empty();
          }
        });
  }

  /** Get all evaluated flags for the current user (for the frontend endpoint). */
  public Map<String, Object> getAllFlags(UserPrincipal principal) {
    return postHogClient
        .map(
            client -> {
              try {
                String identity = buildIdentity(principal);
                Map<String, Object> personProps = buildPersonProperties(principal);
                Map<String, String> groups = buildGroups(principal);
                return toFlagMap(client.evaluateAll(identity, personProps, groups));
              } catch (Exception e) {
                log.warn("Failed to get all flags for identity, returning empty map", e);
                return Collections.<String, Object>emptyMap();
              }
            })
        .orElseGet(Collections::emptyMap);
  }

  /** Get all environment-level flags (no identity context). */
  public Map<String, Object> getAllEnvironmentFlags() {
    return postHogClient
        .map(
            client -> {
              try {
                return toFlagMap(
                    client.evaluateAll(
                        GLOBAL_IDENTITY, Collections.emptyMap(), Collections.emptyMap()));
              } catch (Exception e) {
                log.warn("Failed to get environment flags, returning empty map", e);
                return Collections.<String, Object>emptyMap();
              }
            })
        .orElseGet(Collections::emptyMap);
  }

  /** Get all flags for an arbitrary identity (for backoffice user inspection). */
  public Map<String, Object> getAllFlagsForIdentity(String identity, Map<String, Object> traits) {
    return postHogClient
        .map(
            client -> {
              try {
                return toFlagMap(client.evaluateAll(identity, traits, Collections.emptyMap()));
              } catch (Exception e) {
                log.warn(
                    "Failed to get flags for identity '{}', returning empty map", identity, e);
                return Collections.<String, Object>emptyMap();
              }
            })
        .orElseGet(Collections::emptyMap);
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

  private Map<String, Object> buildPersonProperties(UserPrincipal principal) {
    Map<String, Object> props = new HashMap<>();
    props.put("distinct_id", buildIdentity(principal));
    props.put("email", principal.getEmail());
    props.put("is_owner", principal.isOwner());
    principal.getTeamIdentifier().ifPresent(team -> props.put("team", team));
    principal.getRole().ifPresent(r -> props.put("role", r.name()));
    principal
        .getTeamId()
        .flatMap(teamRepository::findById)
        .ifPresent(team -> props.put("demo", team.isDemo()));
    return props;
  }

  private Map<String, String> buildGroups(UserPrincipal principal) {
    Map<String, String> groups = new HashMap<>();
    principal.getTeamIdentifier().ifPresent(team -> groups.put("team", team));
    return groups;
  }

  /**
   * Converts PostHogFlagClient results to the format expected by the frontend: { "flagKey": {
   * "enabled": bool, "value": payload } }
   */
  private static Map<String, Object> toFlagMap(Map<String, PostHogFlagClient.FlagResult> results) {
    Map<String, Object> map = new HashMap<>();
    results.forEach(
        (key, result) -> {
          Map<String, Object> flagData = new HashMap<>();
          flagData.put("enabled", result.enabled());
          flagData.put("value", result.payload());
          map.put(key, flagData);
        });
    return map;
  }
}
