package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.util.PaginationHelper.PaginatedResult;

@DisplayName("PaginationHelper")
class PaginationHelperTest {

  private final Field<?> nameField = DSL.field("name", String.class);
  private final Field<?> idField = DSL.field("id", Long.class);
  private final Map<String, Field<?>> sortableFields = Map.of("name", nameField, "id", idField);

  @SuppressWarnings("unchecked")
  private final Table<Record> table = mock(Table.class);

  private final Condition condition = mock(Condition.class);

  @Nested
  @DisplayName("paginate")
  class Paginate {

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("returns items and totalElements")
    void returnsItemsAndTotal() {
      DSLContext dsl = mock(DSLContext.class, Answers.RETURNS_DEEP_STUBS);
      when(dsl.fetchCount(table, condition)).thenReturn(42);
      when(dsl.selectFrom(table)
              .where(condition)
              .orderBy(any(org.jooq.OrderField.class))
              .limit(10)
              .offset(0)
              .fetch()
              .map(any(org.jooq.RecordMapper.class)))
          .thenReturn(List.of("Alice", "Bob", "Charlie"));

      PaginatedResult<String> result =
          PaginationHelper.paginate(
              dsl, table, condition, sortableFields, idField, PageRequest.of(0, 10, null, (SortDirection) null), r -> "mapped");

      assertThat(result.totalElements()).isEqualTo(42);
      assertThat(result.items()).containsExactly("Alice", "Bob", "Charlie");
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("uses specified sort field when matching key exists")
    void usesSpecifiedSortField() {
      DSLContext dsl = mock(DSLContext.class, Answers.RETURNS_DEEP_STUBS);
      when(dsl.fetchCount(table, condition)).thenReturn(5);
      when(dsl.selectFrom(table)
              .where(condition)
              .orderBy(any(org.jooq.OrderField.class))
              .limit(10)
              .offset(0)
              .fetch()
              .map(any(org.jooq.RecordMapper.class)))
          .thenReturn(List.of("item"));

      PaginatedResult<String> result =
          PaginationHelper.paginate(
              dsl, table, condition, sortableFields, idField, PageRequest.of(0, 10, "name", SortDirection.ASC), r -> "mapped");

      assertThat(result.totalElements()).isEqualTo(5);
      assertThat(result.items()).containsExactly("item");
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("falls back to default sort when key not in sortableFields")
    void fallsBackToDefaultForUnknownSort() {
      DSLContext dsl = mock(DSLContext.class, Answers.RETURNS_DEEP_STUBS);
      when(dsl.fetchCount(table, condition)).thenReturn(3);
      when(dsl.selectFrom(table)
              .where(condition)
              .orderBy(any(org.jooq.OrderField.class))
              .limit(10)
              .offset(0)
              .fetch()
              .map(any(org.jooq.RecordMapper.class)))
          .thenReturn(List.of("a", "b", "c"));

      PaginatedResult<String> result =
          PaginationHelper.paginate(
              dsl, table, condition, sortableFields, idField, PageRequest.of(0, 10, "unknown_field", SortDirection.ASC), r -> "mapped");

      assertThat(result.items()).hasSize(3);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("defaults to DESC when no direction specified")
    void defaultsToDesc() {
      DSLContext dsl = mock(DSLContext.class, Answers.RETURNS_DEEP_STUBS);
      when(dsl.fetchCount(table, condition)).thenReturn(1);
      when(dsl.selectFrom(table)
              .where(condition)
              .orderBy(any(org.jooq.OrderField.class))
              .limit(10)
              .offset(0)
              .fetch()
              .map(any(org.jooq.RecordMapper.class)))
          .thenReturn(List.of("item"));

      PaginatedResult<String> result =
          PaginationHelper.paginate(
              dsl, table, condition, sortableFields, idField, PageRequest.of(0, 10, "name", (SortDirection) null), r -> "mapped");

      assertThat(result.items()).hasSize(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("page 2 with size 25 produces offset 50")
    void correctOffset() {
      DSLContext dsl = mock(DSLContext.class, Answers.RETURNS_DEEP_STUBS);
      when(dsl.fetchCount(table, condition)).thenReturn(100);
      when(dsl.selectFrom(table)
              .where(condition)
              .orderBy(any(org.jooq.OrderField.class))
              .limit(25)
              .offset(50)
              .fetch()
              .map(any(org.jooq.RecordMapper.class)))
          .thenReturn(List.of());

      PaginatedResult<String> result =
          PaginationHelper.paginate(
              dsl, table, condition, sortableFields, idField, PageRequest.of(2, 25, null, (SortDirection) null), r -> "mapped");

      assertThat(result.totalElements()).isEqualTo(100);
      assertThat(result.items()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("empty result set returns empty items with correct total")
    void emptyResultSet() {
      DSLContext dsl = mock(DSLContext.class, Answers.RETURNS_DEEP_STUBS);
      when(dsl.fetchCount(table, condition)).thenReturn(10);
      when(dsl.selectFrom(table)
              .where(condition)
              .orderBy(any(org.jooq.OrderField.class))
              .limit(25)
              .offset(125)
              .fetch()
              .map(any(org.jooq.RecordMapper.class)))
          .thenReturn(List.of());

      PaginatedResult<String> result =
          PaginationHelper.paginate(
              dsl, table, condition, sortableFields, idField, PageRequest.of(5, 25, null, (SortDirection) null), r -> "mapped");

      assertThat(result.items()).isEmpty();
      assertThat(result.totalElements()).isEqualTo(10);
    }
  }

  @Nested
  @DisplayName("PaginatedResult")
  class PaginatedResultTests {

    @Test
    @DisplayName("record accessors return correct values")
    void accessors() {
      PaginatedResult<String> result = new PaginatedResult<>(List.of("a", "b"), 5);

      assertThat(result.items()).containsExactly("a", "b");
      assertThat(result.totalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("empty items with zero total")
    void emptyWithZero() {
      PaginatedResult<String> result = new PaginatedResult<>(List.of(), 0);

      assertThat(result.items()).isEmpty();
      assertThat(result.totalElements()).isZero();
    }
  }
}
