package com.buurman.controller.backoffice;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.TeamIdentifier;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.backoffice.UpdateTeamNameRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeTeamDetailResponse;
import com.buurman.dto.response.backoffice.BackofficeTeamResponse;
import com.buurman.generated.backoffice.api.BackofficeTeamsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.BackofficeTeamService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeTeamController implements BackofficeTeamsApi {

  private final BackofficeTeamService backofficeTeamService;

  @Override
  public PageResponse<BackofficeTeamResponse> listTeams(
      String search, Integer page, Integer size, String sort, String direction) {
    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    return backofficeTeamService.listTeams(pageRequest, search);
  }

  @Override
  public BackofficeTeamDetailResponse getTeam(TeamIdentifier identifier) {
    return backofficeTeamService.getTeam(identifier);
  }

  @Override
  public BackofficeTeamResponse updateTeamName(
      TeamIdentifier identifier, UpdateTeamNameRequest updateTeamNameRequest) {
    return backofficeTeamService.updateTeamName(
        identifier, updateTeamNameRequest, SecurityUtils.getBackofficePrincipal());
  }

  @Override
  public void deleteTeam(TeamIdentifier identifier) {
    backofficeTeamService.deleteTeam(identifier, SecurityUtils.getBackofficePrincipal());
  }
}
