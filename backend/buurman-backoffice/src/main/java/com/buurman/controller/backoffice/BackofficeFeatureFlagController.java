package com.buurman.controller.backoffice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.domain.identifier.TeamIdentifier;
import com.buurman.domain.identifier.UserIdentifier;
import com.buurman.dto.request.backoffice.UpdateFeatureFlagRequest;
import com.buurman.dto.response.backoffice.FeatureFlagUpdateResponse;
import com.buurman.dto.response.backoffice.SegmentEvaluation;
import com.buurman.dto.response.backoffice.SegmentFlagOverride;
import com.buurman.dto.response.backoffice.TeamFlagEvaluation;
import com.buurman.generated.backoffice.api.BackofficeFeatureFlagsApi;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.PostHogAdminService;
import com.buurman.service.PostHogAdminService.CohortInfo;
import com.buurman.service.PostHogAdminService.FeatureFlagInfo;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeFeatureFlagController implements BackofficeFeatureFlagsApi {

  private final FeatureFlagService featureFlagService;
  private final PostHogAdminService postHogAdminService;
  private final UserRepository userRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final TeamRepository teamRepository;

  // --- Read endpoints ---

  @Override
  public Map<String, Object> getAdminStatus() {
    return Map.of(
        "adminConfigured", postHogAdminService.isAdminConfigured(),
        "authMethod", postHogAdminService.getAuthMethod());
  }

  @Override
  public Map<String, Object> getGlobalFlags() {
    return featureFlagService.getAllEnvironmentFlags();
  }

  @Override
  public List<TeamFlagEvaluation> getUserFlags(UserIdentifier userIdentifier) {
    User user =
        userRepository
            .findByIdentifierUnscoped(userIdentifier)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    List<TeamMember> memberships = teamMemberRepository.findAllByUserId(user.getId());
    Optional<UUID> activeTeamId = resolveActiveTeamId(user, memberships);

    List<TeamFlagEvaluation> evaluations = new ArrayList<>();

    for (TeamMember membership : memberships) {
      Team team = teamRepository.findById(membership.getTeamId()).orElse(null);
      if (team == null) {
        continue;
      }

      Sid teamId = team.getIdentifier().orElseThrow();
      Sid userId = user.getIdentifier().orElseThrow();

      String identity = FeatureFlagService.buildIdentity(teamId.value(), userId.value());

      Map<String, Object> traits = new HashMap<>();
      traits.put("team", teamId);
      traits.put("role", membership.getRole());
      traits.put("is_owner", membership.isOwner());

      Map<String, Object> flags = featureFlagService.getAllFlagsForIdentity(identity, traits);

      evaluations.add(
          new TeamFlagEvaluation(
              teamId,
              team.getName(),
              membership.getRole().name(),
              membership.isOwner(),
              activeTeamId.map(membership.getTeamId()::equals).orElse(false),
              flags));
    }

    return evaluations;
  }

  // --- Mutation endpoints ---

  @Override
  public FeatureFlagUpdateResponse updateGlobalFlag(
      String flagName, UpdateFeatureFlagRequest updateFeatureFlagRequest) {

    FeatureFlagInfo flag =
        postHogAdminService
            .findFeatureFlagByKey(flagName)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Feature flag '%s' not found".formatted(flagName)));

    FeatureFlagInfo updated =
        postHogAdminService.updateFeatureFlag(
            flag.id(),
            updateFeatureFlagRequest.enabled(),
            updateFeatureFlagRequest.value());

    return new FeatureFlagUpdateResponse(updated.key(), updated.active(), updated.payload());
  }

  @Override
  public FeatureFlagUpdateResponse upsertIdentityOverride(
      UserIdentifier userIdentifier,
      TeamIdentifier teamIdentifier,
      String flagName,
      UpdateFeatureFlagRequest updateFeatureFlagRequest) {

    String identity =
        FeatureFlagService.buildIdentity(teamIdentifier.value(), userIdentifier.value());

    FeatureFlagInfo flag =
        postHogAdminService
            .findFeatureFlagByKey(flagName)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Feature flag '%s' not found".formatted(flagName)));

    boolean enabled =
        updateFeatureFlagRequest.enabled() != null
            ? updateFeatureFlagRequest.enabled()
            : flag.active();

    postHogAdminService.upsertIdentityOverride(flag.id(), identity, enabled);

    return new FeatureFlagUpdateResponse(flagName, enabled, flag.payload());
  }

  @Override
  public void deleteIdentityOverride(
      UserIdentifier userIdentifier, TeamIdentifier teamIdentifier, String flagName) {

    String identity =
        FeatureFlagService.buildIdentity(teamIdentifier.value(), userIdentifier.value());

    FeatureFlagInfo flag =
        postHogAdminService
            .findFeatureFlagByKey(flagName)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Feature flag '%s' not found".formatted(flagName)));

    postHogAdminService.deleteIdentityOverride(flag.id(), identity);
  }

  // --- Cohort (segment) endpoints ---

  @Override
  public List<SegmentEvaluation> getSegmentOverrides() {
    List<CohortInfo> cohorts = postHogAdminService.listCohorts();
    List<FeatureFlagInfo> allFlags = postHogAdminService.listFeatureFlags();

    return cohorts.stream()
        .map(
            cohort -> {
              Map<String, Boolean> cohortOverrides =
                  postHogAdminService.getCohortOverridesFromFlags(cohort.id());

              Map<String, SegmentFlagOverride> overrides = new HashMap<>();
              for (FeatureFlagInfo flag : allFlags) {
                Boolean overrideEnabled = cohortOverrides.get(flag.key());
                boolean enabled = overrideEnabled != null ? overrideEnabled : flag.active();
                overrides.put(
                    flag.key(),
                    new SegmentFlagOverride(flag.key(), enabled, flag.payload()));
              }

              return new SegmentEvaluation(
                  cohort.id(), cohort.name(), cohort.description(), overrides);
            })
        .toList();
  }

  @Override
  public FeatureFlagUpdateResponse upsertSegmentOverride(
      Long segmentId, String flagName, UpdateFeatureFlagRequest updateFeatureFlagRequest) {

    FeatureFlagInfo flag =
        postHogAdminService
            .findFeatureFlagByKey(flagName)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Feature flag '%s' not found".formatted(flagName)));

    boolean enabled =
        updateFeatureFlagRequest.enabled() != null
            ? updateFeatureFlagRequest.enabled()
            : flag.active();

    postHogAdminService.upsertCohortOverride(flag.id(), segmentId, enabled);

    return new FeatureFlagUpdateResponse(flagName, enabled, flag.payload());
  }

  @Override
  public void deleteSegmentOverride(Long segmentId, String flagName) {

    FeatureFlagInfo flag =
        postHogAdminService
            .findFeatureFlagByKey(flagName)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Feature flag '%s' not found".formatted(flagName)));

    postHogAdminService.deleteCohortOverride(flag.id(), segmentId);
  }

  // --- Internal ---

  private Optional<UUID> resolveActiveTeamId(User user, List<TeamMember> memberships) {
    if (memberships.isEmpty()) {
      return Optional.empty();
    }

    Optional<UUID> activeTeamIdOpt = user.getActiveTeamId();
    if (activeTeamIdOpt.isPresent()
        && memberships.stream().anyMatch(m -> m.getTeamId().equals(activeTeamIdOpt.get()))) {
      return activeTeamIdOpt;
    }

    Optional<UUID> defaultTeamIdOpt = user.getDefaultTeamId();
    if (defaultTeamIdOpt.isPresent()
        && memberships.stream().anyMatch(m -> m.getTeamId().equals(defaultTeamIdOpt.get()))) {
      return defaultTeamIdOpt;
    }

    return Optional.of(memberships.getFirst().getTeamId());
  }
}
