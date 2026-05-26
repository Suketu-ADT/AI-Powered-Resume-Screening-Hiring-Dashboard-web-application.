package com.resume.screening.dto;

import java.util.List;

public record DashboardSummaryResponse(
    long totalCandidates,
    long totalJobs,
    long shortlisted,
    long rejected,
    List<ApplicationResponse> recentApplications
) {}
