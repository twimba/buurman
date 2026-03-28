package com.buurman.controller.backoffice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.FeatureFlagOverride;
import com.buurman.domain.SegmentDefinition;
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
import com.buurman.exception.NotFoundException;
import com.buurman.generated.backoffice.api.BackofficeFeatureFlagsApi;
import com.buurman.repository.FeatureFlagOverrideRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.SecurityUtils;
import com.buurman.service.FeatureFlagAdminService;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.SegmentAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeFeatureFlagController implements BackofficeFeatureFlagsApi {

  private final FeatureFlagService featureFlagService;
  private final FeatureFlagAdminService featureFlagAdminService;
  private final SegmentAdminService segmentAdminService;
  private final FeatureFlagOverrideRepository overrideRepo;
  private final UserRepository userRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final TeamRepository teamRepository;

  // --- Read endpoints ---

  @Override
  public Map<String, Object> getAdminStatus() {
    return Map.of("adminConfigured", true, "authMethod", "builtin");
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
            .orElseThrow(() -> new NotFoundException("User not found"));

    List<TeamMember> memberships = teamMemberRepository.findAllByUserId(user.getId());
    Optional<UUID> activeTeamId = resolveActiveTeamId(user, memberships);

    List<TeamFlagEvaluation> evaluations = new ArrayList<>();

    for (TeamMember membership : memberships) {
      teamRepository
          .findById(membership.getTeamId())
          .ifPresent(
              team -> {
                Sid teamSid = team.getIdentifier().orElseThrow();

                Map<String, Object> flags =
                    featureFlagService.evaluateAllForUser(
                        user.getId(),
                        membership.getTeamId(),
                        membership.isOwner(),
                        membership.getRole(),
                        user.getEmail(),
                        user.getEmailVerifiedAt().isPresent());

                evaluations.add(
                    new TeamFlagEvaluation(
                        teamSid,
                        team.getName(),
                        membership.getRole().name(),
                        membership.isOwner(),
                        activeTeamId.map(membership.getTeamId()::equals).orElse(false),
                        flags));
              });
    }

    return evaluations;
  }

  // --- Mutation endpoints ---

  @Override
  public FeatureFlagUpdateResponse updateGlobalFlag(
      String flagName, UpdateFeatureFlagRequest updateFeatureFlagRequest) {
    var updated =
        featureFlagAdminService.updateFlag(
            flagName,
            Optional.ofNullable(updateFeatureFlagRequest.enabled()),
            Optional.ofNullable(updateFeatureFlagRequest.value()),
            Optional.empty(),
            currentActorId());

    return new FeatureFlagUpdateResponse(
        updated.getKey(), updated.isDefaultEnabled(), updated.getDefaultValue().orElse(null));
  }

  @Override
  public FeatureFlagUpdateResponse upsertIdentityOverride(
      UserIdentifier userIdentifier,
      TeamIdentifier teamIdentifier,
      String flagName,
      UpdateFeatureFlagRequest updateFeatureFlagRequest) {

    User user =
        userRepository
            .findByIdentifierUnscoped(userIdentifier)
            .orElseThrow(() -> new NotFoundException("User not found"));
    Team team =
        teamRepository
            .findByIdentifierForBackoffice(teamIdentifier)
            .orElseThrow(() -> new NotFoundException("Team not found"));

    boolean enabled =
        updateFeatureFlagRequest.enabled() != null ? updateFeatureFlagRequest.enabled() : false;

    var override =
        featureFlagAdminService.upsertUserOverride(
            flagName,
            team.getId(),
            user.getId(),
            enabled,
            Optional.ofNullable(updateFeatureFlagRequest.value()),
            currentActorId());

    return new FeatureFlagUpdateResponse(
        flagName, override.isEnabled(), override.getValue().orElse(null));
  }

  @Override
  public void deleteIdentityOverride(
      UserIdentifier userIdentifier, TeamIdentifier teamIdentifier, String flagName) {

    User user =
        userRepository
            .findByIdentifierUnscoped(userIdentifier)
            .orElseThrow(() -> new NotFoundException("User not found"));
    Team team =
        teamRepository
            .findByIdentifierForBackoffice(teamIdentifier)
            .orElseThrow(() -> new NotFoundException("Team not found"));

    featureFlagAdminService.deleteUserOverride(
        flagName, team.getId(), user.getId(), currentActorId());
  }

  // --- Segment endpoints ---

  @Override
  public List<SegmentEvaluation> getSegmentOverrides() {
    List<Map<String, Object>> segments = featureFlagAdminService.listSegments();
    Map<String, Object> globalFlags = featureFlagService.getAllEnvironmentFlags();

    // Build a key->index map from DB segments for stable IDs
    List<SegmentDefinition> dbSegments = segmentAdminService.listSegments();
    Map<String, Long> segmentKeyToId = new HashMap<>();
    for (int i = 0; i < dbSegments.size(); i++) {
      segmentKeyToId.put(dbSegments.get(i).getKey(), (long) i);
    }

    return segments.stream()
        .map(
            seg -> {
              String segKey = String.valueOf(seg.get("segmentKey"));
              String segName = String.valueOf(seg.get("segmentName"));
              String desc = String.valueOf(seg.get("description"));

              @SuppressWarnings("unchecked")
              Map<String, Object> segOverrides =
                  (Map<String, Object>) seg.getOrDefault("overrides", Map.of());

              Map<String, SegmentFlagOverride> overrides = new HashMap<>();
              globalFlags.forEach(
                  (flagKey, flagData) -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) flagData;
                    boolean enabled = Boolean.TRUE.equals(data.get("enabled"));
                    overrides.put(
                        flagKey, new SegmentFlagOverride(flagKey, enabled, data.get("value")));
                  });

              segOverrides.forEach(
                  (flagKey, overrideData) -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) overrideData;
                    boolean enabled = Boolean.TRUE.equals(data.get("enabled"));
                    overrides.put(
                        flagKey, new SegmentFlagOverride(flagKey, enabled, data.get("value")));
                  });

              long segmentId = segmentKeyToId.getOrDefault(segKey, 0L);
              return new SegmentEvaluation(segmentId, segName, desc, overrides);
            })
        .toList();
  }

  @Override
  public FeatureFlagUpdateResponse upsertSegmentOverride(
      Long segmentId, String flagName, UpdateFeatureFlagRequest updateFeatureFlagRequest) {

    String segmentKey = resolveSegmentKey(segmentId);
    int priority = segmentAdminService.getSegment(segmentKey).getPriority();

    boolean enabled =
        updateFeatureFlagRequest.enabled() != null ? updateFeatureFlagRequest.enabled() : false;

    var override =
        featureFlagAdminService.upsertSegmentOverride(
            flagName,
            segmentKey,
            enabled,
            Optional.ofNullable(updateFeatureFlagRequest.value()),
            priority,
            currentActorId());

    return new FeatureFlagUpdateResponse(
        flagName, override.isEnabled(), override.getValue().orElse(null));
  }

  @Override
  public void deleteSegmentOverride(Long segmentId, String flagName) {
    String segmentKey = resolveSegmentKey(segmentId);
    featureFlagAdminService.deleteSegmentOverride(flagName, segmentKey, currentActorId());
  }

  // --- Team endpoints ---

  @Override
  public Map<String, Object> getTeamFlags(TeamIdentifier teamIdentifier) {
    Team team =
        teamRepository
            .findByIdentifierForBackoffice(teamIdentifier)
            .orElseThrow(() -> new NotFoundException("Team not found"));

    // Start with global flag defaults
    Map<String, Object> result = new HashMap<>(featureFlagService.getAllEnvironmentFlags());

    // Overlay TEAM-scope overrides
    for (FeatureFlagOverride override : overrideRepo.findTeamScopeOverrides(team.getId())) {
      Map<String, Object> flagData = new HashMap<>();
      flagData.put("enabled", override.isEnabled());
      flagData.put("value", override.getValue().orElse(null));
      result.put(override.getFlagKey(), flagData);
    }

    return result;
  }

  @Override
  public FeatureFlagUpdateResponse upsertTeamOverride(
      TeamIdentifier teamIdentifier,
      String flagName,
      UpdateFeatureFlagRequest updateFeatureFlagRequest) {

    Team team =
        teamRepository
            .findByIdentifierForBackoffice(teamIdentifier)
            .orElseThrow(() -> new NotFoundException("Team not found"));

    boolean enabled =
        updateFeatureFlagRequest.enabled() != null ? updateFeatureFlagRequest.enabled() : false;

    var override =
        featureFlagAdminService.upsertTeamOverride(
            flagName,
            team.getId(),
            enabled,
            Optional.ofNullable(updateFeatureFlagRequest.value()),
            currentActorId());

    return new FeatureFlagUpdateResponse(
        flagName, override.isEnabled(), override.getValue().orElse(null));
  }

  @Override
  public void deleteTeamOverride(TeamIdentifier teamIdentifier, String flagName) {
    Team team =
        teamRepository
            .findByIdentifierForBackoffice(teamIdentifier)
            .orElseThrow(() -> new NotFoundException("Team not found"));

    featureFlagAdminService.deleteTeamOverride(flagName, team.getId(), currentActorId());
  }

  // --- Internal ---

  private UUID currentActorId() {
    return UUID.fromString(SecurityUtils.getBackofficePrincipal().getKeycloakId());
  }

  private String resolveSegmentKey(Long segmentId) {
    List<SegmentDefinition> segments = segmentAdminService.listSegments();
    if (segmentId >= 0 && segmentId < segments.size()) {
      return segments.get(segmentId.intValue()).getKey();
    }
    throw new NotFoundException("Unknown segment ID: " + segmentId);
  }

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
