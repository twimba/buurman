package com.buurman.controller.backoffice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/backoffice/feature-flags")
@Tag(name = "Backoffice - Feature Flags", description = "Feature flag management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class BackofficeFeatureFlagController {

  private final FeatureFlagService featureFlagService;
  private final FlagsmithAdminService flagsmithAdminService;
  private final UserRepository userRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final TeamRepository teamRepository;

  // --- Records ---

  public record TeamFlagEvaluation(
      String teamIdentifier,
      String teamName,
      String role,
      boolean isOwner,
      boolean isActive,
      Map<String, Object> flags) {}

  public record UpdateFeatureFlagRequest(@Nullable Boolean enabled, @Nullable String value) {}

  public record FeatureFlagUpdateResponse(
      String flagName, boolean enabled, @Nullable Object value) {}

  public record SegmentFlagOverride(
      @Nullable String flagName, boolean enabled, @Nullable Object value) {}

  public record SegmentEvaluation(
      long segmentId,
      @Nullable String segmentName,
      @Nullable String description,
      Map<String, SegmentFlagOverride> overrides) {}

  // --- Read endpoints ---

  @Operation(
      summary = "Check if Flagsmith admin operations are available",
      description =
          "Returns whether the backend has admin credentials configured for flag management")
  @GetMapping("/admin-status")
  public Map<String, Object> getAdminStatus() {
    return Map.of(
        "adminConfigured", flagsmithAdminService.isAdminConfigured(),
        "authMethod", flagsmithAdminService.getAuthMethod());
  }

  @Operation(
      summary = "Get global feature flag status",
      description = "Returns all flags at environment level (no identity context)")
  @GetMapping
  public Map<String, Object> getGlobalFlags() {
    return featureFlagService.getAllEnvironmentFlags();
  }

  @Operation(
      summary = "Get feature flags for a specific user across all teams",
      description = "Evaluates all flags for each team membership the user has")
  @GetMapping("/users/{userIdentifier}")
  public List<TeamFlagEvaluation> getUserFlags(@PathVariable String userIdentifier) {
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

      String identity =
          FeatureFlagService.buildIdentity(
              Objects.requireNonNull(team.getIdentifier()),
              Objects.requireNonNull(user.getIdentifier()));

      Map<String, Object> traits = new HashMap<>();
      traits.put("team", team.getIdentifier());
      traits.put("role", membership.getRole());
      traits.put("is_owner", membership.isOwner());

      Map<String, Object> flags = featureFlagService.getAllFlagsForIdentity(identity, traits);

      evaluations.add(
          new TeamFlagEvaluation(
              team.getIdentifier(),
              team.getName(),
              membership.getRole(),
              membership.isOwner(),
              activeTeamId.map(membership.getTeamId()::equals).orElse(false),
              flags));
    }

    return evaluations;
  }

  // --- Mutation endpoints ---

  @Operation(
      summary = "Update a global feature flag",
      description = "Toggles enabled state and/or updates the value of an environment-level flag")
  @PatchMapping("/{flagName}")
  public ResponseEntity<FeatureFlagUpdateResponse> updateGlobalFlag(
      @PathVariable String flagName, @RequestBody UpdateFeatureFlagRequest request) {

    FeatureStateInfo current =
        flagsmithAdminService
            .findFeatureStateByName(flagName)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Feature flag '%s' not found".formatted(flagName)));

    FeatureStateInfo updated =
        flagsmithAdminService.updateFeatureState(
            current.featureStateId(), request.enabled(), request.value());

    return ResponseEntity.ok(
        new FeatureFlagUpdateResponse(updated.featureName(), updated.enabled(), updated.value()));
  }

  @Operation(
      summary = "Create or update an identity override",
      description = "Sets a feature flag override for a specific user in a specific team")
  @PutMapping("/identities/{userIdentifier}/teams/{teamIdentifier}/{flagName}")
  public ResponseEntity<FeatureFlagUpdateResponse> upsertIdentityOverride(
      @PathVariable String userIdentifier,
      @PathVariable String teamIdentifier,
      @PathVariable String flagName,
      @RequestBody UpdateFeatureFlagRequest request) {

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

    boolean enabled = request.enabled() != null ? request.enabled() : globalState.enabled();
    String value = request.value();

    IdentityOverrideInfo result;
    if (existingOverride != null) {
      result =
          flagsmithAdminService.updateIdentityOverride(
              identityInfo.id(), existingOverride.featureStateId(), request.enabled(), value);
    } else {
      result =
          flagsmithAdminService.createIdentityOverride(
              identityInfo.id(), globalState.featureId(), enabled, value);
    }

    return ResponseEntity.ok(
        new FeatureFlagUpdateResponse(flagName, result.enabled(), result.value()));
  }

  @Operation(
      summary = "Remove an identity override",
      description = "Removes a feature flag override so the user falls back to the global default")
  @DeleteMapping("/identities/{userIdentifier}/teams/{teamIdentifier}/{flagName}")
  public ResponseEntity<Void> deleteIdentityOverride(
      @PathVariable String userIdentifier,
      @PathVariable String teamIdentifier,
      @PathVariable String flagName) {

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
    return ResponseEntity.noContent().build();
  }

  // --- Segment endpoints ---

  @Operation(
      summary = "Get all segments with their feature flag overrides",
      description =
          "Returns segments configured in Flagsmith with any feature flag overrides they define")
  @GetMapping("/segments")
  public List<SegmentEvaluation> getSegmentOverrides() {
    List<SegmentWithOverrides> raw = flagsmithAdminService.getSegmentOverrides();

    return raw.stream()
        .map(
            seg -> {
              Map<String, SegmentFlagOverride> overrides = new HashMap<>();
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

  @Operation(
      summary = "Create or update a segment override",
      description = "Sets a feature flag override for a specific segment")
  @PutMapping("/segments/{segmentId}/{flagName}")
  public ResponseEntity<FeatureFlagUpdateResponse> upsertSegmentOverride(
      @PathVariable long segmentId,
      @PathVariable String flagName,
      @RequestBody UpdateFeatureFlagRequest request) {

    FeatureStateInfo globalState =
        flagsmithAdminService
            .findFeatureStateByName(flagName)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Feature flag '%s' not found".formatted(flagName)));

    // Check if a feature-segment link already exists
    var existingFsId =
        flagsmithAdminService.findFeatureSegmentId(globalState.featureId(), segmentId);
    if (existingFsId.isEmpty()) {
      // Create the link (auto-creates a FeatureState with env defaults)
      flagsmithAdminService.createFeatureSegment(globalState.featureId(), segmentId);
    }

    // Find the feature state for this segment override
    Long featureStateId =
        flagsmithAdminService
            .findSegmentOverrideFeatureStateId(segmentId, globalState.featureId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Could not find feature state for segment override (segment=%d, feature=%s)"
                            .formatted(segmentId, flagName)));

    // Update the segment override feature state (uses /features/featurestates/ not /environments/)
    FeatureStateInfo updated =
        flagsmithAdminService.updateSegmentOverrideState(
            featureStateId, request.enabled(), request.value());
    return ResponseEntity.ok(
        new FeatureFlagUpdateResponse(flagName, updated.enabled(), updated.value()));
  }

  @Operation(
      summary = "Remove a segment override",
      description =
          "Removes a feature flag override so the segment falls back to the global default")
  @DeleteMapping("/segments/{segmentId}/{flagName}")
  public ResponseEntity<Void> deleteSegmentOverride(
      @PathVariable long segmentId, @PathVariable String flagName) {

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
    return ResponseEntity.noContent().build();
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
