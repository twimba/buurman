package com.buurman.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * Adds standard error responses (RFC 7807 ProblemDetail) to all API operations.
 *
 * <p>Adds 400/404/409/500 to all operations, plus 401/403 to secured (non-public) endpoints. Never
 * overwrites explicitly declared {@code @ApiResponse} annotations.
 */
@Component
public class OpenApiErrorResponseCustomizer implements GlobalOpenApiCustomizer {

  private static final String PROBLEM_DETAIL_REF = "#/components/schemas/ProblemDetail";

  private static final Set<String> PUBLIC_PATH_PREFIXES =
      Set.of(
          "/health",
          "/info",
          "/reference",
          "/auth/register",
          "/registration-invitations/validate",
          "/registration/config",
          "/invitations/",
          "/calendar/ical/",
          "/webhooks/",
          "/api-docs",
          "/swagger-ui");

  @Override
  @SuppressWarnings("unchecked")
  public void customise(OpenAPI openApi) {
    addProblemDetailSchema(openApi);

    if (openApi.getPaths() == null) {
      return;
    }

    for (Map.Entry<String, PathItem> pathEntry : openApi.getPaths().entrySet()) {
      String path = pathEntry.getKey();
      boolean isPublic = isPublicPath(path);

      for (Operation operation : getOperations(pathEntry.getValue())) {
        if (operation.getResponses() == null) {
          operation.setResponses(new ApiResponses());
        }

        addIfAbsent(operation, "400", "Bad request - validation error or malformed input");
        addIfAbsent(operation, "404", "Resource not found");
        addIfAbsent(operation, "409", "Conflict - business rule violation");
        addIfAbsent(operation, "500", "Internal server error");

        if (!isPublic) {
          addIfAbsent(operation, "401", "Unauthorized - missing or invalid JWT token");
          addIfAbsent(operation, "403", "Forbidden - insufficient permissions");
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void addProblemDetailSchema(OpenAPI openApi) {
    if (openApi.getComponents() == null) {
      openApi.setComponents(new io.swagger.v3.oas.models.Components());
    }
    if (openApi.getComponents().getSchemas() == null) {
      openApi.getComponents().setSchemas(new java.util.LinkedHashMap<>());
    }

    // Only add if not already present (Spring may auto-register ProblemDetail)
    if (openApi.getComponents().getSchemas().containsKey("ProblemDetail")) {
      return;
    }

    Schema<Object> problemDetail = new Schema<>();
    problemDetail.setType("object");
    problemDetail.setDescription("RFC 7807 Problem Detail response");
    problemDetail.setProperties(
        Map.of(
            "type", new StringSchema().description("Problem type URI"),
            "title", new StringSchema().description("Short human-readable summary"),
            "status", new Schema<>().type("integer").description("HTTP status code").example(400),
            "detail", new StringSchema().description("Human-readable explanation of the problem"),
            "instance",
                new StringSchema().description("URI of the request that caused the error")));

    openApi.getComponents().getSchemas().put("ProblemDetail", problemDetail);
  }

  private void addIfAbsent(Operation operation, String statusCode, String description) {
    if (operation.getResponses().get(statusCode) != null) {
      return;
    }

    ApiResponse response =
        new ApiResponse()
            .description(description)
            .content(
                new Content()
                    .addMediaType(
                        "application/json",
                        new MediaType().schema(new Schema<>().$ref(PROBLEM_DETAIL_REF))));

    operation.getResponses().addApiResponse(statusCode, response);
  }

  private boolean isPublicPath(String path) {
    for (String prefix : PUBLIC_PATH_PREFIXES) {
      if (path.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private List<Operation> getOperations(PathItem pathItem) {
    return pathItem.readOperations();
  }
}
