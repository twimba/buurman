package com.buurman.domain.regulation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.buurman.domain.TerminationGivenBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminationNoticeRule {

  private UUID id;
  private UUID countryId;
  @Builder.Default private Optional<UUID> regionId = Optional.empty();
  private TerminationGivenBy partyType;
  @Builder.Default private Optional<Integer> minTenancyMonths = Optional.empty();
  private int noticeDays;
  private boolean groundsRequired;
  @Builder.Default private List<String> groundsCodes = List.of();
  @Builder.Default private Optional<String> sourceUrl = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
}
