package com.buurman.util;

import static com.buurman.domain.SortDirection.ASC;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.Table;

import com.buurman.dto.request.PageRequest;

public final class PaginationHelper {

  private PaginationHelper() {}

  public static <R extends org.jooq.Record, T> PaginatedResult<T> paginate(
      DSLContext dsl,
      Table<R> table,
      Condition condition,
      Map<String, Field<?>> sortableFields,
      Field<?> defaultSort,
      PageRequest pageRequest,
      Function<R, T> recordMapper) {

    Field<?> sortField =
        (pageRequest.sort() != null && sortableFields.containsKey(pageRequest.sort()))
            ? sortableFields.get(pageRequest.sort())
            : defaultSort;

    SortField<?> orderBy = pageRequest.direction() == ASC ? sortField.asc() : sortField.desc();

    long totalElements = dsl.fetchCount(table, condition);

    List<T> items =
        dsl.selectFrom(table)
            .where(condition)
            .orderBy(orderBy)
            .limit(pageRequest.size())
            .offset(pageRequest.offset())
            .fetch()
            .map(recordMapper::apply);

    return new PaginatedResult<>(items, totalElements);
  }

  public record PaginatedResult<T>(List<T> items, long totalElements) {}
}
