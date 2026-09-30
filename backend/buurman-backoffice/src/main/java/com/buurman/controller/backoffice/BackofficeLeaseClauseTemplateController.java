package com.buurman.controller.backoffice;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.LeaseClauseTemplateIdentifier;
import com.buurman.dto.request.backoffice.UpsertLeaseClauseTemplateRequest;
import com.buurman.dto.response.LeaseClauseTemplateResponse;
import com.buurman.generated.backoffice.api.BackofficeLeaseClauseTemplatesApi;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.BackofficeLeaseClauseTemplateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeLeaseClauseTemplateController implements BackofficeLeaseClauseTemplatesApi {

  private final BackofficeLeaseClauseTemplateService leaseClauseTemplateService;

  @Override
  public List<LeaseClauseTemplateResponse> listLeaseClauseTemplates(String countryCode) {
    return leaseClauseTemplateService.list(countryCode);
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

  private UUID currentActorId() {
    return UUID.fromString(SecurityUtils.getBackofficePrincipal().getKeycloakId());
  }
}
