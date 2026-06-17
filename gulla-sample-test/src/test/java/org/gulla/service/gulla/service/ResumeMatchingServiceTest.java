package org.gulla.service.gulla.service;

import org.gulla.service.gulla.model.JobPosting;
import org.gulla.service.gulla.model.ResumeProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeMatchingServiceTest {

    private final ResumeMatchingService service = new ResumeMatchingService();

    @Test
    void shouldProduceHighScoreWhenSkillsAndExperienceMatch() {
        ResumeProfile resume = new ResumeProfile();
        resume.setCandidateName("Alex Dev");
        resume.setSummary("Experienced Java and Spring engineer building cloud APIs");
        resume.setSkills("Java, Spring Boot, SQL, Playwright");
        resume.setYearsExperience(6);

        JobPosting job = new JobPosting();
        job.setTitle("Backend Engineer");
        job.setDescription("Need Java Spring Boot API developer with SQL experience");
        job.setRequiredSkills("Java, Spring Boot, SQL");
        job.setMinExperience(4);

        ResumeMatchResult result = service.score(resume, job);

        assertTrue(result.score() >= 0.9);
        assertTrue(result.summary().contains("Matched 3/3 required skills"));
    }

    @Test
    void shouldPenalizeLowSkillOverlap() {
        ResumeProfile resume = new ResumeProfile();
        resume.setCandidateName("Taylor Ops");
        resume.setSummary("Automation and scripting");
        resume.setSkills("Bash, Terraform");
        resume.setYearsExperience(2);

        JobPosting job = new JobPosting();
        job.setTitle("Data Engineer");
        job.setDescription("Python and SQL role");
        job.setRequiredSkills("Python, SQL, Spark");
        job.setMinExperience(3);

        ResumeMatchResult result = service.score(resume, job);

        assertTrue(result.score() < 0.4);
        assertEquals("Matched 0/3 required skills (none); candidate experience: 2 years; minimum needed: 3 years.", result.summary());
    }
}
