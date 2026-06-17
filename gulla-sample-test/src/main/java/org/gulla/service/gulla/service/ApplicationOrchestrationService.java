package org.gulla.service.gulla.service;

import jakarta.persistence.EntityNotFoundException;
import org.gulla.service.gulla.config.EasyApplyConfigProperties;
import org.gulla.service.gulla.dto.ApplyResponse;
import org.gulla.service.gulla.exception.InvalidCredentialsException;
import org.gulla.service.gulla.exception.ResourceNotFoundException;
import org.gulla.service.gulla.model.ApplicationRecord;
import org.gulla.service.gulla.model.JobPosting;
import org.gulla.service.gulla.model.ResumeProfile;
import org.gulla.service.gulla.repository.ApplicationRecordRepository;
import org.gulla.service.gulla.repository.JobPostingRepository;
import org.gulla.service.gulla.repository.ResumeProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationOrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationOrchestrationService.class);
    private static final String STATUS_MATCHED = "MATCHED";
    private static final String STATUS_SKIPPED_LOW_MATCH = "SKIPPED_LOW_MATCH";
    private static final String STATUS_APPLIED = "APPLIED";

    private final ResumeProfileRepository resumeRepository;
    private final JobPostingRepository jobRepository;
    private final ApplicationRecordRepository applicationRepository;
    private final ResumeMatchingService resumeMatchingService;
    private final LinkedInEasyApplyAutomationService automationService;
    private final EasyApplyConfigProperties config;

    public ApplicationOrchestrationService(
            ResumeProfileRepository resumeRepository,
            JobPostingRepository jobRepository,
            ApplicationRecordRepository applicationRepository,
            ResumeMatchingService resumeMatchingService,
            LinkedInEasyApplyAutomationService automationService,
            EasyApplyConfigProperties config) {
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.resumeMatchingService = resumeMatchingService;
        this.automationService = automationService;
        this.config = config;
    }

    @Transactional
    public ApplyResponse evaluateAndApply(Long resumeId, Long jobId, boolean autoApply, String email, String password, boolean headless) {
        logger.info("Evaluating application: resumeId={}, jobId={}, autoApply={}", resumeId, jobId, autoApply);
        
        ResumeProfile resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));
        JobPosting job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Posting", jobId));

        ResumeMatchResult matchResult = resumeMatchingService.score(resume, job);
        logger.debug("Match score calculated: {} for resume {} and job {}", matchResult.score(), resumeId, jobId);

        String status = matchResult.score() >= config.getMatchThreshold() ? STATUS_MATCHED : STATUS_SKIPPED_LOW_MATCH;

        if (autoApply && STATUS_MATCHED.equals(status)) {
            String resolvedEmail = (email == null || email.isBlank()) ? System.getenv("LINKEDIN_EMAIL") : email;
            String resolvedPassword = (password == null || password.isBlank()) ? System.getenv("LINKEDIN_PASSWORD") : password;

            if (resolvedEmail == null || resolvedEmail.isBlank()) {
                logger.error("LinkedIn email not provided for auto-apply");
                throw new InvalidCredentialsException("LinkedIn email is required when autoApply is true");
            }
            if (resolvedPassword == null || resolvedPassword.isBlank()) {
                logger.error("LinkedIn password not provided for auto-apply");
                throw new InvalidCredentialsException("LinkedIn password is required when autoApply is true");
            }
            
            logger.info("Initiating auto-apply for job {} with resume {}", jobId, resumeId);
            automationService.apply(job, resume, resolvedEmail, resolvedPassword, headless);
            status = STATUS_APPLIED;
            logger.info("Auto-apply completed successfully for job {} with resume {}", jobId, resumeId);
        }

        ApplicationRecord record = new ApplicationRecord();
        record.setResume(resume);
        record.setJob(job);
        record.setMatchScore(matchResult.score());
        record.setMatchSummary(matchResult.summary());
        record.setStatus(status);
        ApplicationRecord saved = applicationRepository.save(record);

        logger.info("Application record created: id={}, status={}, matchScore={}", saved.getId(), status, matchResult.score());
        return new ApplyResponse(saved.getId(), matchResult.score(), matchResult.summary(), status);
    }
}
