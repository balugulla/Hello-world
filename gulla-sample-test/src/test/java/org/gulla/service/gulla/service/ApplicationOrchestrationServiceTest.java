package org.gulla.service.gulla.service;

import org.gulla.service.gulla.config.EasyApplyConfigProperties;
import org.gulla.service.gulla.dto.ApplyResponse;
import org.gulla.service.gulla.model.JobPosting;
import org.gulla.service.gulla.model.ResumeProfile;
import org.gulla.service.gulla.repository.ApplicationRecordRepository;
import org.gulla.service.gulla.repository.JobPostingRepository;
import org.gulla.service.gulla.repository.ResumeProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationOrchestrationServiceTest {

    @Mock
    private ResumeProfileRepository resumeRepository;
    @Mock
    private JobPostingRepository jobRepository;
    @Mock
    private ApplicationRecordRepository applicationRepository;
    @Mock
    private ResumeMatchingService resumeMatchingService;
    @Mock
    private LinkedInEasyApplyAutomationService automationService;
    @Mock
    private CandidateProfileService candidateProfileService;

    private EasyApplyConfigProperties config;
    private ApplicationOrchestrationService service;

    @BeforeEach
    void setUp() {
        config = new EasyApplyConfigProperties();
        config.setMatchThreshold(0.70);
        service = new ApplicationOrchestrationService(
                resumeRepository, jobRepository, applicationRepository,
                resumeMatchingService, automationService, candidateProfileService, config);

        ResumeProfile resume = new ResumeProfile();
        resume.setCandidateName("Test Candidate");
        resume.setSummary("Java developer");
        resume.setSkills("Java, Spring Boot");
        resume.setYearsExperience(5);

        JobPosting job = new JobPosting();
        job.setTitle("Backend Engineer");
        job.setDescription("Java Spring Boot role");
        job.setRequiredSkills("Java, Spring Boot");
        job.setMinExperience(3);
        job.setLinkedinJobUrl("https://linkedin.com/jobs/123");

        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(applicationRepository.save(any())).thenAnswer(inv -> {
            var rec = inv.getArgument(0, org.gulla.service.gulla.model.ApplicationRecord.class);
            return rec;
        });
        when(candidateProfileService.resolveExecutionContext(any(), any(), any(), any(), any()))
                .thenReturn(new CandidateProfileService.CandidateExecutionContext(null, null, true, true));
    }

    @Test
    void whenAiAnalysisIsFalse_shouldSkipMatchingAndReturnAiAnalysisSkippedStatus() {
        when(candidateProfileService.resolveExecutionContext(any(), any(), any(), any(), any()))
                .thenReturn(new CandidateProfileService.CandidateExecutionContext(null, null, true, false));

        ApplyResponse response = service.evaluateAndApply(1L, 1L, false, null, null, null, null);

        assertEquals("AI_ANALYSIS_SKIPPED", response.status());
        assertEquals(0.0, response.matchScore());
        assertEquals("AI analysis skipped", response.matchSummary());
        verifyNoInteractions(resumeMatchingService);
        verify(candidateProfileService).writeApplicationConfirmation(any(), any(), any());
    }

    @Test
    void whenAiAnalysisIsTrue_shouldRunMatchingAndReturnMatchedStatus() {
        when(resumeMatchingService.score(any(), any()))
                .thenReturn(new ResumeMatchResult(0.95, "Matched 2/2 required skills"));

        ApplyResponse response = service.evaluateAndApply(1L, 1L, false, null, null, null, true);

        assertEquals("MATCHED", response.status());
        assertEquals(0.95, response.matchScore());
        verify(resumeMatchingService, times(1)).score(any(), any());
        verify(candidateProfileService).writeApplicationConfirmation(any(), any(), any());
    }

    @Test
    void whenAiAnalysisIsTrue_andScoreBelowThreshold_shouldReturnSkippedLowMatchStatus() {
        when(resumeMatchingService.score(any(), any()))
                .thenReturn(new ResumeMatchResult(0.50, "Low match"));

        ApplyResponse response = service.evaluateAndApply(1L, 1L, false, null, null, null, true);

        assertEquals("SKIPPED_LOW_MATCH", response.status());
        verify(resumeMatchingService, times(1)).score(any(), any());
    }
}
