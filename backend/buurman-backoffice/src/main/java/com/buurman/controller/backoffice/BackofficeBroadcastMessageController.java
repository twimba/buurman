package com.buurman.controller.backoffice;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.BroadcastMessageIdentifier;
import com.buurman.dto.request.backoffice.CreateBroadcastMessageRequest;
import com.buurman.dto.request.backoffice.UpdateBroadcastMessageRequest;
import com.buurman.dto.response.backoffice.BackofficeBroadcastMessageResponse;
import com.buurman.generated.backoffice.api.BackofficeBroadcastsApi;
import com.buurman.security.BackofficePrincipal;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.BackofficeBroadcastMessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeBroadcastMessageController implements BackofficeBroadcastsApi {

  private final BackofficeBroadcastMessageService backofficeBroadcastMessageService;

  @Override
  public List<BackofficeBroadcastMessageResponse> listBroadcastMessages() {
    return backofficeBroadcastMessageService.list();
  }

  @Override
  public BackofficeBroadcastMessageResponse createBroadcastMessage(
      CreateBroadcastMessageRequest request) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return backofficeBroadcastMessageService.create(request, principal);
  }

  @Override
  public BackofficeBroadcastMessageResponse updateBroadcastMessage(
      BroadcastMessageIdentifier identifier, UpdateBroadcastMessageRequest request) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return backofficeBroadcastMessageService.update(identifier, request, principal);
  }

  @Override
  public void deleteBroadcastMessage(BroadcastMessageIdentifier identifier) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    backofficeBroadcastMessageService.delete(identifier, principal);
  }
}
