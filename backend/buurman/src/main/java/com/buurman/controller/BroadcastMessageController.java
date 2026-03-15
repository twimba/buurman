package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.BroadcastMessageIdentifier;
import com.buurman.dto.response.BroadcastMessageResponse;
import com.buurman.generated.api.BroadcastsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.BroadcastMessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BroadcastMessageController implements BroadcastsApi {

  private final BroadcastMessageService broadcastMessageService;

  @Override
  public List<BroadcastMessageResponse> getActiveBroadcasts() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return broadcastMessageService.getActiveMessages(principal.getUserId(), principal.getTeamId());
  }

  @Override
  public List<BroadcastMessageResponse> getPublicBroadcasts(String context) {
    return broadcastMessageService.getPublicMessages(context);
  }

  @Override
  public void dismissBroadcast(BroadcastMessageIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    broadcastMessageService.dismiss(principal.getUserId(), identifier);
  }
}
