package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.Generated;

@Generated
public record UpdateDocumentRequest(Optional<String> title, Optional<String> notes) {}
