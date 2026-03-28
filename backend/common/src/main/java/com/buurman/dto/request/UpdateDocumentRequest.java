package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record UpdateDocumentRequest(Optional<String> title, Optional<String> notes) {}
