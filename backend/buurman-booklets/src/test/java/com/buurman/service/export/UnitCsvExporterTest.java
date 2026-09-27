package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.UnitResidentialDetailsRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.service.export.tabular.CsvRenderer;
import com.buurman.service.export.tabular.UnitTabularExportBuilder;

@DisplayName("UnitCsvExporter")
@ExtendWith(MockitoExtension.class)
class UnitCsvExporterTest {

  @Mock private UnitResidentialDetailsRepository residentialDetailsRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private ContractPartyService contractPartyService;

  private UnitCsvExporter exporter;

  @BeforeEach
  void setUp() {
    ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
    ms.setBasenames("classpath:messages/test-enum-labels");
    ms.setDefaultEncoding("UTF-8");
    ms.setUseCodeAsDefaultMessage(true);

    when(residentialDetailsRepository.findByUnitIdAndTeamId(any(), any()))
        .thenReturn(Optional.empty());
    when(contractRepository.findActiveByUnitId(any(), any())).thenReturn(Optional.empty());

    UnitTabularExportBuilder builder =
        new UnitTabularExportBuilder(
            residentialDetailsRepository,
            contractRepository,
            contractPartyService,
            new EnumLabelResolver(ms),
            new BookletFormatter());
    exporter = new UnitCsvExporter(builder, new CsvRenderer());
  }

  @Test
  @DisplayName("writes one row per unit with translated type and status")
  void writesUnitRows() {
    byte[] csv =
        exporter.export(
            List.of(
                unit("1", UnitType.APARTMENT, UnitStatus.OCCUPIED, new BigDecimal("85.50")),
                unit("2", UnitType.PARKING, UnitStatus.VACANT, null)),
            Locale.ENGLISH);

    String text = new String(csv, StandardCharsets.UTF_8);
    assertThat(text).contains("Apartment").contains("Parking space");
    assertThat(text).contains("85.50");
    // A unit with no area must render an empty cell, never "null".
    assertThat(text).doesNotContain("null");
  }

  private Unit unit(
      String unitNumber, UnitType type, UnitStatus status, @Nullable BigDecimal area) {
    Unit unit = new Unit();
    unit.setId(UUID.randomUUID());
    unit.setTeamId(UUID.randomUUID());
    unit.setPropertyId(UUID.randomUUID());
    unit.setUnitNumber(unitNumber);
    unit.setUnitType(type);
    unit.setStatus(status);
    unit.setAreaValue(Optional.ofNullable(area));
    return unit;
  }
}
