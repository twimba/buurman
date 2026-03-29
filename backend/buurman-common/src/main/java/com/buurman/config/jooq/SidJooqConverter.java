package com.buurman.config.jooq;

import org.jooq.impl.AbstractConverter;
import org.jspecify.annotations.Nullable;

import com.buurman.domain.Sid;

/** JOOQ converter for automatic {@code VARCHAR} ↔ {@link Sid} conversion on identifier columns. */
public class SidJooqConverter extends AbstractConverter<String, Sid> {

  public SidJooqConverter() {
    super(String.class, Sid.class);
  }

  @Override
  public @Nullable Sid from(@Nullable String databaseObject) {
    return databaseObject == null ? null : Sid.of(databaseObject);
  }

  @Override
  public @Nullable String to(@Nullable Sid userObject) {
    return userObject == null ? null : userObject.value();
  }
}
