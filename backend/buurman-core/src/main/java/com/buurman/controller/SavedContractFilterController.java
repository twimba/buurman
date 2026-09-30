package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.SavedContractFilterIdentifier;
import com.buurman.dto.request.CreateSavedContractFilterRequest;
import com.buurman.dto.response.SavedContractFilterResponse;
import com.buurman.generated.api.SavedContractFiltersApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.SavedContractFilterService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class SavedContractFilterController implements SavedContractFiltersApi {

  private final SavedContractFilterService service;

  @Override
  public List<SavedContractFilterResponse> getSavedContractFilters() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return service.list(principal);
  }

  @Override
  public SavedContractFilterResponse createSavedContractFilter(
      CreateSavedContractFilterRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return service.create(request, principal);
  }

  @Override
  public void deleteSavedContractFilter(SavedContractFilterIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    service.delete(identifier, principal);
  }
}
