package com.resume.screening.config;

import com.resume.screening.entity.Application;
import com.resume.screening.entity.Candidate;
import com.resume.screening.entity.Job;
import com.resume.screening.entity.User;
import com.resume.screening.repository.ApplicationRepository;
import com.resume.screening.repository.CandidateRepository;
import com.resume.screening.repository.JobRepository;
import com.resume.screening.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Seed if no users exist
        if (userRepository.count() == 0) {
            System.out.println("No users found in database. Starting database seeding...");

            // 1. Seed Recruiter
            User recruiter = new User();
            recruiter.setName("Demo Recruiter");
            recruiter.setEmail("recruiter@example.com");
            recruiter.setPassword(passwordEncoder.encode("password"));
            recruiter.setRole("ROLE_RECRUITER");
            userRepository.save(recruiter);
            System.out.println("Seeded recruiter: recruiter@example.com / password");

            // 2. Create local uploads directory and placeholder files for demo files
            createUploadsPlaceholderFolder();

            // 3. Seed Jobs
            Job javaJob = new Job();
            javaJob.setTitle("Senior Java Developer");
            javaJob.setCompany("TechCorp Solutions");
            javaJob.setDescription("We are looking for a Senior Java Developer to lead our core payment systems team. You will write clean, scalable Spring Boot applications, manage PostgreSQL databases, and run Docker-based microservices. The candidate should have hands-on experience with Hibernate and RESTful services.");
            javaJob.setRequiredSkills("Java, Spring Boot, PostgreSQL, Docker, REST, Hibernate");
            javaJob.setExperienceRequired(5);
            javaJob.setCreatedBy(recruiter);
            jobRepository.save(javaJob);

            Job aiJob = new Job();
            aiJob.setTitle("AI NLP Research Engineer");
            aiJob.setCompany("DeepMind Labs");
            aiJob.setDescription("We are looking for an AI NLP Engineer to build and fine-tune language models. You will implement NLP parsing engines and build high-performance pipelines with FastAPI, spaCy, and PyTorch. Experience with transformer architectures is required.");
            aiJob.setRequiredSkills("Python, PyTorch, Transformers, NLP, spaCy, FastAPI");
            aiJob.setExperienceRequired(3);
            aiJob.setCreatedBy(recruiter);
            jobRepository.save(aiJob);

            Job frontendJob = new Job();
            frontendJob.setTitle("Frontend Developer (React/Next.js)");
            frontendJob.setCompany("DesignStudio");
            frontendJob.setDescription("Join our design-focused product engineering team as a Frontend Developer. You will construct highly responsive user interfaces using Next.js, React, Tailwind CSS, TypeScript, and clean modern HTML/CSS. Responsive design and micro-animation expertise is highly appreciated.");
            frontendJob.setRequiredSkills("React, Next.js, JavaScript, Tailwind CSS, TypeScript, HTML");
            frontendJob.setExperienceRequired(2);
            frontendJob.setCreatedBy(recruiter);
            jobRepository.save(frontendJob);

            System.out.println("Seeded 3 jobs.");

            // 4. Seed Candidates
            Candidate alice = new Candidate();
            alice.setName("Alice Johnson");
            alice.setEmail("alice.johnson@example.com");
            alice.setPhone("+1 (555) 019-2834");
            alice.setResumeUrl("/uploads/alice_johnson_resume.pdf");
            alice.setParsedText("Alice Johnson. Experience: 6 years. Worked as Senior Java Engineer at FinTech Corp. Skills: Java, Spring Boot, Hibernate, REST, PostgreSQL, Docker, AWS, Git. Expert in backend microservice design.");
            alice.setSkills("Java, Spring Boot, Hibernate, REST, PostgreSQL, Docker, AWS, Git");
            alice.setExperienceYears(6);
            candidateRepository.save(alice);

            Candidate bob = new Candidate();
            bob.setName("Bob Smith");
            bob.setEmail("bob.smith@example.com");
            bob.setPhone("+1 (555) 018-9922");
            bob.setResumeUrl("/uploads/bob_smith_resume.pdf");
            bob.setParsedText("Bob Smith. AI Engineer with 4 years of experience. Python developer. Trained custom SpaCy pipelines and built REST APIs with FastAPI. Skills: Python, PyTorch, NLP, spaCy, FastAPI, Git, Docker. Experience in deep learning and transformers.");
            bob.setSkills("Python, PyTorch, NLP, spaCy, FastAPI, Git, Docker");
            bob.setExperienceYears(4);
            candidateRepository.save(bob);

            Candidate charlie = new Candidate();
            charlie.setName("Charlie Brown");
            charlie.setEmail("charlie.brown@example.com");
            charlie.setPhone("+1 (555) 017-3844");
            charlie.setResumeUrl("/uploads/charlie_brown_resume.pdf");
            charlie.setParsedText("Charlie Brown. Junior Frontend Developer. 1 year experience building web pages with React and Tailwind CSS. Skills: React, JavaScript, HTML, CSS, Tailwind CSS. Focuses on accessibility and performance.");
            charlie.setSkills("React, JavaScript, HTML, CSS, Tailwind CSS");
            charlie.setExperienceYears(1);
            candidateRepository.save(charlie);

            System.out.println("Seeded 3 candidates.");

            // 5. Seed Applications (linking candidate + job + match details)
            Application app1 = new Application();
            app1.setCandidate(alice);
            app1.setJob(javaJob);
            app1.setMatchScore(95.0);
            app1.setStatus("SHORTLISTED");
            app1.setAiSummary("Excellent match. Exceeds the 5 years experience requirement with 6 years of experience. Possesses all critical requested skills including Java, Spring Boot, PostgreSQL, Docker, REST, and Hibernate. Highly recommended for immediate interview.");
            applicationRepository.save(app1);

            Application app2 = new Application();
            app2.setCandidate(bob);
            app2.setJob(aiJob);
            app2.setMatchScore(88.0);
            app2.setStatus("UNDER_REVIEW");
            app2.setAiSummary("Strong candidate. Has 4 years of experience (exceeding the required 3 years). Matches almost all key technical requirements (Python, PyTorch, NLP, spaCy, FastAPI, Docker). Candidate lacks explicit 'Transformers' keyword matching but shows clear deep learning work.");
            applicationRepository.save(app2);

            Application app3 = new Application();
            app3.setCandidate(charlie);
            app3.setJob(frontendJob);
            app3.setMatchScore(72.0);
            app3.setStatus("APPLIED");
            app3.setAiSummary("Decent fit. Displays good frontend expertise (React, JavaScript, HTML, CSS, Tailwind CSS). However, has only 1 year of experience compared to the 2 years required. Worth considering for an entry-level or associate role.");
            applicationRepository.save(app3);

            System.out.println("Seeded 3 applications.");
            System.out.println("Database seeding completed successfully!");
        } else {
            System.out.println("Database already contains records. Skipping seeding.");
        }
    }

    private void createUploadsPlaceholderFolder() {
        try {
            Path uploadsPath = Paths.get("uploads");
            if (!Files.exists(uploadsPath)) {
                Files.createDirectories(uploadsPath);
                System.out.println("Created local uploads/ directory.");
            }
            // Write tiny mock files so PDF viewer endpoints don't return 404 in local testing
            Files.writeString(uploadsPath.resolve("alice_johnson_resume.pdf"), "%PDF-1.4 Mock PDF for Alice Johnson");
            Files.writeString(uploadsPath.resolve("bob_smith_resume.pdf"), "%PDF-1.4 Mock PDF for Bob Smith");
            Files.writeString(uploadsPath.resolve("charlie_brown_resume.pdf"), "%PDF-1.4 Mock PDF for Charlie Brown");
        } catch (IOException e) {
            System.err.println("Could not create uploads directory mock resumes: " + e.getMessage());
        }
    }
}
