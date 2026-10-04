package com.buurman.service;

import java.util.List;

import com.buurman.domain.LeaseKind;

/**
 * SPI listing the languages a lease agreement document exists in. {@code buurman-core} cannot
 * depend on {@code buurman-letters}, so this interface lives here and {@code LeaseDocumentLocator}
 * (in {@code buurman-letters}) implements it; Spring wires the implementation in at runtime via the
 * assembled app module. Same pattern as {@link ContractTerminationLetterGenerator}.
 */
public interface LeaseDocumentLanguageCatalog {

  /**
   * Languages a generate request is honoured in for this country and kind (no silent fallback to
   * another language), in preference order: the country's authoritative language first, then its
   * other national languages, English, and any further translations in canonical order. Empty when
   * no document exists.
   */
  List<String> availableLanguages(String countryCode, LeaseKind kind);
}
