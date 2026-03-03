package com.buurman.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.BroadcastMessageResponse;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.BroadcastMessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BroadcastMessageController {

  private final BroadcastMessageService broadcastMessageService;

  @GetMapping("/broadcasts")
  public List<BroadcastMessageResponse> getActive() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return broadcastMessageService.getActiveMessages(principal.getUserId(), principal.getTeamId());
  }

  @GetMapping("/broadcasts/public")
  public List<BroadcastMessageResponse> getPublic(@RequestParam String context) {
    return broadcastMessageService.getPublicMessages(context);
  }

  @PostMapping("/broadcasts/{identifier}/dismiss")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void dismiss(@PathVariable String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    broadcastMessageService.dismiss(principal.getUserId(), identifier);
  }
}
