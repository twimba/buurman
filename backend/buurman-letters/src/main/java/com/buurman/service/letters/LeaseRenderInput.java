package com.buurman.service.letters;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.buurman.domain.Contract;
import com.buurman.domain.RentComponentType;
import com.buurman.domain.metadata.ContractCountryMetadata;
import com.buurman.util.MoneyAmount;

/**
 * Plain, immutable values a lease agreement is rendered from. {@link
 * LeaseAgreementExporter#loadInput} builds it from a real contract (the only place that touches
 * repositories); a synthetic caller can build one by hand, so rendering needs no contract.
 *
 * <p>{@code landlordName} and {@code tenantNames} are only needed by the typed per-language
 * document path; they stay empty on the legacy path, which never reads them. The typed path
 * requires both and fails with a clear {@link IllegalStateException} when either is empty.
 *
 * @param regionCode the contract's region; the {@code regionCode} template variable of the
 *     per-language document path (null when absent)
 * @param baseRent the net rent ("kale huur"), already derived from the components
 * @param deposit already resolved by {@code depositAmount} then {@code securityDeposit} precedence
 * @param signatureBlocks landlord first, then one block per tenant signer
 */
public record LeaseRenderInput(
    String contractIdentifier,
    LocalDate today,
    Optional<String> regionCode,
    LocalDate startDate,
    Optional<LocalDate> endDate,
    Contract.ContractType contractType,
    Contract.PaymentFrequency paymentFrequency,
    int landlordNoticeDays,
    int tenantNoticeDays,
    List<RentLine> rentComponents,
    MoneyAmount baseRent,
    Optional<MoneyAmount> deposit,
    Optional<Integer> paymentDueDay,
    Optional<ContractCountryMetadata> countryMetadata,
    Optional<String> landlordName,
    Optional<String> tenantNames,
    Optional<String> primaryContactName,
    Optional<Map<String, String>> contactAddress,
    String propertyAddress,
    List<Map<String, String>> signatureBlocks) {

  /**
   * Defensive immutability. The address map is wrapped, not copied with {@code Map.copyOf}: address
   * fields can be null, which {@code Map.copyOf} rejects.
   */
  public LeaseRenderInput {
    rentComponents = List.copyOf(rentComponents);
    signatureBlocks = List.copyOf(signatureBlocks);
    contactAddress = contactAddress.map(Collections::unmodifiableMap);
  }

  /** One rent component; the label is localized when the variables are built. */
  public record RentLine(RentComponentType type, MoneyAmount amount) {}
}
