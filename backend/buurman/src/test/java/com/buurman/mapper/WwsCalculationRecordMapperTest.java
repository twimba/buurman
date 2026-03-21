package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.JSONB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Sid;
import com.buurman.domain.WwsCalculation;
import com.buurman.domain.WwsCalculation.CategoryBreakdown;
import com.buurman.jooq.generated.tables.records.WwsCalculationsRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("WwsCalculationRecordMapper")
class WwsCalculationRecordMapperTest {

  @Mock private ObjectMapper objectMapper;
  @InjectMocks private WwsCalculationRecordMapper mapper;

  private static final UUID ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("WWS01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<WwsCalculation> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() throws Exception {
      WwsCalculationsRecord record = createCompleteRecord();
      List<CategoryBreakdown> breakdown =
          List.of(
              new CategoryBreakdown(
                  "area", "Area", "Oppervlakte", new BigDecimal("44.00"), "Based on 44m2"));
      when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(breakdown);

      Optional<WwsCalculation> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      WwsCalculation calc = result.get();
      assertThat(calc.getId()).isEqualTo(ID);
      assertThat(calc.getIdentifier()).contains(IDENTIFIER);
      assertThat(calc.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(calc.getPropertyId()).isEqualTo(PROPERTY_ID);
      assertThat(calc.getContractId()).contains(CONTRACT_ID);
      assertThat(calc.getSystemVersion()).isEqualTo("2026-v1");
      assertThat(calc.getTotalPoints()).isEqualByComparingTo(new BigDecimal("144.00"));
      assertThat(calc.getSectorClassification()).isEqualTo("FREE");
      assertThat(calc.getMaxRentIndication()).contains(new BigDecimal("1200.00"));
      assertThat(calc.getCategoryBreakdown()).hasSize(1);
      assertThat(calc.getCategoryBreakdown().get(0).key()).isEqualTo("area");
      assertThat(calc.getInputDataJson()).isEqualTo("{\"rooms\":3}");
      assertThat(calc.getCalculationDate()).isEqualTo(LocalDate.of(2026, 1, 15));
      assertThat(calc.getNotes()).contains("Test calculation");
      assertThat(calc.getCreatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(calc.getUpdatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(calc.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("handles breakdown deserialization failure gracefully")
    void handlesBreakdownDeserializationFailure() throws Exception {
      WwsCalculationsRecord record = createCompleteRecord();
      when(objectMapper.readValue(anyString(), any(TypeReference.class)))
          .thenThrow(new RuntimeException("parse error"));

      Optional<WwsCalculation> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getCategoryBreakdown()).isEmpty();
    }

    @Test
    @DisplayName("wraps nullable fields as empty Optional when null")
    void wrapsNullableFieldsAsEmpty() throws Exception {
      WwsCalculationsRecord record = createCompleteRecord();
      record.setContractId(null);
      record.setMaxRentIndication(null);
      record.setNotes(null);
      when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of());

      Optional<WwsCalculation> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      WwsCalculation calc = result.get();
      assertThat(calc.getContractId()).isEmpty();
      assertThat(calc.getMaxRentIndication()).isEmpty();
      assertThat(calc.getNotes()).isEmpty();
    }

    @Test
    @DisplayName("maps deletedAt when present")
    void mapsDeletedAt() throws Exception {
      WwsCalculationsRecord record = createCompleteRecord();
      LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      record.setDeletedAt(deletedAt);
      when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of());

      Optional<WwsCalculation> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDeletedAt()).contains(deletedAt.toInstant(ZoneOffset.UTC));
    }
  }

  private WwsCalculationsRecord createCompleteRecord() {
    WwsCalculationsRecord record = new WwsCalculationsRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setTeamId(TEAM_ID);
    record.setPropertyId(PROPERTY_ID);
    record.setContractId(CONTRACT_ID);
    record.setSystemVersion("2026-v1");
    record.setTotalPoints(new BigDecimal("144.00"));
    record.setSectorClassification("FREE");
    record.setMaxRentIndication(new BigDecimal("1200.00"));
    record.setCategoryBreakdown(JSONB.jsonb("[{\"key\":\"area\"}]"));
    record.setInputData(JSONB.jsonb("{\"rooms\":3}"));
    record.setCalculationDate(LocalDate.of(2026, 1, 15));
    record.setNotes("Test calculation");
    record.setCreatedAt(NOW);
    record.setUpdatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    record.setUpdatedBy(UPDATED_BY);
    record.setDeletedAt(null);
    return record;
  }
}
