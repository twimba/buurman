package com.buurman.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.buurman.domain.ContactType;
import com.buurman.dto.request.CreateContactRequest;
import com.buurman.util.DocumentLanguages;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * The database CHECK constraint already rejects an unsupported language, but it surfaces as a 409
 * with a constraint name in it. Bean validation is what turns that into a 400 naming the field.
 */
@DisplayName("contact request validation")
class ContactRequestValidationTest {

  private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
  private static final Validator VALIDATOR = FACTORY.getValidator();

  private static CreateContactRequest withLanguage(Optional<String> language) {
    return new CreateContactRequest(
        ContactType.INDIVIDUAL,
        Optional.of("Jan"),
        Optional.of("Jansen"),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        language);
  }

  static java.util.stream.Stream<String> supportedLanguages() {
    return DocumentLanguages.ORDERED.stream();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("supportedLanguages")
  @DisplayName("every supported language is accepted")
  void supportedLanguageIsAccepted(String language) {
    assertThat(VALIDATOR.validate(withLanguage(Optional.of(language)))).isEmpty();
  }

  @ParameterizedTest(name = "\"{0}\"")
  @ValueSource(strings = {"xx", "EN", "eng", "", " ", "klingon"})
  @DisplayName("an unsupported language code is rejected before it reaches the database")
  void unsupportedLanguageIsRejected(String language) {
    Set<ConstraintViolation<CreateContactRequest>> violations =
        VALIDATOR.validate(withLanguage(Optional.of(language)));

    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getPropertyPath().toString())
        .contains("preferredLanguage");
  }

  @org.junit.jupiter.api.Test
  @DisplayName("no language at all is accepted — it falls through to the team default")
  void absentLanguageIsAccepted() {
    assertThat(VALIDATOR.validate(withLanguage(Optional.empty()))).isEmpty();
  }
}
