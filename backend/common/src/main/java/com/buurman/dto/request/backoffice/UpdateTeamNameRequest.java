package com.buurman.dto.request.backoffice;

import com.buurman.util.Generated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Generated
public record UpdateTeamNameRequest(@NotBlank @Size(min = 1, max = 100) String name) {}
