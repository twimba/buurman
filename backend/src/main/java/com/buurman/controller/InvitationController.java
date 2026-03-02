package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.InvitationResponse;
import com.buurman.generated.api.InvitationsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.TeamService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class InvitationController implements InvitationsApi {

  private final TeamService teamService;

  @Override
  public List<InvitationResponse> getPendingInvitations() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return teamService.getPendingInvitationsForUser(principal);
  }

  @Override
  public InvitationResponse getInvitation(String token) {
    return teamService.getInvitation(token);
  }

  @Override
  public void acceptInvitation(String token) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    teamService.acceptInvitation(token, principal);
  }
}
