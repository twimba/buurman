package com.buurman.controller;

import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.JurisdictionDefaultResponse;
import com.buurman.service.JurisdictionDefaultService;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/jurisdiction-defaults")
@RequiredArgsConstructor
@Validated
public class JurisdictionDefaultController {

  private final JurisdictionDefaultService jurisdictionDefaultService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public JurisdictionDefaultResponse getDefaults(
      @RequestParam @Size(min = 2, max = 2) @Pattern(regexp = "[A-Z]{2}") String countryCode,
      @RequestParam Optional<String> regionCode,
      @RequestParam Optional<String> landlordType,
      @RequestParam Optional<Boolean> furnished) {
    return jurisdictionDefaultService.getDefaults(countryCode, regionCode, landlordType, furnished);
  }
}
