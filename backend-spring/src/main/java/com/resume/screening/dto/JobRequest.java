package com.resume.screening.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JobRequest(
    @NotBlank(message = "Job title is required")
    String title,
    
    @NotBlank(message = "Company name is required")
    String company,
    
    @NotBlank(message = "Description is required")
    String description,
    
    @NotBlank(message = "Required skills are required")
    String requiredSkills,
    
    @NotNull(message = "Required experience is required")
    @Min(value = 0, message = "Experience cannot be negative")
    Integer experienceRequired
) {}
