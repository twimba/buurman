package com.buurman.dto.response;

import java.time.Instant;

import com.buurman.domain.Sid;

public record TeamResponse(Sid identifier, String teamName, long memberCount, Instant createdAt) {}
