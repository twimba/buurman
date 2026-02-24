package com.buurman.dto.request.backoffice;

import java.util.Optional;

public record SetLogLevelRequest(Optional<String> level) {}
