package com.buurman.config.jooq;

import org.jooq.impl.AbstractConverter;

import com.buurman.domain.Ulid;

/** JOOQ converter for automatic {@code VARCHAR} ↔ {@link Ulid} conversion on identifier columns. */
public class UlidJooqConverter extends AbstractConverter<String, Ulid> {

  public UlidJooqConverter() {
    super(String.class, Ulid.class);
  }

  @Override
  public Ulid from(String databaseObject) {
    return databaseObject == null ? null : Ulid.of(databaseObject);
  }

  @Override
  public String to(Ulid userObject) {
    return userObject == null ? null : userObject.value();
  }
}
