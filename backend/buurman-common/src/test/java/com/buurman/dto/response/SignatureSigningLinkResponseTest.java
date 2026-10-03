package com.buurman.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.buurman.domain.SignatureSignerRole;
import com.buurman.domain.SignatureSignerStatus;

class SignatureSigningLinkResponseTest {

  @Test
  void toStringNeverPrintsTheSigningUrl() {
    var response =
        new SignatureSigningLinkResponse(
            "Lena",
            "l@example.com",
            SignatureSignerRole.LANDLORD,
            SignatureSignerStatus.PENDING,
            Optional.of("https://sign.example.com/sign/tok_secret"),
            false);

    assertThat(response.toString()).doesNotContain("tok_secret").doesNotContain("https://");
  }
}
