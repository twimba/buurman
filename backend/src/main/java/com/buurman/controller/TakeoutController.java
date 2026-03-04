package com.buurman.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.DataTakeoutIdentifier;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.TakeoutResponse;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.TakeoutService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/takeouts")
@RequiredArgsConstructor
public class TakeoutController {

  private final TakeoutService takeoutService;

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public TakeoutResponse requestTakeout() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return takeoutService.requestTakeout(
        principal.requireTeamId(), principal.requireTeamIdentifier(), principal.getUserId());
  }

  @GetMapping
  public PageResponse<TakeoutResponse> listTakeouts(
      @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return takeoutService.listTakeouts(
        principal.requireTeamId(), PageRequest.of(page, size, null, (String) null));
  }

  @GetMapping("/{identifier}")
  public TakeoutResponse getTakeout(@PathVariable DataTakeoutIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return takeoutService.getTakeout(identifier, principal.requireTeamId());
  }

  @DeleteMapping("/{identifier}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteTakeout(@PathVariable DataTakeoutIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    takeoutService.deleteTakeout(identifier, principal.requireTeamId());
  }
}
