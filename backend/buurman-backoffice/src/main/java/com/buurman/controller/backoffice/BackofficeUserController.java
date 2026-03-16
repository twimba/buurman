package com.buurman.controller.backoffice;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.TeamIdentifier;
import com.buurman.domain.identifier.UserIdentifier;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeUserDetailResponse;
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
      Optional<String> search,
      Optional<String> team,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sort,
      Optional<String> direction) {
    PageRequest pageRequest =
        PageRequest.of(
            page.orElse(null), size.orElse(null), sort.orElse(null), direction.orElse(null));
    List<TeamIdentifier> teamIdentifiers =
        team.filter(t -> !t.isBlank())
            .map(
                t ->
                    Arrays.stream(t.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(TeamIdentifier::of)
                        .toList())
            .orElse(List.of());
    return backofficeUserService.listUsers(pageRequest, search.orElse(null), teamIdentifiers);
  }

  @Override
  public BackofficeUserDetailResponse getUser(UserIdentifier identifier) {
    return backofficeUserService.getUser(identifier);
  }

  @Override
  public void disableUser(UserIdentifier identifier) {
    backofficeUserService.disableUser(identifier, SecurityUtils.getBackofficePrincipal());
  }

  @Override
  public void enableUser(UserIdentifier identifier) {
    backofficeUserService.enableUser(identifier, SecurityUtils.getBackofficePrincipal());
  }

  @Override
  public void resetPassword(UserIdentifier identifier) {
    backofficeUserService.resetPassword(identifier, SecurityUtils.getBackofficePrincipal());
  }
}
