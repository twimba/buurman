package com.buurman.security;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.buurman.exception.InsufficientPermissionsException;
import com.buurman.exception.TeamMembershipNotFoundException;

/**
 * AOP Aspect that enforces @RequiresTeamRole annotations on methods. Validates that the
 * authenticated user has the required role in their active team.
 */
@Aspect
@Component
public class TeamMembershipAspect {

  @Before("@annotation(requiresTeamRole)")
  public void checkTeamRole(JoinPoint joinPoint, RequiresTeamRole requiresTeamRole) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
      throw new InsufficientPermissionsException("Authentication required");
    }

    // Check if user has team membership
    if (!principal.hasTeam()) {
      throw new TeamMembershipNotFoundException("User is not a member of any team");
    }

    // Check required roles
    String[] requiredRoles = requiresTeamRole.value();
    if (requiredRoles.length > 0) {
      boolean hasRole = Arrays.asList(requiredRoles).contains(principal.getRole().orElse(null));
      if (!hasRole) {
        throw new InsufficientPermissionsException(
            "Access denied: requires one of " + Arrays.toString(requiredRoles));
      }
    }

    // Check ownership if required
    if (requiresTeamRole.requireOwner() && !principal.isOwner()) {
      throw new InsufficientPermissionsException("Access denied: team owner required");
    }
  }
}
