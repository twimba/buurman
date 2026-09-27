package com.buurman.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Contact;
import com.buurman.domain.TeamPreferences;
import com.buurman.domain.UserPreferences;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.UserPreferencesRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecipientLocaleResolver")
class RecipientLocaleResolverTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTACT_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private ContactRepository contactRepository;
  @Mock private UserPreferencesRepository userPreferencesRepository;
  @Mock private TeamPreferencesRepository teamPreferencesRepository;

  private RecipientLocaleResolver resolver;

  @BeforeEach
  void setUp() {
    resolver =
        new RecipientLocaleResolver(
            contactRepository, userPreferencesRepository, teamPreferencesRepository);
  }

  private Contact contactWithLanguage(Optional<String> language) {
    Contact contact = new Contact();
    contact.setPreferredLanguage(language);
    return contact;
  }

  private void teamDefaultIs(String language) {
    TeamPreferences preferences = new TeamPreferences();
    preferences.setDefaultLanguage(language);
    lenient()
        .when(teamPreferencesRepository.findByTeamId(TEAM_ID))
        .thenReturn(Optional.of(preferences));
  }

  @Test
  @DisplayName("the contact's own language wins")
  void contactLanguageWins() {
    teamDefaultIs("nl");
    when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID))
        .thenReturn(Optional.of(contactWithLanguage(Optional.of("pt"))));

    Locale locale =
        resolver.resolve(
            Optional.of(TEAM_ID), Optional.of(CONTACT_ID), Optional.empty(), Optional.of("de"));

    assertThat(locale).isEqualTo(Locale.forLanguageTag("pt"));
  }

  @Test
  @DisplayName("a contact with no language of its own falls through")
  void nullContactLanguageFallsThrough() {
    teamDefaultIs("nl");
    when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID))
        .thenReturn(Optional.of(contactWithLanguage(Optional.empty())));

    Locale locale =
        resolver.resolve(
            Optional.of(TEAM_ID), Optional.of(CONTACT_ID), Optional.empty(), Optional.of("de"));

    assertThat(locale).isEqualTo(Locale.forLanguageTag("de"));
  }

  @Test
  @DisplayName("a contact that is not in this team falls through rather than leaking")
  void contactFromAnotherTeamFallsThrough() {
    teamDefaultIs("nl");
    when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID)).thenReturn(Optional.empty());

    Locale locale =
        resolver.resolve(
            Optional.of(TEAM_ID), Optional.of(CONTACT_ID), Optional.empty(), Optional.empty());

    assertThat(locale).isEqualTo(Locale.forLanguageTag("nl"));
  }

  @Test
  @DisplayName("no team means no contact lookup at all")
  void noTeamMeansNoContactLookup() {
    Locale locale =
        resolver.resolve(
            Optional.empty(), Optional.of(CONTACT_ID), Optional.empty(), Optional.of("fr"));

    assertThat(locale).isEqualTo(Locale.forLanguageTag("fr"));
    verifyNoInteractions(contactRepository);
  }

  @Test
  @DisplayName("the recipient user's preference beats the context language")
  void userPreferenceBeatsContext() {
    UserPreferences preferences = new UserPreferences();
    preferences.setLanguage("sv");
    when(userPreferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.of(preferences));

    Locale locale =
        resolver.resolve(
            Optional.of(TEAM_ID), Optional.empty(), Optional.of(USER_ID), Optional.of("de"));

    assertThat(locale).isEqualTo(Locale.forLanguageTag("sv"));
  }

  @Test
  @DisplayName("the context language beats the team default")
  void contextBeatsTeamDefault() {
    teamDefaultIs("nl");

    Locale locale =
        resolver.resolve(
            Optional.of(TEAM_ID), Optional.empty(), Optional.empty(), Optional.of("de"));

    assertThat(locale).isEqualTo(Locale.forLanguageTag("de"));
  }

  @Test
  @DisplayName("a blank context language is ignored")
  void blankContextIsIgnored() {
    teamDefaultIs("nl");

    Locale locale =
        resolver.resolve(
            Optional.of(TEAM_ID), Optional.empty(), Optional.empty(), Optional.of("  "));

    assertThat(locale).isEqualTo(Locale.forLanguageTag("nl"));
  }

  @Test
  @DisplayName("an unsupported language anywhere in the chain is ignored")
  void unsupportedLanguageIsIgnored() {
    teamDefaultIs("nl");
    when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID))
        .thenReturn(Optional.of(contactWithLanguage(Optional.of("klingon"))));

    Locale locale =
        resolver.resolve(
            Optional.of(TEAM_ID), Optional.of(CONTACT_ID), Optional.empty(), Optional.empty());

    assertThat(locale).isEqualTo(Locale.forLanguageTag("nl"));
  }

  @Test
  @DisplayName("with nothing to go on it falls back to English")
  void fallsBackToEnglish() {
    when(teamPreferencesRepository.findByTeamId(any())).thenReturn(Optional.empty());

    Locale locale =
        resolver.resolve(
            Optional.of(TEAM_ID), Optional.empty(), Optional.empty(), Optional.empty());

    assertThat(locale).isEqualTo(Locale.ENGLISH);
  }
}
