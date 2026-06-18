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
    private static final String STATUS_AI_ANALYSIS_SKIPPED = "AI_ANALYSIS_SKIPPED";

    private final ResumeProfileRepository resumeRepository;
    private final JobPostingRepository jobRepository;
    private final ApplicationRecordRepository applicationRepository;
    private final ResumeMatchingService resumeMatchingService;
    private final LinkedInEasyApplyAutomationService automationService;
    private final CandidateProfileService candidateProfileService;
    private final EasyApplyConfigProperties config;

    public ApplicationOrchestrationService(
            ResumeProfileRepository resumeRepository,
            JobPostingRepository jobRepository,
            ApplicationRecordRepository applicationRepository,
            ResumeMatchingService resumeMatchingService,
            LinkedInEasyApplyAutomationService automationService,
            CandidateProfileService candidateProfileService,
            EasyApplyConfigProperties config) {
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.resumeMatchingService = resumeMatchingService;
        this.automationService = automationService;
        this.candidateProfileService = candidateProfileService;
        this.config = config;
    }

    @Transactional
    public ApplyResponse evaluateAndApply(Long resumeId, Long jobId, boolean autoApply, String email, String password, Boolean headless, Boolean aiAnalysis) {
        logger.info("Evaluating application: resumeId={}, jobId={}, autoApply={}", resumeId, jobId, autoApply);
        
        ResumeProfile resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));
        JobPosting job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Posting", jobId));
        CandidateProfileService.CandidateExecutionContext candidateContext =
                candidateProfileService.resolveExecutionContext(resume, email, password, headless, aiAnalysis);

        double matchScore;
        String matchSummary;
        String status;

        if (candidateContext.aiAnalysis()) {
            ResumeMatchResult matchResult = resumeMatchingService.score(resume, job);
            logger.debug("Match score calculated: {} for resume {} and job {}", matchResult.score(), resumeId, jobId);
            matchScore = matchResult.score();
            matchSummary = matchResult.summary();
            status = matchScore >= config.getMatchThreshold() ? STATUS_MATCHED : STATUS_SKIPPED_LOW_MATCH;
        } else {
            logger.info("AI analysis disabled — skipping resume matching for resume {} and job {}", resumeId, jobId);
            matchScore = 0.0;
            matchSummary = "AI analysis skipped";
            status = STATUS_AI_ANALYSIS_SKIPPED;
        }

        if (autoApply && (STATUS_MATCHED.equals(status) || STATUS_AI_ANALYSIS_SKIPPED.equals(status))) {
            String resolvedEmail = candidateContext.linkedinEmail();
            String resolvedPassword = candidateContext.linkedinPassword();

            if (resolvedEmail == null || resolvedEmail.isBlank()) {
                resolvedEmail = System.getenv("LINKEDIN_EMAIL");
            }
            if (resolvedPassword == null || resolvedPassword.isBlank()) {
                resolvedPassword = System.getenv("LINKEDIN_PASSWORD");
            }

            if (resolvedEmail == null || resolvedEmail.isBlank()) {
                logger.error("LinkedIn email not provided for auto-apply");
                throw new InvalidCredentialsException("LinkedIn email is required when autoApply is true");
            }
            if (resolvedPassword == null || resolvedPassword.isBlank()) {
                logger.error("LinkedIn password not provided for auto-apply");
                throw new InvalidCredentialsException("LinkedIn password is required when autoApply is true");
            }
            
            logger.info("Initiating auto-apply for job {} with resume {}", jobId, resumeId);
            automationService.apply(job, resume, resolvedEmail, resolvedPassword, candidateContext.headless());
            status = STATUS_APPLIED;
            logger.info("Auto-apply completed successfully for job {} with resume {}", jobId, resumeId);
        }

        ApplicationRecord record = new ApplicationRecord();
        record.setResume(resume);
        record.setJob(job);
        record.setMatchScore(matchScore);
        record.setMatchSummary(matchSummary);
        record.setStatus(status);
        ApplicationRecord saved = applicationRepository.save(record);
        candidateProfileService.writeApplicationConfirmation(resume, job, saved);

        logger.info("Application record created: id={}, status={}, matchScore={}", saved.getId(), status, matchScore);
        return new ApplyResponse(saved.getId(), matchScore, matchSummary, status);
    }
}
