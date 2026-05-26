package com.resume.screening.dto;

import java.time.LocalDateTime;

public record JobResponse(
    Long id,
    String title,
    String company,
    String description,
    String requiredSkills,
    Integer experienceRequired,
    String createdByEmail,
    LocalDateTime createdAt
) {}
