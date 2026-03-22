package com.buurman.dto.response;

import java.util.List;

import com.buurman.util.Generated;

@Generated
public record DuplicateCheckResponse(List<DuplicateMatch> matches) {}
