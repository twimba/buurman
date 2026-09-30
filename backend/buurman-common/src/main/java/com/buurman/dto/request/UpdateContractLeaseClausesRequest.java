package com.buurman.dto.request;

import java.util.List;

public record UpdateContractLeaseClausesRequest(List<ClauseSelection> clauses) {
  public record ClauseSelection(String templateIdentifier, boolean included, int sortOrder) {}
}
