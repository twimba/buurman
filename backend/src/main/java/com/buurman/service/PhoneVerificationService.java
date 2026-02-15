package com.buurman.service;

import com.buurman.domain.PhoneVerificationCode;
import com.buurman.domain.User;
import com.buurman.dto.response.UserProfileResponse;
import com.buurman.exception.VerificationCodeException;
import com.buurman.repository.PhoneVerificationCodeRepository;
import com.buurman.repository.UserRepository;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static com.buurman.domain.NotificationType.PHONE_VERIFICATION_CODE;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

@Service
public class PhoneVerificationService {

    private static final Logger log = LoggerFactory.getLogger(PhoneVerificationService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PhoneVerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PhoneNumberPolicyService phoneNumberPolicyService;
    private final Clock clock;

    public PhoneVerificationService(PhoneVerificationCodeRepository verificationCodeRepository,
                                    UserRepository userRepository,
                                    NotificationService notificationService,
                                    PhoneNumberPolicyService phoneNumberPolicyService,
                                    Clock clock) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.phoneNumberPolicyService = phoneNumberPolicyService;
        this.clock = clock;
    }

    @Transactional
    public void sendVerificationCode(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new VerificationCodeException("No phone number to verify");
        }

        if (user.getPhoneVerifiedAt() != null) {
            throw new VerificationCodeException("Phone is already verified");
        }

        // Enforce minimum 60s between codes
        verificationCodeRepository.findMostRecentCreatedAt(userId).ifPresent(lastSent -> {
            long secondsSince = clock.instant().getEpochSecond() - lastSent.getEpochSecond();
            if (secondsSince < 60) {
                throw new VerificationCodeException(
                    "Please wait " + (60 - secondsSince) + " seconds before requesting another code.");
            }
        });

        int maxCodesPerHour = phoneNumberPolicyService.getPolicy().getMaxCodesPerHour();
        int recentCount = verificationCodeRepository.countRecentByUserId(
            userId, clock.instant().minus(1, HOURS));
        if (recentCount >= maxCodesPerHour) {
            throw new VerificationCodeException("Too many verification attempts. Please try again later.");
        }

        verificationCodeRepository.invalidateAllForUser(userId);
        String code = generateVerificationCode();
        createAndSendCode(user, code);
    }

    @Transactional
    public UserProfileResponse verifyPhone(UUID userId, String code) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new VerificationCodeException("No phone number to verify");
        }

        if (user.getPhoneVerifiedAt() != null) {
            throw new VerificationCodeException("Phone is already verified");
        }

        PhoneVerificationCode validCode = verificationCodeRepository
            .findValidCode(userId, code, user.getPhone())
            .orElseThrow(() -> new VerificationCodeException("Invalid or expired verification code"));

        verificationCodeRepository.markUsed(validCode.getId());
        userRepository.updatePhoneVerifiedAt(userId);

        user.setPhoneVerifiedAt(clock.instant());

        return new UserProfileResponse(
            user.getIdentifier(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhone(),
            true
        );
    }

    @Transactional
    public void resendVerificationCode(UUID userId) {
        sendVerificationCode(userId);
    }

    @Transactional
    public UserProfileResponse cancelVerification(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new VerificationCodeException("No phone number to cancel verification for");
        }

        verificationCodeRepository.invalidateAllForUser(userId);
        userRepository.clearPhoneVerifiedAt(userId);
        user.setPhoneVerifiedAt(null);

        return new UserProfileResponse(
            user.getIdentifier(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhone(),
            false
        );
    }

    private void createAndSendCode(User user, String code) {
        int expiryMinutes = phoneNumberPolicyService.getPolicy().getVerificationCodeExpiryMinutes();

        PhoneVerificationCode verificationCode = new PhoneVerificationCode();
        verificationCode.setUserId(user.getId());
        verificationCode.setPhone(user.getPhone());
        verificationCode.setCode(code);
        verificationCode.setExpiresAt(clock.instant().plus(expiryMinutes, MINUTES));
        verificationCodeRepository.save(verificationCode);

        SendNotificationRequest sendNotificationRequest = SendNotificationRequest.builder()
                .teamId(user.getActiveTeamId())
                .notificationType(PHONE_VERIFICATION_CODE)
                .recipientUserId(user.getId())
                .recipientPhone(user.getPhone())
                .templateName("phone-verification-code")
                .templateVariables(Map.of(
                        "userName", user.getFirstName(),
                        "verificationCode", code,
                        "expiresMinutes", expiryMinutes
                ))
                .build();

        notificationService.send(sendNotificationRequest);

        log.info("Phone verification code sent to user: {}", user.getIdentifier());
    }

    private String generateVerificationCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}
