package com.buurman.controller.backoffice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.Ulid;
import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
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
import com.buurman.service.FlagsmithAdminService;
import com.buurman.service.FlagsmithAdminService.FeatureStateInfo;
import com.buurman.service.FlagsmithAdminService.IdentityInfo;
import com.buurman.service.FlagsmithAdminService.IdentityOverrideInfo;
import com.buurman.service.FlagsmithAdminService.SegmentOverrideState;
import com.buurman.service.FlagsmithAdminService.SegmentWithOverrides;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeFeatureFlagController implements BackofficeFeatureFlagsApi {

  private final FeatureFlagService featureFlagService;
  private final FlagsmithAdminService flagsmithAdminService;
  private final UserRepository userRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final TeamRepository teamRepository;

  // --- Read endpoints ---

  @Override
  public Map<String, Object> getAdminStatus() {
    return Map.of(
        "adminConfigured", flagsmithAdminService.isAdminConfigured(),
        "authMethod", flagsmithAdminService.getAuthMethod());
  }

  @Override
  public Map<String, Object> getGlobalFlags() {
    return featureFlagService.getAllEnvironmentFlags();
  }

  @Override
  public List<TeamFlagEvaluation> getUserFlags(String userIdentifier) {
    User user =
        userRepository
            .findByIdentifierUnscoped(Ulid.of(userIdentifier))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    List<TeamMember> memberships = teamMemberRepository.findAllByUserId(user.getId());
    Optional<UUID> activeTeamId = resolveActiveTeamId(user, memberships);

    List<TeamFlagEvaluation> evaluations = new ArrayList<>();

    for (TeamMember membership : memberships) {
      Team team = teamRepository.findById(membership.getTeamId()).orElse(null);
      if (team == null) {
        continue;
      }

      Ulid teamId = team.getIdentifier().orElseThrow();
      Ulid userId = user.getIdentifier().orElseThrow();

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

    FeatureStateInfo current =
        flagsmithAdminService
            .findFeatureStateByName(flagName)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Feature flag '%s' not found".formatted(flagName)));

    FeatureStateInfo updated =
        flagsmithAdminService.updateFeatureState(
            current.featureStateId(),
            updateFeatureFlagRequest.enabled(),
            updateFeatureFlagRequest.value());

    return new FeatureFlagUpdateResponse(updated.featureName(), updated.enabled(), updated.value());
  }

  @Override
  public FeatureFlagUpdateResponse upsertIdentityOverride(
      String userIdentifier,
      String teamIdentifier,
      String flagName,
      UpdateFeatureFlagRequest updateFeatureFlagRequest) {

    String identity = FeatureFlagService.buildIdentity(teamIdentifier, userIdentifier);

    // Find the feature ID from the global feature states
    FeatureStateInfo globalState =
        flagsmithAdminService
            .findFeatureStateByName(flagName)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Feature flag '%s' not found".formatted(flagName)));

    // Find or get the identity in Flagsmith
    IdentityInfo identityInfo =
        flagsmithAdminService
            .findIdentity(identity)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Identity '%s' not found in Flagsmith. The user must have logged in at least once with this team."
                            .formatted(identity)));

    // Check if an override already exists
    List<IdentityOverrideInfo> existing =
        flagsmithAdminService.listIdentityOverrides(identityInfo.id());
    IdentityOverrideInfo existingOverride =
        existing.stream()
            .filter(o -> o.featureId() == globalState.featureId())
            .findFirst()
            .orElse(null);

    boolean enabled =
        updateFeatureFlagRequest.enabled() != null
            ? updateFeatureFlagRequest.enabled()
            : globalState.enabled();
    String value = updateFeatureFlagRequest.value();

    IdentityOverrideInfo result;
    if (existingOverride != null) {
      result =
          flagsmithAdminService.updateIdentityOverride(
              identityInfo.id(),
              existingOverride.featureStateId(),
              updateFeatureFlagRequest.enabled(),
              value);
    } else {
      result =
          flagsmithAdminService.createIdentityOverride(
              identityInfo.id(), globalState.featureId(), enabled, value);
    }

    return new FeatureFlagUpdateResponse(flagName, result.enabled(), result.value());
  }

  @Override
  public void deleteIdentityOverride(
      String userIdentifier, String teamIdentifier, String flagName) {

    String identity = FeatureFlagService.buildIdentity(teamIdentifier, userIdentifier);

    FeatureStateInfo globalState =
        flagsmithAdminService
            .findFeatureStateByName(flagName)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Feature flag '%s' not found".formatted(flagName)));

    IdentityInfo identityInfo =
        flagsmithAdminService
            .findIdentity(identity)
            .orElseThrow(
                () -> new IllegalArgumentException("Identity '%s' not found".formatted(identity)));

    List<IdentityOverrideInfo> overrides =
        flagsmithAdminService.listIdentityOverrides(identityInfo.id());
    IdentityOverrideInfo target =
        overrides.stream()
            .filter(o -> o.featureId() == globalState.featureId())
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No override found for flag '%s' on identity '%s'"
                            .formatted(flagName, identity)));

    flagsmithAdminService.deleteIdentityOverride(identityInfo.id(), target.featureStateId());
  }

  // --- Segment endpoints ---

  @Override
  public List<SegmentEvaluation> getSegmentOverrides() {
    List<SegmentWithOverrides> raw = flagsmithAdminService.getSegmentOverrides();
    List<FeatureStateInfo> globalFlags = flagsmithAdminService.listFeatureStates();

    return raw.stream()
        .map(
            seg -> {
              // Start with global defaults for all flags
              Map<String, SegmentFlagOverride> overrides = new HashMap<>();
              for (FeatureStateInfo global : globalFlags) {
                overrides.put(
                    global.featureName(),
                    new SegmentFlagOverride(
                        global.featureName(), global.enabled(), global.value()));
              }
              // Layer segment overrides on top
              for (SegmentOverrideState state : seg.overrides()) {
                overrides.put(
                    state.featureName(),
                    new SegmentFlagOverride(state.featureName(), state.enabled(), state.value()));
              }
              return new SegmentEvaluation(
                  seg.segmentId(), seg.segmentName(), seg.description(), overrides);
            })
        .toList();
  }

  @Override
  public FeatureFlagUpdateResponse upsertSegmentOverride(
      Long segmentId, String flagName, UpdateFeatureFlagRequest updateFeatureFlagRequest) {

    FeatureStateInfo globalState =
        flagsmithAdminService
            .findFeatureStateByName(flagName)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Feature flag '%s' not found".formatted(flagName)));

    // Ensure a feature-segment link exists, creating one if needed
    long linkId =
        flagsmithAdminService
            .findFeatureSegmentId(globalState.featureId(), segmentId)
            .orElseGet(
                () ->
                    flagsmithAdminService.createFeatureSegment(globalState.featureId(), segmentId));

    // Find the feature state for this segment override
    var featureStateId =
        flagsmithAdminService.findSegmentOverrideFeatureStateId(segmentId, globalState.featureId());

    FeatureStateInfo updated;
    if (featureStateId.isPresent()) {
      // Update existing feature state
      updated =
          flagsmithAdminService.updateSegmentOverrideState(
              featureStateId.get(),
              segmentId,
              updateFeatureFlagRequest.enabled(),
              updateFeatureFlagRequest.value());
    } else {
      // No auto-created feature state — create one explicitly with desired values
      updated =
          flagsmithAdminService.createSegmentOverrideFeatureState(
              linkId,
              globalState.featureId(),
              updateFeatureFlagRequest.enabled(),
              updateFeatureFlagRequest.value());
    }
    return new FeatureFlagUpdateResponse(flagName, updated.enabled(), updated.value());
  }

  @Override
  public void deleteSegmentOverride(Long segmentId, String flagName) {

    FeatureStateInfo globalState =
        flagsmithAdminService
            .findFeatureStateByName(flagName)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Feature flag '%s' not found".formatted(flagName)));

    long featureSegmentId =
        flagsmithAdminService
            .findFeatureSegmentId(globalState.featureId(), segmentId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No segment override found for flag '%s' on segment %d"
                            .formatted(flagName, segmentId)));

    flagsmithAdminService.deleteFeatureSegment(featureSegmentId);
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
