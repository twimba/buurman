package com.buurman.util;

public enum EntityPrefix {
    AMN("AMN", "Amenities"),
    CPI("CPI", "Contract Payment Instructions"),
    CAL("CAL", "Calendar Feeds"),
    CON("CON", "Contracts"),
    DOC("DOC", "Documents"),
    EXP("EXP", "Expenses"),
    GRP("GRP", "Generated Reports"),
    PRE("PRE", "Payment Receivals"),
    PAY("PAY", "Payments"),
    PIN("PIN", "Payment Instructions"),
    PHO("PHO", "Photos"),
    PRO("PRO", "Properties"),
    POA("POA", "Property Outdoor Areas"),
    TEA("TEA", "Teams"),
    TAD("TAD", "Tenant Addresses"),
    TEN("TEN", "Tenants"),
    USR("USR", "Users");

    private final String code;
    private final String entityName;

    EntityPrefix(String code, String entityName) {
        this.code = code;
        this.entityName = entityName;
    }

    public String getCode() {
        return code;
    }

    public String getEntityName() {
        return entityName;
    }
}
