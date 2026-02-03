package com.buurman.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enforce team role-based access control on methods.
 * The authenticated user must have one of the specified roles in their active team.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresTeamRole {
    /**
     * Required roles - user must have at least one of these.
     * Examples: "TEAM_ADMIN", "TEAM_EDITOR", "TEAM_VIEWER"
     */
    String[] value();

    /**
     * If true, user must be the team owner (in addition to having required role).
     */
    boolean requireOwner() default false;
}
