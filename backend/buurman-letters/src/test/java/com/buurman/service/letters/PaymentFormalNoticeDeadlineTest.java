package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Contract;
import com.buurman.domain.RentRegulationCountry;

@DisplayName("formal notice deadline resolution")
class PaymentFormalNoticeDeadlineTest {

  private static Contract contract(Integer days) {
    return Contract.builder().formalNoticeDays(Optional.ofNullable(days)).build();
  }

  private static Optional<RentRegulationCountry> regulation(Integer days) {
    return Optional.of(
        RentRegulationCountry.builder()
            .countryCode("XX")
            .countryName("Testland")
            .formalNoticeDays(Optional.ofNullable(days))
            .build());
  }

  @Test
  @DisplayName("contract override wins over the country default")
  void contractWins() {
    assertThat(PaymentFormalNoticeExporter.resolveDeadlineDays(contract(21), regulation(8)))
        .isEqualTo(21);
  }

  @Test
  @DisplayName("country default applies when the contract has no override")
  void countryDefault() {
    assertThat(PaymentFormalNoticeExporter.resolveDeadlineDays(contract(null), regulation(8)))
        .isEqualTo(8);
  }

  @Test
  @DisplayName("falls back to 14 days without contract or country setting")
  void fallback() {
    assertThat(PaymentFormalNoticeExporter.resolveDeadlineDays(contract(null), regulation(null)))
        .isEqualTo(PaymentFormalNoticeExporter.DEFAULT_DEADLINE_DAYS);
    assertThat(PaymentFormalNoticeExporter.resolveDeadlineDays(contract(null), Optional.empty()))
        .isEqualTo(14);
  }
}
