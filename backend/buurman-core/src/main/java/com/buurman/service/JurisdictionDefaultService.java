package com.buurman.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.buurman.dto.response.JurisdictionDefaultResponse;
import com.buurman.repository.JurisdictionDefaultRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JurisdictionDefaultService {

  private static final String DISCLAIMER =
      "These are suggested defaults based on general legislation. "
          + "They are not legal advice. Consult a local legal professional.";

  private final JurisdictionDefaultRepository repository;

  public JurisdictionDefaultResponse getDefaults(
      String countryCode,
      Optional<String> regionCode,
      Optional<String> landlordType,
      Optional<Boolean> furnished) {
    Map<String, String> defaults =
        repository.findDefaults(countryCode, regionCode, landlordType, furnished);
    return new JurisdictionDefaultResponse(countryCode, defaults, DISCLAIMER);
  }
}
