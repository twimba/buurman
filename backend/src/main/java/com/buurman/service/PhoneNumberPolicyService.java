package com.buurman.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  private volatile PhoneNumberPolicy cachedPolicy;

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
    CountryGroups.validateMatrix(policy.getPolicyMatrix());
    PhoneNumberPolicy saved = policyRepository.save(policy);
    cachedPolicy = saved;
    log.info("Phone number policy updated by {}", policy.getUpdatedBy());
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
    String typeStr = mapNumberType(type);

    PhoneNumberPolicy policy = getPolicy();

    if (!policy.isAllowed(regionCode, typeStr)) {
      // Determine specific error message
      if (regionCode == null
          || policy.getPolicyMatrix() == null
          || !policy.getPolicyMatrix().containsKey(regionCode)
          || policy.getPolicyMatrix().get(regionCode) == null
          || policy.getPolicyMatrix().get(regionCode).isEmpty()) {
        throw new PhoneNumberPolicyException("Phone numbers from this country are not allowed");
      }
      throw new PhoneNumberPolicyException(
          "This phone number type (" + humanReadableType(typeStr) + ") is not allowed");
    }
  }

  private String mapNumberType(PhoneNumberUtil.PhoneNumberType type) {
    return switch (type) {
      case FIXED_LINE -> "FIXED_LINE";
      case MOBILE -> "MOBILE";
      case FIXED_LINE_OR_MOBILE -> "FIXED_LINE_OR_MOBILE";
      case TOLL_FREE -> "TOLL_FREE";
      case PREMIUM_RATE -> "PREMIUM_RATE";
      case SHARED_COST -> "SHARED_COST";
      case VOIP -> "VOIP";
      case PERSONAL_NUMBER -> "PERSONAL_NUMBER";
      case PAGER -> "PAGER";
      case UAN -> "UAN";
      case VOICEMAIL -> "VOICEMAIL";
      case UNKNOWN -> "UNKNOWN";
    };
  }

  private String humanReadableType(String type) {
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
      default -> type.toLowerCase().replace('_', ' ');
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

    policy.setPolicyMatrix(matrix);
    policy.setMaxCodesPerHour(3);
    policy.setVerificationCodeExpiryMinutes(10);
    return policy;
  }
}
