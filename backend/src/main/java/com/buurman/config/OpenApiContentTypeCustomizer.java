package com.buurman.config;

import java.util.Map;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

/**
 * Replaces {@code *}{@code /*} content types with {@code application/json} for responses that
 * reference named schemas (DTOs). This fixes Orval codegen interpreting untyped responses as Blob.
 *
 * <p>Only converts responses where the schema has a {@code $ref} or is an array of {@code $ref}
 * items — i.e., clearly typed DTO responses. Binary endpoints (downloads, exports) with raw {@code
 * string}/{@code object} schemas are left untouched.
 */
@Component
public class OpenApiContentTypeCustomizer implements GlobalOpenApiCustomizer {

  private static final String WILDCARD = "*/*";
  private static final String JSON = "application/json";

  @Override
  public void customise(OpenAPI openApi) {
    if (openApi.getPaths() == null) {
      return;
    }

    for (Map.Entry<String, PathItem> pathEntry : openApi.getPaths().entrySet()) {
      for (Operation operation : pathEntry.getValue().readOperations()) {
        if (operation.getResponses() == null) {
          continue;
        }
        for (ApiResponse response : operation.getResponses().values()) {
          replaceWildcardIfDto(response);
        }
      }
    }
  }

  private void replaceWildcardIfDto(ApiResponse response) {
    Content content = response.getContent();
    if (content == null || !content.containsKey(WILDCARD)) {
      return;
    }

    MediaType wildcardMedia = content.get(WILDCARD);
    if (wildcardMedia == null || !isDtoSchema(wildcardMedia.getSchema())) {
      return;
    }

    content.remove(WILDCARD);
    content.addMediaType(JSON, wildcardMedia);
  }

  @SuppressWarnings("rawtypes")
  private boolean isDtoSchema(Schema schema) {
    if (schema == null) {
      return false;
    }

    // Direct $ref to a named schema (e.g., PropertyResponse)
    if (schema.get$ref() != null) {
      return true;
    }

    // Array of $ref items (e.g., List<TenantResponse>)
    if ("array".equals(schema.getType()) && schema.getItems() != null) {
      return schema.getItems().get$ref() != null;
    }

    return false;
  }
}
