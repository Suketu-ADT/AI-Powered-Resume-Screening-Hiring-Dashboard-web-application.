package com.resume.screening.service;

import com.resume.screening.entity.Candidate;
import com.resume.screening.repository.CandidateRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CandidateService {

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.ai-service.url}")
    private String aiServiceUrl;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Transactional
    public Candidate saveAndParseCandidate(MultipartFile file) throws IOException {
        // 1. Create upload folder if it does not exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 2. Save file with a unique filename to prevent collisions
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            fileExtension = ".pdf";
        }
        
        String cleanName = originalFilename != null ? originalFilename.replace(fileExtension, "").replaceAll("[^a-zA-Z0-9_-]", "_") : "resume";
        String uniqueFilename = cleanName + "_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;
        Path targetLocation = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Accessible via WebConfig mapping /uploads/**
        String resumeUrl = "/uploads/" + uniqueFilename;

        // Default attributes in case AI parsing fails
        String name = originalFilename != null ? originalFilename.replace(fileExtension, "") : "Unknown Candidate";
        String email = "";
        String phone = "";
        String skills = "";
        Integer experienceYears = 0;
        String parsedText = "";

        // 3. Call FastAPI parsing microservice
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return originalFilename != null ? originalFilename : "resume.pdf";
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServiceUrl + "/ai/parse",
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> bodyMap = response.getBody();
                
                if (bodyMap.get("name") != null && !((String) bodyMap.get("name")).trim().isEmpty()) {
                    name = (String) bodyMap.get("name");
                }
                if (bodyMap.get("email") != null) {
                    email = (String) bodyMap.get("email");
                }
                if (bodyMap.get("phone") != null) {
                    phone = (String) bodyMap.get("phone");
                }
                if (bodyMap.get("experience_years") != null) {
                    experienceYears = ((Number) bodyMap.get("experience_years")).intValue();
                }
                if (bodyMap.get("parsed_text") != null) {
                    parsedText = (String) bodyMap.get("parsed_text");
                }
                if (bodyMap.get("skills") != null) {
                    Object skillsObj = bodyMap.get("skills");
                    if (skillsObj instanceof List) {
                        skills = String.join(", ", (List<String>) skillsObj);
                    } else if (skillsObj instanceof String) {
                        skills = (String) skillsObj;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("AI Service parser invocation failed: " + e.getMessage());
            // Fail gracefully, keep basic default settings derived from file name
        }

        Candidate candidate = new Candidate();
        candidate.setName(name);
        candidate.setEmail(email);
        candidate.setPhone(phone);
        candidate.setResumeUrl(resumeUrl);
        candidate.setParsedText(parsedText);
        candidate.setSkills(skills);
        candidate.setExperienceYears(experienceYears);

        return candidateRepository.save(candidate);
    }
    
    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }
    
    public Candidate getCandidateById(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found with ID: " + id));
    }
}
