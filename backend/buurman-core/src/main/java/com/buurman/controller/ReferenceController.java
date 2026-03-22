package com.buurman.controller;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.CurrencyInfo;
import com.buurman.dto.response.EntityPrefixInfo;
import com.buurman.generated.api.ReferenceApi;
import com.buurman.util.EntityPrefix;

@RestController
public class ReferenceController implements ReferenceApi {

  @Override
  public List<EntityPrefixInfo> getEntityPrefixes() {
    return Arrays.stream(EntityPrefix.values())
        .map(p -> new EntityPrefixInfo(p.getCode(), p.getEntityName()))
        .toList();
  }

  @Override
  public List<CurrencyInfo> getCurrencies() {
    Set<Currency> currencies = Currency.getAvailableCurrencies();
    return currencies.stream()
        .filter(c -> c.getDefaultFractionDigits() >= 0)
        .sorted(Comparator.comparing(Currency::getCurrencyCode))
        .map(
            c ->
                new CurrencyInfo(
                    c.getCurrencyCode(),
                    c.getDisplayName(Locale.ENGLISH),
                    c.getSymbol(Locale.ENGLISH),
                    c.getDefaultFractionDigits()))
        .toList();
  }
}
