package com.buurman.security;

import java.util.Set;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.buurman.domain.ImpersonationMode;
import com.buurman.exception.ImpersonationRestrictionException;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class ImpersonationRestrictionAspect {

  // Service methods that are always blocked during impersonation (any mode)
  private static final Set<String> BLOCKED_METHODS =
      Set.of(
          "changePassword",
          "updateEmail",
          "deleteAccount",
          "requestAccountDeletion",
          "updateBillingInfo",
          "cancelSubscription",
          "updateSubscription",
          "enableMfa",
          "disableMfa",
          "createTeam",
          "deleteTeam",
          "inviteTeamMember",
          "removeTeamMember");

  // HTTP methods that indicate write operations
  private static final Set<String> WRITE_HTTP_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

  @Before("execution(* com.buurman.service..*(..)) || execution(* com.buurman.controller..*(..))")
  public void checkImpersonationRestrictions(JoinPoint joinPoint) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof ImpersonationPrincipal principal)) {
      return;
    }

    String methodName = joinPoint.getSignature().getName();

    // Always block sensitive operations
    if (BLOCKED_METHODS.contains(methodName)) {
      throw new ImpersonationRestrictionException(
          "Operation '" + methodName + "' is not allowed during impersonation");
    }

    // In READ_ONLY mode, block write operations based on HTTP method
    if (principal.getMode() == ImpersonationMode.READ_ONLY) {
      ServletRequestAttributes attrs =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs != null) {
        HttpServletRequest request = attrs.getRequest();
        if (WRITE_HTTP_METHODS.contains(request.getMethod())) {
          throw new ImpersonationRestrictionException(
              "Write operations are not allowed in read-only impersonation mode");
        }
      }
    }
  }
}
