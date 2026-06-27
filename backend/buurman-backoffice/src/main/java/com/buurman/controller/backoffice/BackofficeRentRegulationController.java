package com.buurman.controller.backoffice;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.RentRegulationRuleIdentifier;
import com.buurman.domain.regulation.RentRegulationCatalog;
import com.buurman.dto.request.BulkCreateRentRegulationRulesRequest;
import com.buurman.dto.request.CreateRentRegulationCountryRequest;
import com.buurman.dto.request.CreateRentRegulationRegionRequest;
import com.buurman.dto.request.CreateRentRegulationRuleRequest;
import com.buurman.dto.request.UpdateRentRegulationCountryRequest;
import com.buurman.dto.request.UpdateRentRegulationRegionRequest;
import com.buurman.dto.request.UpdateRentRegulationRuleRequest;
import com.buurman.dto.response.BulkImportResult;
import com.buurman.dto.response.CountryRegulationRequestSummary;
import com.buurman.dto.response.RentRegulationCatalogDiff;
import com.buurman.dto.response.RentRegulationCatalogInfo;
import com.buurman.dto.response.RentRegulationCountryResponse;
import com.buurman.dto.response.RentRegulationRegionResponse;
import com.buurman.dto.response.RentRegulationReloadResult;
import com.buurman.dto.response.RentRegulationRuleResponse;
import com.buurman.generated.backoffice.api.BackofficeRentRegulationsApi;
import com.buurman.security.BackofficePrincipal;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.BackofficeRentRegulationService;
import com.buurman.service.backoffice.RentRegulationCatalogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeRentRegulationController implements BackofficeRentRegulationsApi {

  private final BackofficeRentRegulationService backofficeRentRegulationService;
  private final RentRegulationCatalogService rentRegulationCatalogService;

  @Override
  public RentRegulationCatalogInfo getRentRegulationCatalogInfo() {
    return rentRegulationCatalogService.catalogInfo();
  }

  @Override
  public RentRegulationCatalogDiff diffRentRegulationCatalog() {
    return rentRegulationCatalogService.diff();
  }

  @Override
  public RentRegulationReloadResult reloadRentRegulationCatalog() {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return rentRegulationCatalogService.reload(principal);
  }

  @Override
  public RentRegulationCatalog exportRentRegulationCatalog() {
    return rentRegulationCatalogService.exportCurrent();
  }

  @Override
  public List<RentRegulationCountryResponse> listBackofficeRentRegulationCountries() {
    return backofficeRentRegulationService.listCountries();
  }

  @Override
  @ResponseStatus(HttpStatus.CREATED)
  public RentRegulationCountryResponse createRentRegulationCountry(
      CreateRentRegulationCountryRequest createRentRegulationCountryRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return backofficeRentRegulationService.createCountry(
        createRentRegulationCountryRequest, principal);
  }

  @Override
  public RentRegulationCountryResponse updateRentRegulationCountry(
      String code, UpdateRentRegulationCountryRequest updateRentRegulationCountryRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return backofficeRentRegulationService.updateCountry(
        code, updateRentRegulationCountryRequest, principal);
  }

  @Override
  public void deleteRentRegulationCountry(String code) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    backofficeRentRegulationService.deleteCountry(code, principal);
  }

  @Override
  public void reviewRentRegulationCountry(String code) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    backofficeRentRegulationService.reviewCountry(code, principal);
  }

  @Override
  public List<RentRegulationRegionResponse> listRentRegulationRegions(String code) {
    return backofficeRentRegulationService.listRegions(code);
  }

  @Override
  @ResponseStatus(HttpStatus.CREATED)
  public RentRegulationRegionResponse createRentRegulationRegion(
      String code, CreateRentRegulationRegionRequest createRentRegulationRegionRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return backofficeRentRegulationService.createRegion(
        code, createRentRegulationRegionRequest, principal);
  }

  @Override
  public RentRegulationRegionResponse updateRentRegulationRegion(
      String code,
      String regionCode,
      UpdateRentRegulationRegionRequest updateRentRegulationRegionRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return backofficeRentRegulationService.updateRegion(
        code, regionCode, updateRentRegulationRegionRequest, principal);
  }

  @Override
  public void deleteRentRegulationRegion(String code, String regionCode) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    backofficeRentRegulationService.deleteRegion(code, regionCode, principal);
  }

  @Override
  public List<RentRegulationRuleResponse> listRentRegulationRules(String code) {
    return backofficeRentRegulationService.listRules(code);
  }

  @Override
  @ResponseStatus(HttpStatus.CREATED)
  public RentRegulationRuleResponse createRentRegulationRule(
      String code, CreateRentRegulationRuleRequest createRentRegulationRuleRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return backofficeRentRegulationService.createRule(
        code, createRentRegulationRuleRequest, principal);
  }

  @Override
  public BulkImportResult bulkImportRentRegulationRules(
      String code, BulkCreateRentRegulationRulesRequest bulkCreateRentRegulationRulesRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return backofficeRentRegulationService.bulkImportRules(
        code, bulkCreateRentRegulationRulesRequest, principal);
  }

  @Override
  public RentRegulationRuleResponse updateRentRegulationRule(
      RentRegulationRuleIdentifier identifier,
      UpdateRentRegulationRuleRequest updateRentRegulationRuleRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    return backofficeRentRegulationService.updateRule(
        identifier, updateRentRegulationRuleRequest, principal);
  }

  @Override
  public void deleteRentRegulationRule(RentRegulationRuleIdentifier identifier) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    backofficeRentRegulationService.deleteRule(identifier, principal);
  }

  @Override
  public List<CountryRegulationRequestSummary> listCountryRegulationRequests() {
    return backofficeRentRegulationService.listCountryRequests();
  }

  @Override
  public void dismissCountryRegulationRequest(String countryName) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    backofficeRentRegulationService.dismissCountryRequest(countryName, principal);
  }
}
