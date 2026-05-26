package com.resume.screening.service;

import com.resume.screening.dto.ApplicationResponse;
import com.resume.screening.entity.Application;
import com.resume.screening.entity.Candidate;
import com.resume.screening.entity.Job;
import com.resume.screening.repository.ApplicationRepository;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private JobService jobService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.ai-service.url}")
    private String aiServiceUrl;

    @Transactional
    public ApplicationResponse createApplication(Long jobId, MultipartFile file) throws IOException {
        // 1. Upload & Parse Candidate
        Candidate candidate = candidateService.saveAndParseCandidate(file);

        // 2. Fetch Job details
        Job job = jobService.getJobEntityById(jobId);

        // 3. Perform Match
        double matchScore = 0.0;
        String aiSummary = "No analysis available.";

        List<String> requiredSkills = splitSkills(job.getRequiredSkills());
        List<String> candidateSkills = splitSkills(candidate.getSkills());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = new HashMap<>();
            payload.put("resume_text", candidate.getParsedText() != null ? candidate.getParsedText() : "");
            payload.put("job_description", job.getDescription());
            payload.put("required_skills", requiredSkills);
            payload.put("candidate_skills", candidateSkills);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServiceUrl + "/ai/match",
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> bodyMap = response.getBody();
                if (bodyMap.get("match_score") != null) {
                    matchScore = ((Number) bodyMap.get("match_score")).doubleValue();
                }
                if (bodyMap.get("ai_summary") != null) {
                    aiSummary = (String) bodyMap.get("ai_summary");
                }
            }
        } catch (Exception e) {
            System.err.println("AI Service match invocation failed: " + e.getMessage());
            // Fallback match calculation in Java
            matchScore = calculateFallbackScore(candidateSkills, requiredSkills);
            aiSummary = "Fallbacked to local matching. Found matching skills: " 
                + String.join(", ", findCommonSkills(candidateSkills, requiredSkills))
                + ". Complete AI Service summary was skipped due to offline status.";
        }

        // 4. Save application
        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setMatchScore(matchScore);
        application.setAiSummary(aiSummary);
        application.setStatus("APPLIED");

        Application savedApp = applicationRepository.save(application);
        return mapToResponse(savedApp);
    }

    public List<ApplicationResponse> getApplicationsByJobId(Long jobId) {
        return applicationRepository.findByJobIdOrderByMatchScoreDesc(jobId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ApplicationResponse> getAllApplications() {
        return applicationRepository.findAllByOrderByMatchScoreDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(Long id, String status) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found with ID: " + id));
        
        application.setStatus(status.toUpperCase());
        Application saved = applicationRepository.save(application);
        return mapToResponse(saved);
    }

    private List<String> splitSkills(String skillsStr) {
        if (skillsStr == null || skillsStr.trim().isEmpty()) {
            return List.of();
        }
        return List.of(skillsStr.split(",\\s*"));
    }

    private double calculateFallbackScore(List<String> candidateSkills, List<String> requiredSkills) {
        if (requiredSkills.isEmpty()) {
            return 100.0;
        }
        List<String> common = findCommonSkills(candidateSkills, requiredSkills);
        return ((double) common.size() / requiredSkills.size()) * 100.0;
    }

    private List<String> findCommonSkills(List<String> candidateSkills, List<String> requiredSkills) {
        List<String> candidateLower = candidateSkills.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        return requiredSkills.stream()
                .filter(reqSkill -> candidateLower.contains(reqSkill.toLowerCase()))
                .collect(Collectors.toList());
    }

    public ApplicationResponse mapToResponse(Application app) {
        return new ApplicationResponse(
                app.getId(),
                app.getCandidate().getId(),
                app.getCandidate().getName(),
                app.getCandidate().getEmail(),
                app.getCandidate().getPhone(),
                app.getCandidate().getResumeUrl(),
                app.getCandidate().getSkills(),
                app.getCandidate().getExperienceYears(),
                app.getJob().getId(),
                app.getJob().getTitle(),
                app.getMatchScore(),
                app.getAiSummary(),
                app.getStatus(),
                app.getAppliedAt()
        );
    }
}
