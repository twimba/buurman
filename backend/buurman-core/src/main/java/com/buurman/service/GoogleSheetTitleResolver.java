package com.buurman.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.buurman.domain.Team;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * Builds the title used when creating a new Google Sheet. The format is {@code "Buurman — {Entity}
 * — {ISO date}"}, with the team name inserted only when the user is a member of more than one team
 * (so single-team users get short, uncluttered titles).
 */
@Service
@RequiredArgsConstructor
public class GoogleSheetTitleResolver {

  private final TeamMemberRepository teamMemberRepository;
  private final TeamRepository teamRepository;
  private final Clock clock;

  /** Title for a per-entity export (date only). */
  public String resolve(UserPrincipal principal, String entityLabel) {
    return base(
        principal, entityLabel, LocalDate.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE));
  }

  /** Title for the full takeout (date + time, so multiple same-day runs read clearly). */
  public String resolveTakeout(UserPrincipal principal) {
    String timestamp =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(clock.getZone())
            .format(clock.instant());
    return base(principal, "Full export", timestamp);
  }

  private String base(UserPrincipal principal, String entityLabel, String dateLabel) {
    String prefix = "Buurman";
    if (showTeamName(principal)) {
      Optional<Team> team = teamRepository.findById(principal.requireTeamId());
      String teamName = team.map(Team::getName).orElse("");
      if (!teamName.isBlank()) {
        prefix = prefix + " — " + teamName;
      }
    }
    return prefix + " — " + entityLabel + " — " + dateLabel;
  }

  private boolean showTeamName(UserPrincipal principal) {
    return teamMemberRepository.findAllByUserId(principal.getUserId()).size() > 1;
  }
}
