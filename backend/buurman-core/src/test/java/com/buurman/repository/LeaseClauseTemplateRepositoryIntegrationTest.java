package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

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
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
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

  /**
   * Verifies the real {@code V083__seed_lease_clause_templates.sql} content — deliberately against
   * a throwaway container it migrates itself, rather than the class's shared {@code dsl}. The
   * shared container's {@code lease_clause_templates} table is truncated by {@code cleanDatabase()}
   * before every test in this suite (including other test classes that share the same static
   * container), so by the time any test method here runs, the seed rows the shared migration step
   * inserted are long gone. Migrating a fresh schema here is what actually proves the seed
   * migration does what it claims.
   */
  @Test
  @DisplayName(
      "migration V083 seeds exactly 7 NL clause templates with the expected optional flags")
  void migrationSeedsNlClauseTemplates() {
    try (PostgreSQLContainer<?> seedContainer =
        new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("buurman_seed_check")
            .withUsername("buurman")
            .withPassword("buurman")) {
      seedContainer.start();

      PGSimpleDataSource dataSource = new PGSimpleDataSource();
      dataSource.setUrl(seedContainer.getJdbcUrl());
      dataSource.setUser(seedContainer.getUsername());
      dataSource.setPassword(seedContainer.getPassword());

      Flyway.configure()
          .dataSource(dataSource)
          .locations("classpath:db/migration")
          .load()
          .migrate();

      DSLContext seedDsl = DSL.using((DataSource) dataSource, SQLDialect.POSTGRES);
      LeaseClauseTemplateRepository seedRepository =
          new LeaseClauseTemplateRepository(
              seedDsl, new LeaseClauseTemplateRecordMapperImpl(), CLOCK);

      List<LeaseClauseTemplate> nlTemplates = seedRepository.findByCountryCode("NL");
      assertThat(nlTemplates).hasSize(7);

      assertThat(nlTemplates)
          .filteredOn(template -> !template.isOptional())
          .extracting(LeaseClauseTemplate::getClauseKey)
          .containsExactlyInAnyOrder("parties", "premises", "rent", "duration");

      assertThat(nlTemplates)
          .filteredOn(LeaseClauseTemplate::isOptional)
          .extracting(LeaseClauseTemplate::getClauseKey)
          .containsExactlyInAnyOrder("deposit", "maintenance", "termination-reference");
    }
  }
}
