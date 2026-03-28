package com.buurman.controller.backoffice;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.SegmentDefinition;
import com.buurman.generated.backoffice.api.BackofficeSegmentsApi;
import com.buurman.generated.backoffice.model.CreateSegmentRequest;
import com.buurman.generated.backoffice.model.SegmentConditionResponse;
import com.buurman.generated.backoffice.model.SegmentDetailResponse;
import com.buurman.generated.backoffice.model.SegmentMatchesResponse;
import com.buurman.generated.backoffice.model.SegmentMatchingTeam;
import com.buurman.generated.backoffice.model.SegmentMatchingUser;
import com.buurman.generated.backoffice.model.UpdateSegmentRequest;
import com.buurman.security.SecurityUtils;
import com.buurman.service.SegmentAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeSegmentController implements BackofficeSegmentsApi {

  private final SegmentAdminService segmentAdminService;

  @Override
  public List<SegmentDetailResponse> listSegments() {
    return segmentAdminService.listSegments().stream().map(this::toResponse).toList();
  }

  @Override
  public SegmentDetailResponse getSegment(String segmentKey) {
    return toResponse(segmentAdminService.getSegment(segmentKey));
  }

  @Override
  @ResponseStatus(HttpStatus.CREATED)
  public SegmentDetailResponse createSegment(CreateSegmentRequest request) {
    List<SegmentAdminService.ConditionInput> conditions = List.of();
    if (request.getConditions() != null) {
      conditions =
          request.getConditions().stream()
              .map(
                  c ->
                      new SegmentAdminService.ConditionInput(
                          c.getAttribute().getValue(), c.getOperator().getValue(), c.getValue()))
              .toList();
    }

    SegmentDefinition created =
        segmentAdminService.createSegment(
            request.getKey(),
            request.getName(),
            request.getDescription(),
            request.getPriority() != null ? request.getPriority() : 0,
            conditions,
            currentActorId());

    return toResponse(created);
  }

  @Override
  public SegmentDetailResponse updateSegment(String segmentKey, UpdateSegmentRequest request) {
    List<SegmentAdminService.ConditionInput> conditions = List.of();
    if (request.getConditions() != null) {
      conditions =
          request.getConditions().stream()
              .map(
                  c ->
                      new SegmentAdminService.ConditionInput(
                          c.getAttribute().getValue(), c.getOperator().getValue(), c.getValue()))
              .toList();
    }

    SegmentDefinition updated =
        segmentAdminService.updateSegment(
            segmentKey,
            request.getName(),
            request.getDescription(),
            request.getPriority() != null ? request.getPriority() : 0,
            conditions,
            currentActorId());

    return toResponse(updated);
  }

  @Override
  public void deleteSegment(String segmentKey) {
    segmentAdminService.deleteSegment(segmentKey, currentActorId());
  }

  @Override
  public SegmentMatchesResponse getSegmentMatches(String segmentKey) {
    long teamCount = segmentAdminService.countMatchingTeams(segmentKey);
    long userCount = segmentAdminService.countMatchingUsers(segmentKey);
    SegmentMatchesResponse response = new SegmentMatchesResponse();
    response.setMatchingTeamCount(teamCount);
    response.setMatchingUserCount(userCount);
    return response;
  }

  @Override
  public List<SegmentMatchingTeam> getMatchingTeams(String segmentKey) {
    return segmentAdminService.findMatchingTeams(segmentKey).stream()
        .map(
            t -> {
              SegmentMatchingTeam r = new SegmentMatchingTeam();
              r.setIdentifier(t.identifier().toString());
              r.setTeamName(t.teamName());
              r.setDemo(t.demo());
              r.setCreatedAt(t.createdAt());
              return r;
            })
        .toList();
  }

  @Override
  public List<SegmentMatchingUser> getMatchingUsers(String segmentKey) {
    return segmentAdminService.findMatchingUsers(segmentKey).stream()
        .map(
            u -> {
              SegmentMatchingUser r = new SegmentMatchingUser();
              r.setIdentifier(u.identifier().toString());
              r.setEmail(u.email());
              r.setFirstName(u.firstName());
              r.setLastName(u.lastName());
              r.setRole(u.role());
              r.setTeamIdentifier(u.teamIdentifier().toString());
              r.setTeamName(u.teamName());
              return r;
            })
        .toList();
  }

  private SegmentDetailResponse toResponse(SegmentDefinition segment) {
    SegmentDetailResponse response = new SegmentDetailResponse();
    response.setKey(segment.getKey());
    response.setName(segment.getName());
    response.setDescription(segment.getDescription().orElse(null));
    response.setPriority(segment.getPriority());
    response.setConditions(
        segment.getConditions().stream()
            .map(
                c -> {
                  SegmentConditionResponse cr = new SegmentConditionResponse();
                  cr.setAttribute(c.getAttribute().toDbValue());
                  cr.setOperator(c.getOperator().toDbValue());
                  cr.setValue(c.getValue());
                  return cr;
                })
            .toList());
    response.setMatchingTeamCount(0L);
    response.setMatchingUserCount(0L);
    return response;
  }

  private UUID currentActorId() {
    return UUID.fromString(SecurityUtils.getBackofficePrincipal().getKeycloakId());
  }
}
