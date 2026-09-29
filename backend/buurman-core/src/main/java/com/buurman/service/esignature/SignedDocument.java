package com.buurman.service.esignature;

public record SignedDocument(byte[] signedPdfBytes, byte[] certificatePdfBytes) {}
