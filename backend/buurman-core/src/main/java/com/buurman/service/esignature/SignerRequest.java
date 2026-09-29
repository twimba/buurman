package com.buurman.service.esignature;

import com.buurman.domain.SignatureSignerRole;

public record SignerRequest(String email, String name, SignatureSignerRole role) {}
