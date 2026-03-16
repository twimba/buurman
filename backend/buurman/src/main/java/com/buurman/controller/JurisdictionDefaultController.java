package com.buurman.controller;

import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.JurisdictionDefaultResponse;
import com.buurman.service.JurisdictionDefaultService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/jurisdiction-defaults")
@RequiredArgsConstructor
public class JurisdictionDefaultController {

  private final JurisdictionDefaultService jurisdictionDefaultService;

  @GetMapping
  public JurisdictionDefaultResponse getDefaults(
      @RequestParam String countryCode,
      @RequestParam(required = false) String regionCode,
      @RequestParam(required = false) String landlordType,
      @RequestParam(required = false) Boolean furnished) {
    return jurisdictionDefaultService.getDefaults(
        countryCode,
        Optional.ofNullable(regionCode),
        Optional.ofNullable(landlordType),
        Optional.ofNullable(furnished));
  }
}
