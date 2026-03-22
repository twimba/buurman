package com.buurman.service;

import static com.buurman.domain.NotificationType.PHONE_VERIFICATION_CODE;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.PhoneVerificationCode;
import com.buurman.domain.User;
import com.buurman.dto.response.UserProfileResponse;
import com.buurman.exception.VerificationCodeException;
import com.buurman.repository.PhoneVerificationCodeRepository;
import com.buurman.repository.UserRepository;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PhoneVerificationService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final PhoneVerificationCodeRepository verificationCodeRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;
  private final PhoneNumberPolicyService phoneNumberPolicyService;
  private final Clock clock;

  @Transactional
  public void sendVerificationCode(UUID userId) {
    User user = userRepository.getById(userId);

    if (user.getPhone().map(String::isBlank).orElse(true)) {
      throw new VerificationCodeException("No phone number to verify");
    }

    if (user.getPhoneVerifiedAt().isPresent()) {
      throw new VerificationCodeException("Phone is already verified");
    }

    // Enforce minimum 60s between codes
    verificationCodeRepository
        .findMostRecentCreatedAt(userId)
        .ifPresent(
            lastSent -> {
              long secondsSince = clock.instant().getEpochSecond() - lastSent.getEpochSecond();
              if (secondsSince < 60) {
                throw new VerificationCodeException(
                    "Please wait "
                        + (60 - secondsSince)
                        + " seconds before requesting another code.");
              }
            });

    int maxCodesPerHour = phoneNumberPolicyService.getPolicy().getMaxCodesPerHour();
    int recentCount =
        verificationCodeRepository.countRecentByUserId(userId, clock.instant().minus(1, HOURS));
    if (recentCount >= maxCodesPerHour) {
      throw new VerificationCodeException(
          "Too many verification attempts. Please try again later.");
    }

    verificationCodeRepository.invalidateAllForUser(userId);
    String code = generateVerificationCode();
    createAndSendCode(user, code);
  }

  @Transactional
  public UserProfileResponse verifyPhone(UUID userId, String code) {
    User user = userRepository.getById(userId);

    if (user.getPhone().map(String::isBlank).orElse(true)) {
      throw new VerificationCodeException("No phone number to verify");
    }

    if (user.getPhoneVerifiedAt().isPresent()) {
      throw new VerificationCodeException("Phone is already verified");
    }

    String phone =
        user.getPhone().orElseThrow(() -> new IllegalStateException("User has no phone number"));
    PhoneVerificationCode validCode =
        verificationCodeRepository
            .findValidCode(userId, code, phone)
            .orElseThrow(
                () -> new VerificationCodeException("Invalid or expired verification code"));

    verificationCodeRepository.markUsed(validCode.getId());
    userRepository.updatePhoneVerifiedAt(userId);

    user.setPhoneVerifiedAt(Optional.of(clock.instant()));

    return new UserProfileResponse(
        user.getIdentifier().orElseThrow(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getPhone(),
        true);
  }

  @Transactional
  public void resendVerificationCode(UUID userId) {
    sendVerificationCode(userId);
  }

  @Transactional
  public UserProfileResponse cancelVerification(UUID userId) {
    User user = userRepository.getById(userId);

    if (user.getPhone().map(String::isBlank).orElse(true)) {
      throw new VerificationCodeException("No phone number to cancel verification for");
    }

    verificationCodeRepository.invalidateAllForUser(userId);
    userRepository.clearPhoneVerifiedAt(userId);
    user.setPhoneVerifiedAt(Optional.empty());

    return new UserProfileResponse(
        user.getIdentifier().orElseThrow(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getPhone(),
        false);
  }

  private void createAndSendCode(User user, String code) {
    int expiryMinutes = phoneNumberPolicyService.getPolicy().getVerificationCodeExpiryMinutes();

    PhoneVerificationCode verificationCode = new PhoneVerificationCode();
    verificationCode.setUserId(user.getId());
    verificationCode.setPhone(
        user.getPhone().orElseThrow(() -> new IllegalStateException("User has no phone number")));
    verificationCode.setCode(code);
    verificationCode.setExpiresAt(clock.instant().plus(expiryMinutes, MINUTES));
    verificationCodeRepository.save(verificationCode);

    SendNotificationRequest sendNotificationRequest =
        SendNotificationRequest.builder()
            .teamId(user.getActiveTeamId())
            .notificationType(PHONE_VERIFICATION_CODE)
            .recipientUserId(Optional.of(user.getId()))
            .recipientPhone(user.getPhone())
            .createdBy(user.getId())
            .templateName("phone-verification-code")
            .templateVariables(
                Map.of(
                    "userName", user.getFirstName(),
                    "verificationCode", code,
                    "expiresMinutes", expiryMinutes))
            .build();

    notificationService.send(sendNotificationRequest);

    log.info("Phone verification code sent to user: {}", user.getIdentifier().orElseThrow());
  }

  private String generateVerificationCode() {
    return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
  }
}
