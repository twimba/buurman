package com.buurman.domain;

/**
 * Kind of separately-lettable object within a property. ROOM is deliberately absent: room rentals
 * and HMO cost splitting are a separate follow-up issue (see the BUUR-106 spec, Out of scope).
 */
public enum UnitType {
  APARTMENT,
  PARKING,
  STORAGE,
  COMMERCIAL
}
