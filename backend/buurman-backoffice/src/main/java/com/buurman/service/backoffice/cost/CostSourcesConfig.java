package com.buurman.service.backoffice.cost;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.buurman.domain.backoffice.CostProviderId;
import com.buurman.util.SkipTestCoverage;

/**
 * Registers the manual (config-driven) cost sources. API-backed sources are {@code @Component}s.
 */
@Configuration
@SkipTestCoverage
public class CostSourcesConfig {

  @Bean
  public CostSource cloudflareCostSource(CostProperties props) {
    return new ManualCostSource(CostProviderId.CLOUDFLARE, props);
  }

  @Bean
  public CostSource betterStackCostSource(CostProperties props) {
    return new ManualCostSource(CostProviderId.BETTER_STACK, props);
  }

  @Bean
  public CostSource mailgunCostSource(CostProperties props) {
    return new ManualCostSource(CostProviderId.MAILGUN, props);
  }
}
