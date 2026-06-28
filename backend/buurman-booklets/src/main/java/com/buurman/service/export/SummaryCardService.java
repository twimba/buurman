package com.buurman.service.export;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.domain.identifier.ContractIdentifier;

/**
 * Generates the one-page A4-landscape entity summary cards: assembler (repos → display-ready
 * variables) → booklet Thymeleaf engine → {@link DocumentRenderer} in landscape. Property and
 * contact summaries are wired here as their assemblers land.
 */
@Service
public class SummaryCardService {

  private final TemplateEngine templateEngine;
  private final DocumentRenderer renderer;
  private final ContractSummaryAssembler contractAssembler;

  public SummaryCardService(
      @Qualifier("bookletTemplateEngine") TemplateEngine templateEngine,
      DocumentRenderer renderer,
      ContractSummaryAssembler contractAssembler) {
    this.templateEngine = templateEngine;
    this.renderer = renderer;
    this.contractAssembler = contractAssembler;
  }

  public byte[] contractSummary(ContractIdentifier identifier, UUID teamId, Locale locale) {
    return render(
        "contract-summary/generic", contractAssembler.assemble(identifier, teamId, locale), locale);
  }

  private byte[] render(String template, Map<String, Object> variables, Locale locale) {
    Context context = new Context(locale);
    context.setVariables(variables);
    String html = templateEngine.process(template, context);
    return renderer.render(html, PageSpec.A4_LANDSCAPE);
  }
}
