package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.mapper.LeaseClauseTemplateRecordMapperImpl;

@DisplayName("LeaseClauseTemplateRepository")
class LeaseClauseTemplateRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private LeaseClauseTemplateRepository repository;

  @BeforeEach
  void setUp() {
    repository =
        new LeaseClauseTemplateRepository(dsl, new LeaseClauseTemplateRecordMapperImpl(), CLOCK);
  }

  private LeaseClauseTemplate newTemplate(String country, String key, int sortOrder) {
    return LeaseClauseTemplate.builder()
        .countryCode(country)
        .clauseKey(key)
        .titleI18nKey("lease." + key + ".title")
        .bodyI18nKey("lease." + key + ".body")
        .defaultIncluded(true)
        .optional(false)
        .sortOrder(sortOrder)
        .version(1)
        .build();
  }

  @Test
  @DisplayName("saves and finds templates by country, ordered by sort_order")
  void findsByCountryOrdered() {
    repository.save(newTemplate("NL", "rent", 2));
    repository.save(newTemplate("NL", "parties", 1));
    repository.save(newTemplate("DE", "parties", 1));

    var nlTemplates = repository.findByCountryCode("NL");
    assertThat(nlTemplates).hasSize(2);
    assertThat(nlTemplates)
        .extracting(LeaseClauseTemplate::getClauseKey)
        .containsExactly("parties", "rent");
  }

  @Test
  @DisplayName("soft-deleted templates are excluded from findByCountryCode")
  void excludesSoftDeleted() {
    LeaseClauseTemplate saved = repository.save(newTemplate("FR", "parties", 1));
    repository.softDeleteByIdentifier(saved.getIdentifier().orElseThrow());

    assertThat(repository.findByCountryCode("FR")).isEmpty();
  }
}
