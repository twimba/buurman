package com.buurman.service.esignature;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A provider capable of collecting e-signatures on a PDF. The only implementation today is {@link
 * DocumensoClient}; a second provider would add another {@code @Component} implementing this
 * interface, never a change to callers.
 */
public interface SignatureProviderClient {

  /** Uploads the PDF, adds the given signers, and sends it for signature (all in parallel). */
  SignatureSubmission createSubmission(
      byte[] pdfBytes, String fileName, List<SignerRequest> signers);

  /** Fetches the final signed PDF and its audit certificate once every signer has signed. */
  SignedDocument downloadCompleted(String providerSubmissionId);

  /** Cancels a pending submission so its signers can no longer sign it. */
  void cancelSubmission(String providerSubmissionId, @Nullable String reason);

  /** Validates a webhook's shared-secret header against the configured secret. */
  boolean isValidWebhookSecret(@Nullable String providedSecret);
}
