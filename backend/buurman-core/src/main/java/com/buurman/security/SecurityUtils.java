package com.buurman.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Utility to retrieve the current {@link UserPrincipal} from the SecurityContext. */
public final class SecurityUtils {

  private SecurityUtils() {}

  /**
   * Returns the {@link UserPrincipal} of the currently authenticated user.
   *
   * @throws IllegalStateException if no authenticated user is in the SecurityContext
   */
  public static UserPrincipal getCurrentPrincipal() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
      throw new IllegalStateException("No authenticated UserPrincipal in SecurityContext");
    }
    return principal;
  }

  /**
   * Returns the {@link BackofficePrincipal} of the currently authenticated backoffice user.
   *
   * @throws IllegalStateException if no authenticated backoffice user is in the SecurityContext
   */
  public static BackofficePrincipal getBackofficePrincipal() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof BackofficePrincipal principal)) {
      throw new IllegalStateException("No authenticated BackofficePrincipal in SecurityContext");
    }
    return principal;
  }

  /** Returns true if the current principal is an impersonation session. */
  public static boolean isImpersonating() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getPrincipal() instanceof ImpersonationPrincipal;
  }

  /** Returns the {@link ImpersonationPrincipal} if the current session is impersonated, or null. */
  public static @org.jspecify.annotations.Nullable ImpersonationPrincipal
      getImpersonationPrincipal() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof ImpersonationPrincipal principal) {
      return principal;
    }
    return null;
  }
}
