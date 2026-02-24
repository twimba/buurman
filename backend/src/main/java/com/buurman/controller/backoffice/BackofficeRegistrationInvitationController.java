package com.buurman.controller.backoffice;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.backoffice.CreateRegistrationInvitationRequest;
import com.buurman.dto.request.backoffice.SendRegistrationInvitationRequest;
import com.buurman.dto.request.backoffice.UpdateRegistrationInvitationNoteRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.RegistrationInvitationDetailResponse;
import com.buurman.dto.response.backoffice.RegistrationInvitationResponse;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.RegistrationInvitationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/backoffice/registration-invitations")
@Tag(
    name = "Backoffice - Registration Invitations",
    description = "Manage registration invitation codes")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class BackofficeRegistrationInvitationController {

  private final RegistrationInvitationService invitationService;

  @GetMapping
  @Operation(summary = "List registration invitations")
  public ResponseEntity<PageResponse<RegistrationInvitationResponse>> list(
      @RequestParam Optional<String> search,
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "25") Integer size,
      @RequestParam Optional<String> sort,
      @RequestParam(defaultValue = "DESC") SortDirection direction,
      @AuthenticationPrincipal BackofficePrincipal principal) {

    PageRequest pageRequest = PageRequest.of(page, size, sort.orElse(null), direction);
    return ResponseEntity.ok(invitationService.list(pageRequest, search.orElse(null)));
  }

  @PostMapping
  @Operation(summary = "Create a new registration invitation")
  public ResponseEntity<RegistrationInvitationResponse> create(
      @RequestBody @Valid CreateRegistrationInvitationRequest request,
      @AuthenticationPrincipal BackofficePrincipal principal) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(invitationService.create(request, principal));
  }

  @GetMapping("/{identifier}")
  @Operation(summary = "Get registration invitation details")
  public ResponseEntity<RegistrationInvitationDetailResponse> get(
      @PathVariable String identifier, @AuthenticationPrincipal BackofficePrincipal principal) {
    return ResponseEntity.ok(invitationService.getByIdentifier(identifier));
  }

  @PostMapping("/{identifier}/revoke")
  @Operation(summary = "Revoke a registration invitation")
  public ResponseEntity<Void> revoke(
      @PathVariable String identifier, @AuthenticationPrincipal BackofficePrincipal principal) {
    invitationService.revoke(identifier, principal);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{identifier}/send")
  @Operation(summary = "Send registration invitation via email or SMS")
  public ResponseEntity<Void> send(
      @PathVariable String identifier,
      @RequestBody @Valid SendRegistrationInvitationRequest request,
      @AuthenticationPrincipal BackofficePrincipal principal) {
    invitationService.sendInvitation(identifier, request, principal);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{identifier}/note")
  @Operation(summary = "Update registration invitation note")
  public ResponseEntity<RegistrationInvitationDetailResponse> updateNote(
      @PathVariable String identifier,
      @RequestBody UpdateRegistrationInvitationNoteRequest request,
      @AuthenticationPrincipal BackofficePrincipal principal) {
    return ResponseEntity.ok(invitationService.updateNote(identifier, request, principal));
  }

  @GetMapping("/suggest-code")
  @Operation(summary = "Generate a suggested invitation code")
  public ResponseEntity<Map<String, String>> suggestCode(
      @AuthenticationPrincipal BackofficePrincipal principal) {
    return ResponseEntity.ok(Map.of("code", invitationService.suggestCode()));
  }
}
