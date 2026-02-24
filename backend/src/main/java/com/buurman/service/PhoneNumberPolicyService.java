package com.buurman.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.PhoneNumberPolicy;
import com.buurman.exception.PhoneNumberPolicyException;
import com.buurman.repository.PhoneNumberPolicyRepository;
import com.buurman.util.CountryGroups;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PhoneNumberPolicyService {

  private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

  private final PhoneNumberPolicyRepository policyRepository;

  private volatile @Nullable PhoneNumberPolicy cachedPolicy;

  public PhoneNumberPolicy getPolicy() {
    PhoneNumberPolicy policy = cachedPolicy;
    if (policy != null) {
      return policy;
    }
    policy = policyRepository.findCurrent().orElse(defaultPolicy());
    cachedPolicy = policy;
    return policy;
  }

  @Transactional
  public PhoneNumberPolicy updatePolicy(PhoneNumberPolicy policy) {
    if (policy.getPolicyMatrix().isPresent()) {
      CountryGroups.validateMatrix(policy.getPolicyMatrix().get());
    }
    PhoneNumberPolicy saved = policyRepository.save(policy);
    cachedPolicy = saved;
    log.info("Phone number policy updated by {}", policy.getUpdatedBy().orElse("unknown"));
    return saved;
  }

  public void validate(String phone) {
    if (phone == null || phone.isBlank()) {
      return;
    }

    Phonenumber.PhoneNumber parsed;
    try {
      parsed = PHONE_UTIL.parse(phone, null);
    } catch (NumberParseException e) {
      throw new PhoneNumberPolicyException("Invalid phone number format");
    }

    String regionCode = PHONE_UTIL.getRegionCodeForNumber(parsed);
    PhoneNumberUtil.PhoneNumberType type = PHONE_UTIL.getNumberType(parsed);
    String typeStr = mapNumberType(type).orElse(null);

    PhoneNumberPolicy policy = getPolicy();

    if (!policy.isAllowed(Optional.ofNullable(regionCode), Optional.ofNullable(typeStr))) {
      // Determine specific error message
      if (regionCode == null
          || policy.getPolicyMatrix().isEmpty()
          || !policy.getPolicyMatrix().map(m -> m.containsKey(regionCode)).orElse(false)
          || policy.getPolicyMatrix().map(m -> m.get(regionCode)).map(List::isEmpty).orElse(true)) {
        throw new PhoneNumberPolicyException("Phone numbers from this country are not allowed");
      }
      throw new PhoneNumberPolicyException(
          "This phone number type (" + humanReadableType(typeStr) + ") is not allowed");
    }
  }

  private Optional<String> mapNumberType(PhoneNumberUtil.PhoneNumberType type) {
    return switch (type) {
      case FIXED_LINE -> Optional.of("FIXED_LINE");
      case MOBILE -> Optional.of("MOBILE");
      case FIXED_LINE_OR_MOBILE -> Optional.of("FIXED_LINE_OR_MOBILE");
      case TOLL_FREE -> Optional.of("TOLL_FREE");
      case PREMIUM_RATE -> Optional.of("PREMIUM_RATE");
      case SHARED_COST -> Optional.of("SHARED_COST");
      case VOIP -> Optional.of("VOIP");
      case PERSONAL_NUMBER -> Optional.of("PERSONAL_NUMBER");
      case PAGER -> Optional.of("PAGER");
      case UAN -> Optional.of("UAN");
      case VOICEMAIL -> Optional.of("VOICEMAIL");
      case UNKNOWN -> Optional.empty();
    };
  }

  private String humanReadableType(@Nullable String type) {
    if (type == null) {
      return "UNKNOWN";
    }

    return switch (type) {
      case "FIXED_LINE" -> "fixed line";
      case "MOBILE" -> "mobile";
      case "FIXED_LINE_OR_MOBILE" -> "fixed line or mobile";
      case "TOLL_FREE" -> "toll-free";
      case "PREMIUM_RATE" -> "premium rate";
      case "SHARED_COST" -> "shared cost";
      case "VOIP" -> "VoIP";
      case "PERSONAL_NUMBER" -> "personal number";
      case "PAGER" -> "pager";
      case "UAN" -> "universal access";
      case "VOICEMAIL" -> "voicemail";
      default -> type.toLowerCase(Locale.ROOT).replace('_', ' ');
    };
  }

  private PhoneNumberPolicy defaultPolicy() {
    PhoneNumberPolicy policy = new PhoneNumberPolicy();
    List<String> defaultTypes = List.of("MOBILE", "FIXED_LINE_OR_MOBILE");

    Map<String, List<String>> matrix = new HashMap<>();
    // EU countries
    for (String code :
        List.of(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE",
            "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE")) {
      matrix.put(code, defaultTypes);
    }
    // North America
    for (String code : List.of("US", "CA", "MX")) {
      matrix.put(code, defaultTypes);
    }

    policy.setPolicyMatrix(Optional.of(matrix));
    policy.setMaxCodesPerHour(3);
    policy.setVerificationCodeExpiryMinutes(10);
    return policy;
  }
}
