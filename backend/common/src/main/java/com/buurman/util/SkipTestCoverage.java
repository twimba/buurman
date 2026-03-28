package com.buurman.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class, method, or constructor as trivial code that does not warrant unit test coverage.
 *
 * <p>Apply this to code with no meaningful logic to test: pure DTO records, simple value types,
 * exception classes with only constructors, Spring configuration classes that are pure wiring, etc.
 *
 * <p>This is a documentation-only annotation. JaCoCo coverage exclusion is handled via explicit
 * {@code <excludes>} patterns in the JaCoCo Maven plugin configuration.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface SkipTestCoverage {}
