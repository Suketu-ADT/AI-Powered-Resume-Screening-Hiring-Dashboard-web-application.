package com.resume.screening.service;

import com.resume.screening.dto.JobRequest;
import com.resume.screening.dto.JobResponse;
import com.resume.screening.entity.Job;
import com.resume.screening.entity.User;
import com.resume.screening.repository.JobRepository;
import com.resume.screening.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public JobResponse createJob(JobRequest request, String recruiterEmail) {
        User recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Recruiter not found: " + recruiterEmail));

        Job job = new Job();
        job.setTitle(request.title());
        job.setCompany(request.company());
        job.setDescription(request.description());
        job.setRequiredSkills(request.requiredSkills());
        job.setExperienceRequired(request.experienceRequired());
        job.setCreatedBy(recruiter);

        Job savedJob = jobRepository.save(job);
        return mapToResponse(savedJob);
    }

    public List<JobResponse> getAllJobs() {
        return jobRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public JobResponse getJobById(Long id) {
        Job job = getJobEntityById(id);
        return mapToResponse(job);
    }

    public Job getJobEntityById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + id));
    }

    private JobResponse mapToResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getDescription(),
                job.getRequiredSkills(),
                job.getExperienceRequired(),
                job.getCreatedBy().getEmail(),
                job.getCreatedAt()
        );
    }
}
