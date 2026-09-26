package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;

@DisplayName("PropertySummaryAssembler")
@ExtendWith(MockitoExtension.class)
class PropertySummaryAssemblerTest {

  private static final UUID TEAM = UUID.randomUUID();
  private static final PropertyIdentifier ID = PropertyIdentifier.of("P-001");

  @Mock private PropertyRepository propertyRepository;
  @Mock private ContractRepository contractRepository;

  @Test
  @DisplayName("throws — status/area/energy/residential details moved to units (BUUR-106 Task 12)")
  void assembleThrowsUntilUnitJoinExists() {
    ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
    ms.setBasenames("classpath:messages/test-enum-labels");
    ms.setDefaultEncoding("UTF-8");
    ms.setUseCodeAsDefaultMessage(true);
    PropertySummaryAssembler assembler =
        new PropertySummaryAssembler(
            propertyRepository,
            contractRepository,
            new BookletFormatter(),
            new EnumLabelResolver(ms),
            new QrCodeGenerator(),
            Clock.fixed(Instant.parse("2026-06-28T00:00:00Z"), ZoneOffset.UTC),
            "https://app.buurman.io");

    assertThatThrownBy(() -> assembler.assemble(ID, TEAM, Locale.ENGLISH))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
