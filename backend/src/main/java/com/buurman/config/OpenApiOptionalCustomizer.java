package com.buurman.config;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Marks all {@link Optional} fields in DTO schemas as {@code nullable: true} in the OpenAPI spec.
 *
 * <p>SpringDoc 3.0.1 with Jackson's Jdk8Module correctly unwraps {@code Optional<T>} to the inner
 * type in the schema, but does NOT set the nullable flag. This customizer fixes that by reflecting
 * on the DTO record components (or class fields) and marking matching schema properties as
 * nullable.
 *
 * <p>Runs as a {@link GlobalOpenApiCustomizer} so it applies across all API groups (app +
 * backoffice).
 */
@Component
public class OpenApiOptionalCustomizer implements GlobalOpenApiCustomizer {

  private static final String[] SCAN_PACKAGES = {
    "com.buurman.dto.response", "com.buurman.dto.request",
  };

  private final Map<String, Class<?>> schemaClassMap;

  public OpenApiOptionalCustomizer() {
    this.schemaClassMap = buildSchemaClassMap();
  }

  @Override
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

      Set<String> optionalFields = getOptionalFieldNames(clazz);
      for (String fieldName : optionalFields) {
        @SuppressWarnings("unchecked")
        Schema<?> property = (Schema<?>) schema.getProperties().get(fieldName);
        if (property != null) {
          property.setNullable(true);
        }
      }
    }
  }

  private Set<String> getOptionalFieldNames(Class<?> clazz) {
    Set<String> names = new HashSet<>();

    if (clazz.isRecord()) {
      for (RecordComponent rc : clazz.getRecordComponents()) {
        if (rc.getType() == Optional.class) {
          names.add(rc.getName());
        }
      }
    } else {
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == Optional.class) {
          names.add(field.getName());
        }
      }
    }

    return names;
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

          // Handle inner classes/records (SpringDoc may use enclosing name as prefix)
          if (clazz.getEnclosingClass() != null) {
            map.put(clazz.getEnclosingClass().getSimpleName() + "." + clazz.getSimpleName(), clazz);
          }
        } catch (ClassNotFoundException e) {
          // Skip classes that can't be loaded
        }
      }
    }

    return map;
  }
}
