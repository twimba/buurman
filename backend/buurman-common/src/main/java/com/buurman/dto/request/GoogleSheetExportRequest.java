package com.buurman.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for every Google Sheets export endpoint. Carries the user-supplied OAuth 2.0 access
 * token (scope drive.file). The backend uses the token only for the duration of the request and
 * never persists it.
 *
 * <p>The {@code accessToken} field is annotated WRITE_ONLY so Jackson never serializes it back — if
 * this DTO were ever included in a response or error payload, the token would not leak. {@link
 * #toString()} is also overridden to return a redacted form for logs.
 */
public class GoogleSheetExportRequest {

  @NotBlank @JsonProperty(access = Access.WRITE_ONLY)
  private String accessToken = "";

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  @Override
  public String toString() {
    return "GoogleSheetExportRequest[accessToken=REDACTED]";
  }
}
