package com.buurman.controller.backoffice;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.TeamIdentifier;
import com.buurman.domain.identifier.UserIdentifier;
import com.buurman.dto.request.backoffice.UpdateFeatureFlagRequest;
import com.buurman.dto.response.FeatureFlagState;
import com.buurman.dto.response.backoffice.FeatureFlagAdminStatusResponse;
import com.buurman.dto.response.backoffice.FeatureFlagUpdateResponse;
import com.buurman.dto.response.backoffice.SegmentEvaluation;
import com.buurman.dto.response.backoffice.TeamFlagEvaluation;
import com.buurman.generated.backoffice.api.BackofficeFeatureFlagsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.BackofficeFeatureFlagService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeFeatureFlagController implements BackofficeFeatureFlagsApi {

  private final BackofficeFeatureFlagService featureFlagService;

  @Override
  public FeatureFlagAdminStatusResponse getAdminStatus() {
    return new FeatureFlagAdminStatusResponse(true, "builtin");
  }

  @Override
  public Map<String, FeatureFlagState> getGlobalFlags() {
    return featureFlagService.getGlobalFlags();
  }

  @Override
  public List<TeamFlagEvaluation> getUserFlags(UserIdentifier userIdentifier) {
    return featureFlagService.getUserFlags(userIdentifier);
  }

  @Override
  public FeatureFlagUpdateResponse updateGlobalFlag(
      String flagName, UpdateFeatureFlagRequest request) {
    return featureFlagService.updateGlobalFlag(flagName, request, currentActorId());
  }

  @Override
  public FeatureFlagUpdateResponse upsertIdentityOverride(
      UserIdentifier userIdentifier,
      TeamIdentifier teamIdentifier,
      String flagName,
      UpdateFeatureFlagRequest request) {
    return featureFlagService.upsertIdentityOverride(
        userIdentifier, teamIdentifier, flagName, request, currentActorId());
  }

  @Override
  public void deleteIdentityOverride(
      UserIdentifier userIdentifier, TeamIdentifier teamIdentifier, String flagName) {
    featureFlagService.deleteIdentityOverride(
        userIdentifier, teamIdentifier, flagName, currentActorId());
  }

  @Override
  public List<SegmentEvaluation> getSegmentOverrides() {
    return featureFlagService.getSegmentOverrides();
  }

  @Override
  public FeatureFlagUpdateResponse upsertSegmentOverride(
      Long segmentId, String flagName, UpdateFeatureFlagRequest request) {
    return featureFlagService.upsertSegmentOverride(segmentId, flagName, request, currentActorId());
  }

  @Override
  public void deleteSegmentOverride(Long segmentId, String flagName) {
    featureFlagService.deleteSegmentOverride(segmentId, flagName, currentActorId());
  }

  @Override
  public Map<String, FeatureFlagState> getTeamFlags(TeamIdentifier teamIdentifier) {
    return featureFlagService.getTeamFlags(teamIdentifier);
  }

  @Override
  public FeatureFlagUpdateResponse upsertTeamOverride(
      TeamIdentifier teamIdentifier, String flagName, UpdateFeatureFlagRequest request) {
    return featureFlagService.upsertTeamOverride(
        teamIdentifier, flagName, request, currentActorId());
  }

  @Override
  public void deleteTeamOverride(TeamIdentifier teamIdentifier, String flagName) {
    featureFlagService.deleteTeamOverride(teamIdentifier, flagName, currentActorId());
  }

  private UUID currentActorId() {
    return UUID.fromString(SecurityUtils.getBackofficePrincipal().getKeycloakId());
  }
}
