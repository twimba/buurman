package com.buurman.controller;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.util.EntityPrefix;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/reference")
@Tag(name = "Reference", description = "Technical reference data for integrators and developers")
public class ReferenceController {

  public record EntityPrefixInfo(String prefix, String entity) {}

  public record CurrencyInfo(String code, String name, String symbol, int fractionalDigits) {}

  @Operation(
      summary = "Entity identifier prefixes",
      description =
          "Returns the 3-character prefix codes used in entity identifiers (format:"
              + " <prefix><ULID>)")
  @GetMapping("/entity-prefixes")
  public List<EntityPrefixInfo> getEntityPrefixes() {
    return Arrays.stream(EntityPrefix.values())
        .map(p -> new EntityPrefixInfo(p.getCode(), p.getEntityName()))
        .toList();
  }

  @Operation(
      summary = "Available currencies",
      description =
          "Returns all ISO 4217 currencies with their fractional digit count. "
              + "Use fractionalDigits to determine the input step for monetary amounts.")
  @GetMapping("/currencies")
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
