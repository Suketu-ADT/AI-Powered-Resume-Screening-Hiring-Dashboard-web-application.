package com.resume.screening.repository;

import com.resume.screening.entity.Application;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByJobIdOrderByMatchScoreDesc(Long jobId);
    List<Application> findByJobId(Long jobId);
    List<Application> findAllByOrderByMatchScoreDesc();
    long countByStatus(String status);
}
