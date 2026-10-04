package com.buurman.controller.backoffice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.LeaseKind;
import com.buurman.domain.identifier.LeaseClauseTemplateIdentifier;
import com.buurman.dto.request.backoffice.LeaseAgreementPreviewRequest;
import com.buurman.dto.request.backoffice.UpsertLeaseClauseTemplateRequest;
import com.buurman.dto.response.LeaseClauseTemplateResponse;
import com.buurman.dto.response.backoffice.LeaseAgreementPreviewResponse;
import com.buurman.generated.backoffice.api.BackofficeLeaseClauseTemplatesApi;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.BackofficeLeaseClauseTemplateService;
import com.buurman.service.letters.LeasePreviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeLeaseClauseTemplateController implements BackofficeLeaseClauseTemplatesApi {

  private final BackofficeLeaseClauseTemplateService leaseClauseTemplateService;
  private final LeasePreviewService leasePreviewService;

  @Override
  public List<LeaseClauseTemplateResponse> listLeaseClauseTemplates(
      String countryCode, Optional<LeaseKind> leaseKind, Optional<String> language) {
    return leaseClauseTemplateService.list(countryCode, leaseKind, language);
  }

  @Override
  public LeaseClauseTemplateResponse createLeaseClauseTemplate(
      UpsertLeaseClauseTemplateRequest upsertLeaseClauseTemplateRequest) {
    return leaseClauseTemplateService.create(upsertLeaseClauseTemplateRequest, currentActorId());
  }

  @Override
  public LeaseClauseTemplateResponse updateLeaseClauseTemplate(
      LeaseClauseTemplateIdentifier identifier,
      UpsertLeaseClauseTemplateRequest upsertLeaseClauseTemplateRequest) {
    return leaseClauseTemplateService.update(
        identifier, upsertLeaseClauseTemplateRequest, currentActorId());
  }

  @Override
  public void deleteLeaseClauseTemplate(LeaseClauseTemplateIdentifier identifier) {
    leaseClauseTemplateService.delete(identifier, currentActorId());
  }

  @Override
  public LeaseAgreementPreviewResponse previewLeaseAgreement(
      LeaseAgreementPreviewRequest leaseAgreementPreviewRequest) {
    return leasePreviewService.preview(leaseAgreementPreviewRequest);
  }

  private UUID currentActorId() {
    return UUID.fromString(SecurityUtils.getBackofficePrincipal().getKeycloakId());
  }
}
