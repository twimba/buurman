package com.buurman.dto.response;

/** Result of a successful Google Sheets export. */
public record GoogleSheetExportResponse(String spreadsheetId, String url) {}
