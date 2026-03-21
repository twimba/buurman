package com.buurman.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class, method, or constructor for exclusion from JaCoCo coverage reports.
 *
 * <p>Apply this to trivial code that has no meaningful logic to test: pure DTO records, simple
 * value types, exception classes with only constructors, Spring configuration classes that are pure
 * wiring, etc.
 *
 * <p>Named {@code Generated} because JaCoCo automatically filters out code annotated with any
 * annotation whose simple name is "Generated" (since JaCoCo 0.8.2). This is the standard convention
 * used by many projects for coverage exclusion.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Generated {}
