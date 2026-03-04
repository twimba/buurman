package com.buurman.controller.backoffice;

import java.util.Map;

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
      String search, Integer page, Integer size, String sort, String direction) {
    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    return invitationService.list(pageRequest, search);
  }

  @Override
  public RegistrationInvitationResponse create(
      CreateRegistrationInvitationRequest createRegistrationInvitationRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return invitationService.create(createRegistrationInvitationRequest, principal);
  }

  @Override
  public RegistrationInvitationDetailResponse get(String identifier) {
    return invitationService.getByIdentifier(RegistrationInvitationIdentifier.of(identifier));
  }

  @Override
  public void revoke(String identifier) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    invitationService.revoke(RegistrationInvitationIdentifier.of(identifier), principal);
  }

  @Override
  public void send(
      String identifier, SendRegistrationInvitationRequest sendRegistrationInvitationRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    invitationService.sendInvitation(RegistrationInvitationIdentifier.of(identifier), sendRegistrationInvitationRequest, principal);
  }

  @Override
  public RegistrationInvitationDetailResponse updateNote(
      String identifier,
      UpdateRegistrationInvitationNoteRequest updateRegistrationInvitationNoteRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return invitationService.updateNote(
        RegistrationInvitationIdentifier.of(identifier), updateRegistrationInvitationNoteRequest, principal);
  }

  @Override
  public Map<String, String> suggestCode() {
    return Map.of("code", invitationService.suggestCode());
  }
}
