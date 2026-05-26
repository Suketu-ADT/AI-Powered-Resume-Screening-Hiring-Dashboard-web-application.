package com.resume.screening.dto;

public record AuthResponse(
    String token,
    String name,
    String email,
    String role
) {}
