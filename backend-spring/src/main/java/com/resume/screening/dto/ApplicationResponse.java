package com.resume.screening.dto;

import java.time.LocalDateTime;

public record ApplicationResponse(
    Long id,
    Long candidateId,
    String candidateName,
    String candidateEmail,
    String candidatePhone,
    String resumeUrl,
    String skills,
    Integer experienceYears,
    Long jobId,
    String jobTitle,
    Double matchScore,
    String aiSummary,
    String status,
    LocalDateTime appliedAt
) {}
