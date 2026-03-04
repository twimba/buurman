package com.buurman.controller.backoffice;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.UserIdentifier;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeUserResponse;
import com.buurman.generated.backoffice.api.BackofficeUsersApi;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.BackofficeUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeUserController implements BackofficeUsersApi {

  private final BackofficeUserService backofficeUserService;

  @Override
  public PageResponse<BackofficeUserResponse> listUsers(
      String search, Integer page, Integer size, String sort, String direction) {
    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    return backofficeUserService.listUsers(pageRequest, search);
  }

  @Override
  public BackofficeUserResponse getUser(String identifier) {
    return backofficeUserService.getUser(UserIdentifier.of(identifier));
  }

  @Override
  public void disableUser(String identifier) {
    backofficeUserService.disableUser(UserIdentifier.of(identifier), SecurityUtils.getBackofficePrincipal());
  }

  @Override
  public void enableUser(String identifier) {
    backofficeUserService.enableUser(UserIdentifier.of(identifier), SecurityUtils.getBackofficePrincipal());
  }

  @Override
  public void resetPassword(String identifier) {
    backofficeUserService.resetPassword(UserIdentifier.of(identifier), SecurityUtils.getBackofficePrincipal());
  }
}
