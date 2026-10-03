package com.buurman.exception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

  @RestController
  static class ThrowingController {
    @GetMapping("/lease-country")
    String country() {
      throw LeaseNotAvailableException.forCountry("IT");
    }

    @GetMapping("/lease-no-country")
    String noCountry() {
      throw LeaseNotAvailableException.noCountry();
    }

    @GetMapping("/plain")
    String plain() {
      throw new BusinessRuleException("nope");
    }
  }

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    MessageSource messageSource = mock(MessageSource.class);
    when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Conflict");
    mvc =
        MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler(messageSource))
            .build();
  }

  @Test
  void leaseNotAvailableIs409WithCodeInJson() throws Exception {
    mvc.perform(get("/lease-country"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("LEASE_NOT_AVAILABLE_FOR_COUNTRY"))
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("IT")))
        .andExpect(jsonPath("$.instance").value("/lease-country"));
  }

  @Test
  void noCountryUsesItsOwnCode() throws Exception {
    mvc.perform(get("/lease-no-country"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("LEASE_CONTRACT_HAS_NO_COUNTRY"));
  }

  @Test
  void plainBusinessRuleExceptionHasNoCode() throws Exception {
    mvc.perform(get("/plain"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").doesNotExist());
  }
}
