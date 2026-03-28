package com.buurman.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum SegmentAttribute {
  IS_DEMO,
  IS_OWNER,
  ROLE,
  EMAIL,
  EMAIL_VERIFIED,
  PROPERTY_COUNT,
  MEMBER_COUNT,
  TEAM_AGE_DAYS,
  CONTRACT_COUNT,
  CONTACT_COUNT,
  PHOTO_COUNT,
  DOCUMENT_COUNT,
  EXPENSE_COUNT,
  PAYMENT_COUNT,
  CALENDAR_FEED_COUNT,
  IS_TEAM_SCOPE("is_team"),
  IS_USER_SCOPE("is_user");

  private static final Map<String, SegmentAttribute> BY_DB_VALUE =
      Arrays.stream(values())
          .collect(Collectors.toMap(SegmentAttribute::toDbValue, Function.identity()));

  private final String dbValue;

  SegmentAttribute() {
    this.dbValue = name().toLowerCase(Locale.ROOT);
  }

  SegmentAttribute(String dbValue) {
    this.dbValue = dbValue;
  }

  public String toDbValue() {
    return dbValue;
  }

  public static SegmentAttribute fromDbValue(String value) {
    SegmentAttribute attr = BY_DB_VALUE.get(value);
    if (attr != null) {
      return attr;
    }
    return valueOf(value.toUpperCase(Locale.ROOT));
  }
}
