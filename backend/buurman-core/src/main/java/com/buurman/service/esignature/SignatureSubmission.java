package com.buurman.service.esignature;

import java.util.List;

public record SignatureSubmission(String providerSubmissionId, List<ProviderSigner> signers) {}
