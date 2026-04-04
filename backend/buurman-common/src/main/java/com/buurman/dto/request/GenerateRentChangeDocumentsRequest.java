package com.buurman.dto.request;

import java.util.List;
import java.util.Optional;

public record GenerateRentChangeDocumentsRequest(
    Optional<List<String>> languages, Optional<Boolean> replaceExisting) {}
