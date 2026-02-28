package com.buurman.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Enriches {@link Optional}-typed DTO fields in the OpenAPI spec:
 *
 * <ol>
 *   <li>Marks as nullable via OAS 3.1 {@code type: ["...", "null"]} (SpringDoc unwraps {@code
 *       Optional<T>} but omits nullable).
 *   <li>Propagates Jakarta validation constraints declared as TYPE_USE annotations on the inner
 *       type (e.g. {@code Optional<@Positive BigDecimal>}), which SpringDoc does not traverse.
 * </ol>
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

      Map<String, Annotation[]> optionalFields = getOptionalFieldAnnotations(clazz);
      for (Map.Entry<String, Annotation[]> fieldEntry : optionalFields.entrySet()) {
        @SuppressWarnings("unchecked")
        Schema<?> property = (Schema<?>) schema.getProperties().get(fieldEntry.getKey());
        if (property != null) {
          markNullable(property);
          applyValidationConstraints(property, fieldEntry.getValue());
        }
      }
    }
  }

  /** Marks a schema property as nullable using OAS 3.1 {@code type: ["...", "null"]} syntax. */
  private void markNullable(Schema<?> property) {
    Set<String> types = property.getTypes();
    if (types != null && !types.isEmpty()) {
      Set<String> newTypes = new LinkedHashSet<>(types);
      newTypes.add("null");
      property.setTypes(newTypes);
    } else if (property.getType() != null) {
      property.setTypes(new LinkedHashSet<>(Set.of(property.getType(), "null")));
      property.setType(null);
    }
  }

  /** Returns a map of Optional field name → TYPE_USE annotations on the inner type parameter. */
  private Map<String, Annotation[]> getOptionalFieldAnnotations(Class<?> clazz) {
    Map<String, Annotation[]> result = new HashMap<>();

    if (clazz.isRecord()) {
      for (RecordComponent rc : clazz.getRecordComponents()) {
        if (rc.getType() == Optional.class) {
          result.put(rc.getName(), getTypeArgAnnotations(rc.getAnnotatedType()));
        }
      }
    } else {
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == Optional.class) {
          result.put(field.getName(), getTypeArgAnnotations(field.getAnnotatedType()));
        }
      }
    }

    return result;
  }

  private Annotation[] getTypeArgAnnotations(AnnotatedType annotatedType) {
    if (annotatedType instanceof AnnotatedParameterizedType paramType) {
      AnnotatedType[] typeArgs = paramType.getAnnotatedActualTypeArguments();
      if (typeArgs.length > 0) {
        return typeArgs[0].getAnnotations();
      }
    }
    return new Annotation[0];
  }

  /** Maps Jakarta validation annotations to OpenAPI 3.1 schema constraints. */
  @SuppressWarnings("unchecked")
  private void applyValidationConstraints(Schema<?> property, Annotation[] annotations) {
    for (Annotation annotation : annotations) {
      if (annotation instanceof Positive) {
        property.setExclusiveMinimumValue(BigDecimal.ZERO);
      } else if (annotation instanceof PositiveOrZero) {
        property.setMinimum(BigDecimal.ZERO);
      } else if (annotation instanceof Min min) {
        property.setMinimum(BigDecimal.valueOf(min.value()));
      } else if (annotation instanceof Max max) {
        property.setMaximum(BigDecimal.valueOf(max.value()));
      } else if (annotation instanceof DecimalMin dm) {
        BigDecimal val = new BigDecimal(dm.value());
        if (dm.inclusive()) {
          property.setMinimum(val);
        } else {
          property.setExclusiveMinimumValue(val);
        }
      } else if (annotation instanceof DecimalMax dm) {
        BigDecimal val = new BigDecimal(dm.value());
        if (dm.inclusive()) {
          property.setMaximum(val);
        } else {
          property.setExclusiveMaximumValue(val);
        }
      } else if (annotation instanceof Size size) {
        if (size.min() > 0) {
          property.setMinLength(size.min());
        }
        if (size.max() < Integer.MAX_VALUE) {
          property.setMaxLength(size.max());
        }
      } else if (annotation instanceof Email) {
        property.setFormat("email");
      } else if (annotation instanceof jakarta.validation.constraints.Pattern pattern) {
        property.setPattern(pattern.regexp());
      }
    }
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
