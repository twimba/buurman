package com.buurman.controller.backoffice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.buurman.domain.LeaseAvailability;
import com.buurman.dto.response.backoffice.LeaseAgreementPreviewResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.GlobalExceptionHandler;
import com.buurman.service.backoffice.BackofficeLeaseClauseTemplateService;
import com.buurman.service.letters.LeasePreviewService;

/**
 * Real Spring MVC dispatch (generated API mappings, real controller, real {@link
 * GlobalExceptionHandler}); the preview service is a mock, except that the validation case calls
 * its real method (validation runs before any collaborator is touched). No security filter chain:
 * the role gate is covered by LeasePreviewAuthorizationTest.
 */
@DisplayName("POST lease-clause-templates/preview over the real MVC stack")
class LeaseAgreementPreviewHttpTest {

  private static final String URL = "/backoffice/lease-clause-templates/preview";
  private static final LeasePreviewService SERVICE = mock(LeasePreviewService.class);

  private static final String VALID_BODY =
      """
      {"countryCode":"NL","leaseKind":"RESIDENTIAL","language":"nl",
       "sample":{"contractType":"INDEFINITE","startDate":"2026-03-01",
         "rentComponents":[{"type":"BASE_RENT","amount":1250,"currency":"EUR"}],
         "landlordName":"L","propertyAddress":"A"}}
      """;

  @Configuration
  @EnableWebMvc
  static class Config {
    @Bean
    BackofficeLeaseClauseTemplateController controller() {
      return new BackofficeLeaseClauseTemplateController(
          mock(BackofficeLeaseClauseTemplateService.class), SERVICE);
    }

    @Bean
    GlobalExceptionHandler globalExceptionHandler() {
      return new GlobalExceptionHandler(mock(MessageSource.class));
    }
  }

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    reset(SERVICE);
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.register(Config.class);
    context.refresh();
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  private org.springframework.test.web.servlet.ResultActions call(String body) throws Exception {
    return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  @Test
  @DisplayName("200 carries the preview response")
  void ok() throws Exception {
    when(SERVICE.preview(any()))
        .thenReturn(
            new LeaseAgreementPreviewResponse(
                LeaseAvailability.UNAVAILABLE_COUNTRY,
                null,
                null,
                null,
                null,
                java.util.List.of()));

    call(VALID_BODY)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.availability").value("UNAVAILABLE_COUNTRY"))
        .andExpect(jsonPath("$.html").doesNotExist());
  }

  @Test
  @DisplayName("all clauses excluded (BusinessRuleException) is a 409 ProblemDetail")
  void businessRuleIsConflict() throws Exception {
    when(SERVICE.preview(any()))
        .thenThrow(new BusinessRuleException("No lease clauses are included"));

    call(VALID_BODY)
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.detail").value("No lease clauses are included"))
        .andExpect(jsonPath("$.instance").value(URL));
  }

  @Test
  @DisplayName("a service validation failure is a 400 ProblemDetail")
  void validationFailureIsBadRequest() throws Exception {
    when(SERVICE.preview(any())).thenCallRealMethod();

    call(VALID_BODY.replace("\"NL\"", "\"nl\""))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(
            jsonPath("$.detail").value("countryCode must be a two-letter upper-case ISO code"));
  }

  @Test
  @DisplayName("an unknown enum value is a 400 ProblemDetail")
  void badEnumIsBadRequest() throws Exception {
    call(VALID_BODY.replace("RESIDENTIAL", "NOT_A_KIND"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  @DisplayName("a malformed body is a 400 ProblemDetail")
  void malformedBodyIsBadRequest() throws Exception {
    call("{not json").andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
  }
}
