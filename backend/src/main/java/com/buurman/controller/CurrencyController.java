package com.buurman.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.TeamIdentifier;
import com.buurman.domain.metadata.CountryMetadataRegistry;
import com.buurman.dto.request.CurrencyChangeRequest;
import com.buurman.dto.response.CountryCurrencyResponse;
import com.buurman.dto.response.CurrencyChangeResponse;
import com.buurman.generated.api.CurrencyApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.CurrencyChangeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CurrencyController implements CurrencyApi {

  private final CurrencyChangeService currencyChangeService;

  @Override
  public Map<String, String> getCountryCurrencies() {
    return CountryMetadataRegistry.getCountryCurrencies();
  }

  @Override
  public CountryCurrencyResponse getCurrencyForCountry(String code) {
    String currency = CountryMetadataRegistry.getDefaultCurrency(code);
    return new CountryCurrencyResponse(code.toUpperCase(), currency);
  }

  @Override
  public CurrencyChangeResponse changeCurrency(
      TeamIdentifier teamIdentifier, CurrencyChangeRequest currencyChangeRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return currencyChangeService.changeCurrency(
        principal.requireTeamId(), currencyChangeRequest, principal.getUserId());
  }
}
