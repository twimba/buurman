package com.buurman.config.jooq;

import org.jooq.impl.AbstractConverter;

import com.buurman.domain.Sid;

/** JOOQ converter for automatic {@code VARCHAR} ↔ {@link Sid} conversion on identifier columns. */
public class SidJooqConverter extends AbstractConverter<String, Sid> {

  public SidJooqConverter() {
    super(String.class, Sid.class);
  }

  @Override
  public Sid from(String databaseObject) {
    return databaseObject == null ? null : Sid.of(databaseObject);
  }

  @Override
  public String to(Sid userObject) {
    return userObject == null ? null : userObject.value();
  }
}
