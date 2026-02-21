package com.buurman.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamSettings {

  private Boolean demoData;
  private PaymentSettings payments = new PaymentSettings();
  private RegionalSettings regional = new RegionalSettings();

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PaymentSettings {
    private Integer paymentsAheadCount = 3;
    private Boolean autoGenerationEnabled = true;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RegionalSettings {
    private String defaultCurrency;
    private String defaultCountry = "Netherlands";
    private String timezone = "Europe/Amsterdam";
    private String dateFormat = "DD/MM/YYYY";
    private String fiscalYearStartMonth = "01";
  }
}
