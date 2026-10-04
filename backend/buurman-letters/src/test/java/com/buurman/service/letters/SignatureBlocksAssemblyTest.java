package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LetterExporterHelper.assembleSignatureBlocks")
class SignatureBlocksAssemblyTest {

  @Test
  @DisplayName(
      "landlord first, tenants numbered from 1 with the placeholders DocumensoClient matches")
  void placeholdersAreStable() {
    assertThat(LetterExporterHelper.assembleSignatureBlocks("Landlord", List.of("Ann", "Bob")))
        .containsExactly(
            Map.of("label", "Landlord", "placeholder", "signature-landlord"),
            Map.of("label", "Ann", "placeholder", "signature-tenant-1"),
            Map.of("label", "Bob", "placeholder", "signature-tenant-2"));
  }

  @Test
  @DisplayName("without tenants only the landlord block remains")
  void noTenants() {
    assertThat(LetterExporterHelper.assembleSignatureBlocks("Landlord", List.of()))
        .containsExactly(Map.of("label", "Landlord", "placeholder", "signature-landlord"));
  }
}
