package com.buurman.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import com.buurman.domain.ImpersonationMode;
import com.buurman.domain.Sid;
import com.buurman.domain.TeamRole;
import com.buurman.exception.ImpersonationRestrictionException;

@DisplayName("ImpersonationRestrictionInterceptor")
class ImpersonationRestrictionInterceptorTest {

  private ImpersonationRestrictionInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new ImpersonationRestrictionInterceptor();
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private ImpersonationPrincipal createImpersonationPrincipal(ImpersonationMode mode) {
    return new ImpersonationPrincipal(
        UUID.randomUUID(),
        "USR01HQJK4B2X5M3N7P8Q9R0S1T2",
        "kc-123",
        "user@example.com",
        "Jane Tenant",
        UUID.randomUUID(),
        "TEA01HQJK4B2X5M3N7P8Q9R0S1T2",
        TeamRole.TEAM_ADMIN,
        true,
        true,
        Sid.of("IMP01HQJK4B2X5M3N7P8Q9R0S1T2"),
        UUID.randomUUID(),
        "admin@buurman.io",
        "Admin User",
        mode);
  }

  private void setImpersonationContext(ImpersonationMode mode) {
    ImpersonationPrincipal principal = createImpersonationPrincipal(mode);
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private void setRegularUserContext() {
    UserPrincipal principal =
        new UserPrincipal(
            UUID.randomUUID(),
            "USR01HQJK4B2X5M3N7P8Q9R0S1T2",
            "kc-123",
            "user@example.com",
            "Regular User",
            UUID.randomUUID(),
            "TEA01HQJK4B2X5M3N7P8Q9R0S1T2",
            TeamRole.TEAM_ADMIN,
            true,
            true);
    UserAuthentication auth = new UserAuthentication(principal, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private HandlerMethod createHandlerMethod(String methodName) throws Exception {
    // Use a dummy class with the method name we want to test
    Method method = DummyController.class.getDeclaredMethod(methodName);
    return new HandlerMethod(new DummyController(), method);
  }

  @Nested
  @DisplayName("non-impersonation requests")
  class NonImpersonation {

    @Test
    @DisplayName("allows request when no authentication")
    void allowsWhenNoAuth() throws Exception {
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/teams");
      MockHttpServletResponse response = new MockHttpServletResponse();

      boolean result = interceptor.preHandle(request, response, new Object());

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("allows request when regular UserPrincipal (not impersonating)")
    void allowsRegularUser() throws Exception {
      setRegularUserContext();
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/teams");
      MockHttpServletResponse response = new MockHttpServletResponse();

      boolean result =
          interceptor.preHandle(request, response, createHandlerMethod("changePassword"));

      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("always-blocked methods (any impersonation mode)")
  class AlwaysBlockedMethods {

    @ParameterizedTest(name = "{0} is always blocked during impersonation")
    @ValueSource(
        strings = {
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
          "removeTeamMember"
        })
    @DisplayName("blocked method")
    void blockedInFullMode(String methodName) throws Exception {
      setImpersonationContext(ImpersonationMode.FULL);
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
      MockHttpServletResponse response = new MockHttpServletResponse();

      assertThatThrownBy(
              () -> interceptor.preHandle(request, response, createHandlerMethod(methodName)))
          .isInstanceOf(ImpersonationRestrictionException.class)
          .hasMessageContaining(methodName)
          .hasMessageContaining("not allowed during impersonation");
    }

    @ParameterizedTest(name = "{0} is also blocked in READ_ONLY mode")
    @ValueSource(
        strings = {
          "changePassword",
          "updateEmail",
          "deleteAccount",
          "requestAccountDeletion"
        })
    @DisplayName("also blocked in READ_ONLY")
    void blockedInReadOnlyMode(String methodName) throws Exception {
      setImpersonationContext(ImpersonationMode.READ_ONLY);
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
      MockHttpServletResponse response = new MockHttpServletResponse();

      assertThatThrownBy(
              () -> interceptor.preHandle(request, response, createHandlerMethod(methodName)))
          .isInstanceOf(ImpersonationRestrictionException.class);
    }
  }

  @Nested
  @DisplayName("READ_ONLY mode — write HTTP methods blocked")
  class ReadOnlyWriteMethods {

    @ParameterizedTest(name = "{0} is blocked in READ_ONLY mode")
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    @DisplayName("write HTTP method blocked")
    void writeMethodsBlocked(String httpMethod) throws Exception {
      setImpersonationContext(ImpersonationMode.READ_ONLY);
      MockHttpServletRequest request = new MockHttpServletRequest(httpMethod, "/api/v1/properties");
      MockHttpServletResponse response = new MockHttpServletResponse();

      assertThatThrownBy(
              () ->
                  interceptor.preHandle(
                      request, response, createHandlerMethod("safeMethod")))
          .isInstanceOf(ImpersonationRestrictionException.class)
          .hasMessageContaining("Write operations are not allowed in read-only impersonation mode");
    }

    @Test
    @DisplayName("GET is allowed in READ_ONLY mode")
    void getIsAllowed() throws Exception {
      setImpersonationContext(ImpersonationMode.READ_ONLY);
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/properties");
      MockHttpServletResponse response = new MockHttpServletResponse();

      boolean result =
          interceptor.preHandle(request, response, createHandlerMethod("safeMethod"));

      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("FULL mode — write HTTP methods allowed")
  class FullModeWriteMethods {

    @ParameterizedTest(name = "{0} is allowed in FULL mode for non-blocked methods")
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    @DisplayName("write HTTP method allowed")
    void writeMethodsAllowed(String httpMethod) throws Exception {
      setImpersonationContext(ImpersonationMode.FULL);
      MockHttpServletRequest request = new MockHttpServletRequest(httpMethod, "/api/v1/properties");
      MockHttpServletResponse response = new MockHttpServletResponse();

      boolean result =
          interceptor.preHandle(request, response, createHandlerMethod("safeMethod"));

      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("non-HandlerMethod handler")
  class NonHandlerMethod {

    @Test
    @DisplayName("skips method name check for non-HandlerMethod handlers (e.g. static resources)")
    void skipsNonHandlerMethod() {
      setImpersonationContext(ImpersonationMode.FULL);
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/static/app.js");
      MockHttpServletResponse response = new MockHttpServletResponse();

      assertThatCode(() -> interceptor.preHandle(request, response, new Object()))
          .doesNotThrowAnyException();
    }
  }

  /**
   * Dummy controller class with methods matching the blocked method names. Used to create
   * HandlerMethod instances for testing.
   */
  @SuppressWarnings("unused")
  static class DummyController {
    public void changePassword() {}

    public void updateEmail() {}

    public void deleteAccount() {}

    public void requestAccountDeletion() {}

    public void updateBillingInfo() {}

    public void cancelSubscription() {}

    public void updateSubscription() {}

    public void enableMfa() {}

    public void disableMfa() {}

    public void createTeam() {}

    public void deleteTeam() {}

    public void inviteTeamMember() {}

    public void removeTeamMember() {}

    public void safeMethod() {}
  }
}
