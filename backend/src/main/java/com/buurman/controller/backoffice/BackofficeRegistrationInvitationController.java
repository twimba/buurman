package com.buurman.controller.backoffice;

import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.RegistrationInvitationIdentifier;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.backoffice.CreateRegistrationInvitationRequest;
import com.buurman.dto.request.backoffice.SendRegistrationInvitationRequest;
import com.buurman.dto.request.backoffice.UpdateRegistrationInvitationNoteRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.RegistrationInvitationDetailResponse;
import com.buurman.dto.response.backoffice.RegistrationInvitationResponse;
import com.buurman.generated.backoffice.api.BackofficeRegistrationInvitationsApi;
import com.buurman.security.BackofficePrincipal;
import com.buurman.security.SecurityUtils;
import com.buurman.service.RegistrationInvitationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeRegistrationInvitationController
    implements BackofficeRegistrationInvitationsApi {

  private final RegistrationInvitationService invitationService;

  @Override
  public PageResponse<RegistrationInvitationResponse> callList(
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sort,
      Optional<String> direction) {
    PageRequest pageRequest =
        PageRequest.of(
            page.orElse(null), size.orElse(null), sort.orElse(null), direction.orElse(null));
    return invitationService.list(pageRequest, search.orElse(null));
  }

  @Override
  public RegistrationInvitationResponse create(
      CreateRegistrationInvitationRequest createRegistrationInvitationRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return invitationService.create(createRegistrationInvitationRequest, principal);
  }

  @Override
  public RegistrationInvitationDetailResponse get(RegistrationInvitationIdentifier identifier) {
    return invitationService.getByIdentifier(identifier);
  }

  @Override
  public void revoke(RegistrationInvitationIdentifier identifier) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    invitationService.revoke(identifier, principal);
  }

  @Override
  public void send(
      RegistrationInvitationIdentifier identifier,
      SendRegistrationInvitationRequest sendRegistrationInvitationRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    invitationService.sendInvitation(identifier, sendRegistrationInvitationRequest, principal);
  }

  @Override
  public RegistrationInvitationDetailResponse updateNote(
      RegistrationInvitationIdentifier identifier,
      UpdateRegistrationInvitationNoteRequest updateRegistrationInvitationNoteRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return invitationService.updateNote(
        identifier, updateRegistrationInvitationNoteRequest, principal);
  }

  @Override
  public Map<String, String> suggestCode() {
    return Map.of("code", invitationService.suggestCode());
  }
}
