package com.buurman.util;

import lombok.Getter;

@Getter
public enum EntityPrefix {
  AMN("AMN", "Amenities"),
  BCM("BCM", "Broadcast Messages"),
  CPI("CPI", "Contract Payment Instructions"),
  CTP("CTP", "Contract Parties"),
  CAL("CAL", "Calendar Feeds"),
  CON("CON", "Contracts"),
  DII("DII", "Data Import Items"),
  DIM("DIM", "Data Imports"),
  DOC("DOC", "Documents"),
  EXP("EXP", "Expenses"),
  GRP("GRP", "Generated Reports"),
  NTF("NTF", "Notifications"),
  PRE("PRE", "Payment Receivals"),
  PRM("PRM", "Payment Reminders"),
  CCR("CCR", "Contact Credits"),
  DEP("DEP", "Deposits"),
  DDD("DDD", "Deposit Deductions"),
  PPL("PPL", "Payment Plans"),
  PAY("PAY", "Payments"),
  PIN("PIN", "Payment Instructions"),
  PHO("PHO", "Photos"),
  PRO("PRO", "Properties"),
  RIN("RIN", "Registration Invitations"),
  OCP("OCP", "Property Occupancy Periods"),
  POA("POA", "Property Outdoor Areas"),
  TEA("TEA", "Teams"),
  CAD("CAD", "Contact Addresses"),
  CTC("CTC", "Contacts"),
  CNT("CNT", "Contact Notes"),
  CRL("CRL", "Contact Relationships"),
  CRP("CRP", "Contract Rent Periods"),
  USR("USR", "Users"),
  ACQ("ACQ", "Property Acquisitions"),
  VAL("VAL", "Property Valuations"),
  FIN("FIN", "Property Financings"),
  FPY("FPY", "Financing Payments"),
  INS("INS", "Property Insurances"),
  PTX("PTX", "Property Taxes"),
  FEE("FEE", "Property Fees"),
  TKO("TKO", "Data Takeouts"),
  WWS("WWS", "WWS Calculations"),
  RRC("RRC", "Rent Regulation Countries"),
  RRG("RRG", "Rent Regulation Regions"),
  RRL("RRL", "Rent Regulation Rules"),
  RRT("RRT", "Rent Regulation Tenancy Rules"),
  CRQ("CRQ", "Country Regulation Requests"),
  IMS("IMS", "Impersonation Sessions"),
  CEX("CEX", "Contract Extensions"),
  RCO("RCO", "Rent Components");

  private final String code;
  private final String entityName;

  EntityPrefix(String code, String entityName) {
    this.code = code;
    this.entityName = entityName;
  }
}
