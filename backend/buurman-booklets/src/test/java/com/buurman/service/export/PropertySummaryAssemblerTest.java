package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.identifier.PropertyIdentifier;

@DisplayName("PropertySummaryAssembler")
class PropertySummaryAssemblerTest {

  private static final UUID TEAM = UUID.randomUUID();
  private static final PropertyIdentifier ID = PropertyIdentifier.of("P-001");

  @Test
  @DisplayName("throws — status/area/energy/residential details moved to units (BUUR-106 Task 12)")
  void assembleThrowsUntilUnitJoinExists() {
    PropertySummaryAssembler assembler = new PropertySummaryAssembler();

    assertThatThrownBy(() -> assembler.assemble(ID, TEAM, Locale.ENGLISH))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
