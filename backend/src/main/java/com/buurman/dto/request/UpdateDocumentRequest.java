package com.buurman.dto.request;

import java.util.Optional;

public record UpdateDocumentRequest(Optional<String> title, Optional<String> notes) {}
