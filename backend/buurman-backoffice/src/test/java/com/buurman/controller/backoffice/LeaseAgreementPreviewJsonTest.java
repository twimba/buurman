package com.buurman.controller.backoffice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Contract;
import com.buurman.domain.LeaseAvailability;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.RentComponentType;
import com.buurman.dto.request.backoffice.LeaseAgreementPreviewRequest;
import com.buurman.dto.response.backoffice.LeaseAgreementPreviewResponse;

import tools.jackson.databind.json.JsonMapper;

@DisplayName("lease preview JSON contract")
class LeaseAgreementPreviewJsonTest {

  private final JsonMapper mapper = JsonMapper.builder().build();

  @Test
  @DisplayName("the request body of openapi/backoffice.yaml deserializes into the hand-written DTO")
  void requestDeserializes() {
    String json =
        """
        {"countryCode":"NL","leaseKind":"RESIDENTIAL","language":"nl",
         "sample":{"contractType":"FIXED_TERM","startDate":"2026-03-01","endDate":"2027-02-28",
           "rentComponents":[{"type":"BASE_RENT","amount":1250.00,"currency":"EUR"}],
           "deposit":{"amount":2500,"currency":"EUR"},"paymentDueDay":1,
           "paymentFrequency":"MONTHLY","landlordName":"L","tenantNames":["T"],
           "propertyAddress":"A"},
         "clauses":[{"clauseKey":"rent","included":true,"sortOrder":3}]}
        """;

    LeaseAgreementPreviewRequest request =
        mapper.readValue(json, LeaseAgreementPreviewRequest.class);

    assertThat(request.leaseKind()).isEqualTo(LeaseKind.RESIDENTIAL);
    assertThat(request.sample().contractType()).isEqualTo(Contract.ContractType.FIXED_TERM);
    assertThat(request.sample().startDate()).isEqualTo(LocalDate.of(2026, 3, 1));
    assertThat(request.sample().rentComponents().get(0).type())
        .isEqualTo(RentComponentType.BASE_RENT);
    assertThat(request.sample().rentComponents().get(0).amount())
        .isEqualByComparingTo(new BigDecimal("1250"));
    assertThat(request.sample().deposit())
        .extracting(LeaseAgreementPreviewRequest.Money::currency)
        .isEqualTo("EUR");
    assertThat(request.clauses()).hasSize(1);
  }

  @Test
  @DisplayName("an unavailable response serializes with absent optional fields")
  void unavailableResponseSerializes() {
    String json =
        mapper.writeValueAsString(
            new LeaseAgreementPreviewResponse(
                LeaseAvailability.UNAVAILABLE_COUNTRY,
                null,
                null,
                null,
                null,
                java.util.List.of()));

    assertThat(json)
        .contains("\"availability\":\"UNAVAILABLE_COUNTRY\"")
        .contains("\"clauses\":[]")
        .contains("\"html\":null")
        .contains("\"languageUsed\":null")
        .contains("\"kindUsed\":null")
        .contains("\"source\":null");
  }
}
