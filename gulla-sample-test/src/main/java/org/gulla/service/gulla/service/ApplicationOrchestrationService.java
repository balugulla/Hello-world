package org.gulla.service.gulla.service;

import jakarta.persistence.EntityNotFoundException;
import org.gulla.service.gulla.dto.ApplyResponse;
import org.gulla.service.gulla.model.ApplicationRecord;
import org.gulla.service.gulla.model.JobPosting;
import org.gulla.service.gulla.model.ResumeProfile;
import org.gulla.service.gulla.repository.ApplicationRecordRepository;
import org.gulla.service.gulla.repository.JobPostingRepository;
import org.gulla.service.gulla.repository.ResumeProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationOrchestrationService {

    private final ResumeProfileRepository resumeRepository;
    private final JobPostingRepository jobRepository;
    private final ApplicationRecordRepository applicationRepository;
    private final ResumeMatchingService resumeMatchingService;
    private final LinkedInEasyApplyAutomationService automationService;
    private final double autoApplyThreshold;

    public ApplicationOrchestrationService(
            ResumeProfileRepository resumeRepository,
            JobPostingRepository jobRepository,
            ApplicationRecordRepository applicationRepository,
            ResumeMatchingService resumeMatchingService,
            LinkedInEasyApplyAutomationService automationService,
            @Value("${easyapply.match-threshold:0.70}") double autoApplyThreshold) {
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.resumeMatchingService = resumeMatchingService;
        this.automationService = automationService;
        this.autoApplyThreshold = autoApplyThreshold;
    }

    @Transactional
    public ApplyResponse evaluateAndApply(Long resumeId, Long jobId, boolean autoApply, String email, String password, boolean headless) {
        ResumeProfile resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new EntityNotFoundException("Resume not found: " + resumeId));
        JobPosting job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job posting not found: " + jobId));

        ResumeMatchResult matchResult = resumeMatchingService.score(resume, job);

        String status = matchResult.score() >= autoApplyThreshold ? "MATCHED" : "SKIPPED_LOW_MATCH";

        if (autoApply && "MATCHED".equals(status)) {
            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                throw new IllegalArgumentException("LinkedIn credentials are required when autoApply is true");
            }
            automationService.apply(job, resume, email, password, headless);
            status = "APPLIED";
        }

        ApplicationRecord record = new ApplicationRecord();
        record.setResume(resume);
        record.setJob(job);
        record.setMatchScore(matchResult.score());
        record.setMatchSummary(matchResult.summary());
        record.setStatus(status);
        ApplicationRecord saved = applicationRepository.save(record);

        return new ApplyResponse(saved.getId(), matchResult.score(), matchResult.summary(), status);
    }
}
