package com.resume.screening.service;

import com.resume.screening.dto.ApplicationResponse;
import com.resume.screening.dto.DashboardSummaryResponse;
import com.resume.screening.dto.MatchDistributionResponse;
import com.resume.screening.dto.SkillAnalyticsResponse;
import com.resume.screening.entity.Application;
import com.resume.screening.repository.ApplicationRepository;
import com.resume.screening.repository.CandidateRepository;
import com.resume.screening.repository.JobRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationService applicationService;

    public DashboardSummaryResponse getDashboardSummary() {
        long totalCandidates = candidateRepository.count();
        long totalJobs = jobRepository.count();
        long shortlisted = applicationRepository.countByStatus("SHORTLISTED");
        long rejected = applicationRepository.countByStatus("REJECTED");

        List<ApplicationResponse> recentApplications = applicationRepository.findAllByOrderByAppliedAtDesc().stream()
                .limit(10)
                .map(applicationService::mapToResponse)
                .collect(Collectors.toList());

        return new DashboardSummaryResponse(
                totalCandidates,
                totalJobs,
                shortlisted,
                rejected,
                recentApplications
        );
    }

    public List<SkillAnalyticsResponse> getSkillAnalytics() {
        Map<String, Long> skillCounts = new HashMap<>();

        candidateRepository.findAll().forEach(candidate -> {
            String skillsStr = candidate.getSkills();
            if (skillsStr != null && !skillsStr.trim().isEmpty()) {
                Arrays.stream(skillsStr.split(","))
                        .map(String::trim)
                        .filter(skill -> !skill.isEmpty())
                        .forEach(skill -> {
                            // Normalize skill name to Title Case for nice UI grouping
                            String normalizedSkill = toTitleCase(skill);
                            skillCounts.put(normalizedSkill, skillCounts.getOrDefault(normalizedSkill, 0L) + 1);
                        });
            }
        });

        return skillCounts.entrySet().stream()
                .map(entry -> new SkillAnalyticsResponse(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Long.compare(b.count(), a.count())) // Sort descending
                .limit(15) // Top 15 skills
                .collect(Collectors.toList());
    }

    public List<MatchDistributionResponse> getMatchDistribution() {
        long[] counts = new long[5]; // ranges: 0-20, 21-40, 41-60, 61-80, 81-100

        List<Application> apps = applicationRepository.findAll();
        for (Application app : apps) {
            double score = app.getMatchScore();
            if (score <= 20.0) {
                counts[0]++;
            } else if (score <= 40.0) {
                counts[1]++;
            } else if (score <= 60.0) {
                counts[2]++;
            } else if (score <= 80.0) {
                counts[3]++;
            } else {
                counts[4]++;
            }
        }

        List<MatchDistributionResponse> distribution = new ArrayList<>();
        distribution.add(new MatchDistributionResponse("0-20%", counts[0]));
        distribution.add(new MatchDistributionResponse("21-40%", counts[1]));
        distribution.add(new MatchDistributionResponse("41-60%", counts[2]));
        distribution.add(new MatchDistributionResponse("61-80%", counts[3]));
        distribution.add(new MatchDistributionResponse("81-100%", counts[4]));

        return distribution;
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        // Handle common short abbreviations in uppercase
        String upper = input.toUpperCase();
        if (upper.equals("API") || upper.equals("SQL") || upper.equals("AWS") || upper.equals("JVM") || 
            upper.equals("CSS") || upper.equals("HTML") || upper.equals("REST") || upper.equals("MVC") || 
            upper.equals("NLP") || upper.equals("AI") || upper.equals("NER") || upper.equals("PDF") || 
            upper.equals("JWT") || upper.equals("JPA") || upper.equals("UI") || upper.equals("UX")) {
            return upper;
        }

        return Arrays.stream(input.split("\\s+"))
                .map(word -> word.isEmpty() ? "" : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
