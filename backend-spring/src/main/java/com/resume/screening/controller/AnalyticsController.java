package com.resume.screening.controller;

import com.resume.screening.dto.DashboardSummaryResponse;
import com.resume.screening.dto.MatchDistributionResponse;
import com.resume.screening.dto.SkillAnalyticsResponse;
import com.resume.screening.service.AnalyticsService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(analyticsService.getDashboardSummary());
    }

    @GetMapping("/skills")
    public ResponseEntity<List<SkillAnalyticsResponse>> getSkills() {
        return ResponseEntity.ok(analyticsService.getSkillAnalytics());
    }

    @GetMapping("/distribution")
    public ResponseEntity<List<MatchDistributionResponse>> getDistribution() {
        return ResponseEntity.ok(analyticsService.getMatchDistribution());
    }
}
