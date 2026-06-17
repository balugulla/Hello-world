package org.gulla.service.gulla.controller;

import jakarta.validation.Valid;
import org.gulla.service.gulla.dto.ApplyRequest;
import org.gulla.service.gulla.dto.ApplyResponse;
import org.gulla.service.gulla.dto.JobPostingRequest;
import org.gulla.service.gulla.dto.ResumeRequest;
import org.gulla.service.gulla.model.JobPosting;
import org.gulla.service.gulla.model.ResumeProfile;
import org.gulla.service.gulla.repository.JobPostingRepository;
import org.gulla.service.gulla.repository.ResumeProfileRepository;
import org.gulla.service.gulla.service.ApplicationOrchestrationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EasyApplyController {

    private final ResumeProfileRepository resumeRepository;
    private final JobPostingRepository jobRepository;
    private final ApplicationOrchestrationService orchestrationService;

    public EasyApplyController(
            ResumeProfileRepository resumeRepository,
            JobPostingRepository jobRepository,
            ApplicationOrchestrationService orchestrationService) {
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/resumes")
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeProfile createResume(@Valid @RequestBody ResumeRequest request) {
        ResumeProfile resume = new ResumeProfile();
        resume.setCandidateName(request.candidateName());
        resume.setSummary(request.summary());
        resume.setSkills(request.skills());
        resume.setYearsExperience(request.yearsExperience());
        return resumeRepository.save(resume);
    }

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public JobPosting createJob(@Valid @RequestBody JobPostingRequest request) {
        JobPosting job = new JobPosting();
        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setRequiredSkills(request.requiredSkills());
        job.setMinExperience(request.minExperience());
        job.setLinkedinJobUrl(request.linkedinJobUrl());
        job.setActive(request.active());
        return jobRepository.save(job);
    }

    @PostMapping("/applications/evaluate")
    public ApplyResponse evaluateAndApply(@Valid @RequestBody ApplyRequest request) {
        return orchestrationService.evaluateAndApply(
                request.resumeId(),
                request.jobId(),
                request.autoApply(),
                request.linkedinEmail(),
                request.linkedinPassword(),
                request.headless());
    }
}
