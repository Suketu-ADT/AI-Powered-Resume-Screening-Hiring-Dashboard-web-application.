package com.resume.screening.dto;

public record MatchDistributionResponse(
    String range,
    long count
) {}
