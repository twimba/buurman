package com.buurman.controller;

import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.DataTakeoutIdentifier;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.TakeoutResponse;
import com.buurman.generated.api.DataTakeoutApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.TakeoutService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TakeoutController implements DataTakeoutApi {

  private final TakeoutService takeoutService;

  @Override
  public TakeoutResponse requestTakeout() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return takeoutService.requestTakeout(
        principal.requireTeamId(), principal.requireTeamIdentifier(), principal.getUserId());
  }

  @Override
  public PageResponse listTakeouts(Optional<Integer> page, Optional<Integer> size) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return takeoutService.listTakeouts(
        principal.requireTeamId(),
        PageRequest.of(page.orElse(null), size.orElse(null), null, (String) null));
  }

  @Override
  public TakeoutResponse getTakeout(DataTakeoutIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return takeoutService.getTakeout(identifier, principal.requireTeamId());
  }

  @Override
  public void deleteTakeout(DataTakeoutIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    takeoutService.deleteTakeout(identifier, principal.requireTeamId());
  }
}
