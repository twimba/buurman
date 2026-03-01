package com.buurman.controller.backoffice;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.backoffice.CreateBuurmyRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BuurmyResponse;
import com.buurman.generated.backoffice.api.BackofficeBuurmiesApi;
import com.buurman.security.BackofficePrincipal;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.BackofficeBuurmyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeBuurmyController implements BackofficeBuurmiesApi {

  private final BackofficeBuurmyService buurmyService;

  @Override
  public PageResponse<BuurmyResponse> listBuurmies(
      String search, Integer page, Integer size, String sort, String direction) {
    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    return buurmyService.listBuurmies(pageRequest, search);
  }

  @Override
  public BuurmyResponse getBuurmy(String keycloakId) {
    return buurmyService.getBuurmy(keycloakId);
  }

  @Override
  public BuurmyResponse createBuurmy(CreateBuurmyRequest createBuurmyRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return buurmyService.createBuurmy(createBuurmyRequest, principal);
  }

  @Override
  public void disableBuurmy(String keycloakId) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    buurmyService.disableBuurmy(keycloakId, principal);
  }

  @Override
  public void enableBuurmy(String keycloakId) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    buurmyService.enableBuurmy(keycloakId, principal);
  }

  @Override
  public void deleteBuurmy(String keycloakId) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    buurmyService.deleteBuurmy(keycloakId, principal);
  }

  @Override
  public void forcePasswordUpdate(String keycloakId) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    buurmyService.forcePasswordUpdate(keycloakId, principal);
  }

  @Override
  public void forceProfileUpdate(String keycloakId) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    buurmyService.forceProfileUpdate(keycloakId, principal);
  }

  @Override
  public void removePasswordReset(String keycloakId) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    buurmyService.removePasswordReset(keycloakId, principal);
  }

  @Override
  public void removeProfileReset(String keycloakId) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    buurmyService.removeProfileReset(keycloakId, principal);
  }

  @Override
  public void verifyBuurmy(String keycloakId) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    buurmyService.verifyBuurmy(keycloakId, principal);
  }

  @Override
  public void unverifyBuurmy(String keycloakId) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    buurmyService.unverifyBuurmy(keycloakId, principal);
  }
}
