package com.buurman.service.export;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.buurman.domain.Unit;
import com.buurman.service.export.tabular.ExcelRenderer;
import com.buurman.service.export.tabular.UnitTabularExportBuilder;

import lombok.RequiredArgsConstructor;

/**
 * Excel export for an explicit list of units, with type/status labels localized to {@code locale}.
 */
@Component
@RequiredArgsConstructor
public class UnitExcelExporter {

  private final UnitTabularExportBuilder builder;
  private final ExcelRenderer renderer;

  public byte[] export(List<Unit> units, Locale locale) {
    return renderer.render(builder.build(units, locale));
  }
}
