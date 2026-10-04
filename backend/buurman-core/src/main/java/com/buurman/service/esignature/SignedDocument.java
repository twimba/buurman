package com.buurman.service.esignature;

@SuppressWarnings("ArrayRecordComponent")
public record SignedDocument(byte[] signedPdfBytes, byte[] certificatePdfBytes) {}
