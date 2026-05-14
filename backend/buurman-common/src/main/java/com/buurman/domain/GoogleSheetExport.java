package com.buurman.domain;

/** Result of a successful Google Sheets export: the spreadsheet id and its web-edit URL. */
public record GoogleSheetExport(String spreadsheetId, String url) {}
