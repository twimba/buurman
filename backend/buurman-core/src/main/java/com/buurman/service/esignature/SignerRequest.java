package com.buurman.service.esignature;

import com.buurman.domain.SignatureSignerRole;

/**
 * {@code placeholder} is the literal text (e.g. {@code "signature-landlord"}) the document's PDF
 * must contain for this signer — {@link DocumensoClient} locates it via Documenso's PDF-text
 * placeholder matching to place that signer's signature field. It must agree with whatever text the
 * document's own template rendered for this signer; see {@code LeaseAgreementExporter} and {@code
 * SignatureService} for the shared "landlord" / "tenant-N" naming convention.
 */
public record SignerRequest(
    String email, String name, SignatureSignerRole role, String placeholder) {}
