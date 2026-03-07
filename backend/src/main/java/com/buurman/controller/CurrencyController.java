package com.buurman.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.TeamIdentifier;
import com.buurman.domain.metadata.CountryMetadataRegistry;
import com.buurman.dto.request.CurrencyChangeRequest;
import com.buurman.dto.response.CurrencyChangeResponse;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.CurrencyChangeService;
import com.buurman.service.TeamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class CurrencyController {

  private final CurrencyChangeService currencyChangeService;
  private final TeamService teamService;

  /** Returns a map of ISO 3166-1 country codes to their default ISO 4217 currency codes. */
  @GetMapping("/countries/currencies")
  public Map<String, String> getCountryCurrencies() {
    return CountryMetadataRegistry.getCountryCurrencies();
  }

  /** Returns the default currency for a specific country code. */
  @GetMapping("/countries/{code}/currency")
  public Map<String, String> getCurrencyForCountry(@PathVariable String code) {
    String currency = CountryMetadataRegistry.getDefaultCurrency(code);
    return Map.of("countryCode", code.toUpperCase(), "currency", currency);
  }

  /** Change the team's currency. TEAM_ADMIN only. */
  @PostMapping("/teams/{teamIdentifier}/currency-change")
  public ResponseEntity<CurrencyChangeResponse> changeCurrency(
      @PathVariable TeamIdentifier teamIdentifier,
      @Valid @RequestBody CurrencyChangeRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    CurrencyChangeResponse response =
        currencyChangeService.changeCurrency(
            principal.requireTeamId(), request, principal.getUserId());
    return ResponseEntity.ok(response);
  }
}
