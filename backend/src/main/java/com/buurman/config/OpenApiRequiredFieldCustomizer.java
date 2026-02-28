package com.buurman.config;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Marks non-{@link Optional} fields as {@code required} in OpenAPI schemas.
 *
 * <p>In {@code @NullMarked} DTO packages, all record components that are not {@code Optional<T>}
 * are guaranteed non-null and should be marked required. SpringDoc does not infer this
 * automatically for records without validation annotations.
 */
@Component
public class OpenApiRequiredFieldCustomizer implements GlobalOpenApiCustomizer {

  private static final String[] SCAN_PACKAGES = {
    "com.buurman.dto.response", "com.buurman.dto.request",
  };

  private final Map<String, Class<?>> schemaClassMap;

  public OpenApiRequiredFieldCustomizer() {
    this.schemaClassMap = buildSchemaClassMap();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void customise(OpenAPI openApi) {
    if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
      return;
    }

    for (Map.Entry<String, Schema> entry : openApi.getComponents().getSchemas().entrySet()) {
      String schemaName = entry.getKey();
      Schema<?> schema = entry.getValue();

      Class<?> clazz = schemaClassMap.get(schemaName);
      if (clazz == null || schema.getProperties() == null) {
        continue;
      }

      List<String> requiredFields = getNonOptionalFieldNames(clazz, schema);
      if (!requiredFields.isEmpty()) {
        // Merge with any existing required list (e.g., from @NotNull validation)
        List<String> existing =
            schema.getRequired() != null
                ? new ArrayList<>(schema.getRequired())
                : new ArrayList<>();
        for (String field : requiredFields) {
          if (!existing.contains(field)) {
            existing.add(field);
          }
        }
        schema.setRequired(existing);
      }
    }
  }

  /**
   * Returns field names that are NOT {@code Optional<T>} and exist in the schema properties. Only
   * includes fields that actually appear in the OpenAPI schema (SpringDoc may skip some).
   */
  private List<String> getNonOptionalFieldNames(Class<?> clazz, Schema<?> schema) {
    List<String> required = new ArrayList<>();
    Map<String, Boolean> fieldOptionalStatus = new HashMap<>();

    if (clazz.isRecord()) {
      for (RecordComponent rc : clazz.getRecordComponents()) {
        fieldOptionalStatus.put(rc.getName(), rc.getType() == Optional.class);
      }
    } else {
      for (Field field : clazz.getDeclaredFields()) {
        fieldOptionalStatus.put(field.getName(), field.getType() == Optional.class);
      }
    }

    for (String propertyName : schema.getProperties().keySet()) {
      Boolean isOptional = fieldOptionalStatus.get(propertyName);
      if (isOptional != null && !isOptional) {
        required.add(propertyName);
      }
    }

    return required;
  }

  private static Map<String, Class<?>> buildSchemaClassMap() {
    Map<String, Class<?>> map = new HashMap<>();

    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AssignableTypeFilter(Record.class));
    scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));

    for (String basePackage : SCAN_PACKAGES) {
      for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
        try {
          Class<?> clazz = Class.forName(bd.getBeanClassName());
          map.put(clazz.getSimpleName(), clazz);
          if (clazz.getEnclosingClass() != null) {
            map.put(clazz.getEnclosingClass().getSimpleName() + "." + clazz.getSimpleName(), clazz);
          }
        } catch (ClassNotFoundException e) {
          // Skip
        }
      }
    }

    return map;
  }
}
