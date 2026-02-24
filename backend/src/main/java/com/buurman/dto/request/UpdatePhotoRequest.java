package com.buurman.dto.request;

import java.util.Optional;

public record UpdatePhotoRequest(Optional<String> title, Optional<String> notes) {}
