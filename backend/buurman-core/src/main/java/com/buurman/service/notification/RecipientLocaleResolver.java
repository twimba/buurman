package com.buurman.service.notification;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.TeamPreferences;
import com.buurman.domain.UserPreferences;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.UserPreferencesRepository;
import com.buurman.util.DocumentLanguages;

import lombok.RequiredArgsConstructor;

/**
 * Decides which language a notification is written in.
 *
 * <p>The order is: the contact's own preference, then the recipient user's, then the caller's
 * context language (a contract's document language), then the team default, then English. The
 * contact comes first because a tenant's own language beats the language the landlord happens to
 * file contracts in.
 *
 * <p>Both the send path and the resend path use this, so a resent reminder cannot come back in a
 * different language than the original.
 */
@Component
@RequiredArgsConstructor
public class RecipientLocaleResolver {

  private final ContactRepository contactRepository;
  private final UserPreferencesRepository userPreferencesRepository;
  private final TeamPreferencesRepository teamPreferencesRepository;

  public Locale resolve(
      Optional<UUID> teamId,
      Optional<UUID> contactId,
      Optional<UUID> userId,
      Optional<String> contextLanguageTag) {
    return contactLanguage(teamId, contactId)
        .or(() -> userLanguage(userId))
        .or(() -> supported(contextLanguageTag))
        .or(() -> teamLanguage(teamId))
        .map(Locale::forLanguageTag)
        .orElse(Locale.ENGLISH);
  }

  /** Scoped by team on purpose: a contact id from another team must not resolve to its language. */
  private Optional<String> contactLanguage(Optional<UUID> teamId, Optional<UUID> contactId) {
    return teamId.flatMap(
        team ->
            contactId
                .flatMap(contact -> contactRepository.findByIdAndTeamId(contact, team))
                // Optional.ofNullable: a Contact built directly (not via the builder) leaves
                // this null, and Review Focus 4 requires the chain to fall through, never throw.
                .flatMap(
                    contact ->
                        Optional.ofNullable(contact.getPreferredLanguage())
                            .orElse(Optional.empty()))
                .flatMap(language -> supported(Optional.of(language))));
  }

  private Optional<String> userLanguage(Optional<UUID> userId) {
    return userId
        .flatMap(userPreferencesRepository::findByUserId)
        .map(UserPreferences::getLanguage)
        .flatMap(language -> supported(Optional.ofNullable(language)));
  }

  private Optional<String> teamLanguage(Optional<UUID> teamId) {
    return teamId
        .flatMap(teamPreferencesRepository::findByTeamId)
        .map(TeamPreferences::getDefaultLanguage)
        .flatMap(language -> supported(Optional.ofNullable(language)));
  }

  private Optional<String> supported(Optional<String> languageTag) {
    return languageTag.map(String::trim).filter(DocumentLanguages::isSupported);
  }
}
