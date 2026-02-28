package com.buurman.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of entity tracked in audit logs")
public enum AuditEntityType {
  PROPERTY,
  TENANT,
  CONTRACT,
  CONTRACT_PARTY,
  EXPENSE,
  PAYMENT,
  TEAM,
  USER
}
