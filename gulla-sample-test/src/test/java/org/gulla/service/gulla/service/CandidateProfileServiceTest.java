package org.gulla.service.gulla.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gulla.service.gulla.config.EasyApplyConfigProperties;
import org.gulla.service.gulla.exception.InvalidCandidateProfileException;
import org.gulla.service.gulla.model.ApplicationRecord;
import org.gulla.service.gulla.model.JobPosting;
import org.gulla.service.gulla.model.ResumeProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class CandidateProfileServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateCandidateWorkspaceActivateProfileAndWriteConfirmation() throws Exception {
        EasyApplyConfigProperties properties = new EasyApplyConfigProperties();
        properties.setCandidateDataRoot(tempDir.toString());
        CandidateProfileService service = new CandidateProfileService(properties, new ObjectMapper());

        ResumeProfile resume = new ResumeProfile();
        ReflectionTestUtils.setField(resume, "id", 7L);
        resume.setCandidateName("Jane Doe");
        resume.setSummary("Java developer");
        resume.setSkills("Java, Spring");
        resume.setYearsExperience(6);

        service.registerCandidateProfile(resume);
        Path candidateDir = tempDir.resolve("7-jane-doe");
        assertTrue(Files.exists(candidateDir.resolve("candidate-profile.json")));
        assertTrue(Files.exists(candidateDir.resolve("candidate-config.properties")));

        Properties candidateProperties = new Properties();
        candidateProperties.setProperty("candidate.resume-id", "7");
        candidateProperties.setProperty("candidate.name", "Jane Doe");
        candidateProperties.setProperty("linkedin.email", "jane@example.com");
        candidateProperties.setProperty("linkedin.password", "test-password");
        candidateProperties.setProperty("automation.headless", "false");
        candidateProperties.setProperty("automation.ai-analysis", "true");
        try (OutputStream outputStream = Files.newOutputStream(candidateDir.resolve("candidate-config.properties"))) {
            candidateProperties.store(outputStream, "test");
        }

        service.activateProfile(resume);

        CandidateProfileService.CandidateExecutionContext context =
                service.resolveExecutionContext(resume, null, null, null, null);

        assertEquals("jane@example.com", context.linkedinEmail());
        assertEquals("test-password", context.linkedinPassword());
        assertFalse(context.headless());
        assertTrue(context.aiAnalysis());

        JobPosting job = new JobPosting();
        ReflectionTestUtils.setField(job, "id", 11L);
        job.setTitle("Backend Engineer");
        job.setLinkedinJobUrl("https://linkedin.com/jobs/view/11");

        ApplicationRecord record = new ApplicationRecord();
        ReflectionTestUtils.setField(record, "id", 22L);
        record.setMatchScore(0.91);
        record.setMatchSummary("Strong match");
        record.setStatus("APPLIED");

        service.writeApplicationConfirmation(resume, job, record);

        assertTrue(Files.exists(candidateDir.resolve("confirmations/application-22.json")));
    }

    @Test
    void shouldRejectResumeWhenAnotherCandidateIsActive() {
        EasyApplyConfigProperties properties = new EasyApplyConfigProperties();
        properties.setCandidateDataRoot(tempDir.toString());
        CandidateProfileService service = new CandidateProfileService(properties, new ObjectMapper());

        ResumeProfile firstResume = new ResumeProfile();
        ReflectionTestUtils.setField(firstResume, "id", 1L);
        firstResume.setCandidateName("First Candidate");
        firstResume.setSummary("Summary");
        firstResume.setSkills("Java");
        firstResume.setYearsExperience(5);
        service.activateProfile(firstResume);

        ResumeProfile secondResume = new ResumeProfile();
        ReflectionTestUtils.setField(secondResume, "id", 2L);
        secondResume.setCandidateName("Second Candidate");
        secondResume.setSummary("Summary");
        secondResume.setSkills("Java");
        secondResume.setYearsExperience(5);

        InvalidCandidateProfileException exception = assertThrows(
                InvalidCandidateProfileException.class,
                () -> service.resolveExecutionContext(secondResume, null, null, null, null));

        assertTrue(exception.getMessage().contains("does not match"));
    }
}
