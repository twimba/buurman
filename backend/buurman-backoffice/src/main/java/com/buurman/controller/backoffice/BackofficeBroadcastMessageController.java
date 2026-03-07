package com.buurman.controller.backoffice;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.BroadcastMessageIdentifier;
import com.buurman.dto.request.backoffice.CreateBroadcastMessageRequest;
import com.buurman.dto.request.backoffice.UpdateBroadcastMessageRequest;
import com.buurman.dto.response.backoffice.BackofficeBroadcastMessageResponse;
import com.buurman.security.BackofficePrincipal;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.BackofficeBroadcastMessageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeBroadcastMessageController {

  private final BackofficeBroadcastMessageService backofficeBroadcastMessageService;

  @GetMapping("/backoffice/broadcasts")
  public List<BackofficeBroadcastMessageResponse> list() {
    return backofficeBroadcastMessageService.list();
  }

  @PostMapping("/backoffice/broadcasts")
  @ResponseStatus(HttpStatus.CREATED)
  public BackofficeBroadcastMessageResponse create(
      @Valid @RequestBody CreateBroadcastMessageRequest request) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return backofficeBroadcastMessageService.create(request, principal);
  }

  @PutMapping("/backoffice/broadcasts/{identifier}")
  public BackofficeBroadcastMessageResponse update(
      @PathVariable BroadcastMessageIdentifier identifier,
      @Valid @RequestBody UpdateBroadcastMessageRequest request) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return backofficeBroadcastMessageService.update(identifier, request, principal);
  }

  @DeleteMapping("/backoffice/broadcasts/{identifier}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable BroadcastMessageIdentifier identifier) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    backofficeBroadcastMessageService.delete(identifier, principal);
  }
}
