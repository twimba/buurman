package com.buurman.security;

import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.buurman.domain.ImpersonationMode;
import com.buurman.exception.ImpersonationRestrictionException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ImpersonationRestrictionInterceptor implements HandlerInterceptor {

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

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof ImpersonationPrincipal principal)) {
      return true;
    }

    // Check method name against blocked list (only for controller handler methods)
    if (handler instanceof HandlerMethod handlerMethod) {
      String methodName = handlerMethod.getMethod().getName();
      if (BLOCKED_METHODS.contains(methodName)) {
        throw new ImpersonationRestrictionException(
            "Operation '" + methodName + "' is not allowed during impersonation");
      }
    }

    // In READ_ONLY mode, block write operations based on HTTP method
    if (principal.getMode() == ImpersonationMode.READ_ONLY) {
      if (WRITE_HTTP_METHODS.contains(request.getMethod())) {
        throw new ImpersonationRestrictionException(
            "Write operations are not allowed in read-only impersonation mode");
      }
    }

    return true;
  }
}
