package org.gulla.service.gulla.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.gulla.service.gulla.dto.ApplyRequest;
import org.gulla.service.gulla.dto.ApplyResponse;
import org.gulla.service.gulla.dto.CandidateProfileResponse;
import org.gulla.service.gulla.dto.CandidateProfileSelectionRequest;
import org.gulla.service.gulla.dto.JobPostingRequest;
import org.gulla.service.gulla.dto.ResumeRequest;
import org.gulla.service.gulla.model.JobPosting;
import org.gulla.service.gulla.model.ResumeProfile;
import org.gulla.service.gulla.repository.JobPostingRepository;
import org.gulla.service.gulla.repository.ResumeProfileRepository;
import org.gulla.service.gulla.service.ApplicationOrchestrationService;
import org.gulla.service.gulla.service.CandidateProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "LinkedIn Easy Apply", description = "APIs for managing resumes, jobs, and automated applications")
public class EasyApplyController {

    private static final Logger logger = LoggerFactory.getLogger(EasyApplyController.class);

    private final ResumeProfileRepository resumeRepository;
    private final JobPostingRepository jobRepository;
    private final ApplicationOrchestrationService orchestrationService;
    private final CandidateProfileService candidateProfileService;

    public EasyApplyController(
            ResumeProfileRepository resumeRepository,
            JobPostingRepository jobRepository,
            ApplicationOrchestrationService orchestrationService,
            CandidateProfileService candidateProfileService) {
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.orchestrationService = orchestrationService;
        this.candidateProfileService = candidateProfileService;
    }

    @Operation(summary = "Create a new resume profile", description = "Store a candidate's resume information for future job applications")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Resume created successfully",
                    content = @Content(schema = @Schema(implementation = ResumeProfile.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/resumes")
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeProfile createResume(@Valid @RequestBody ResumeRequest request) {
        logger.info("Creating resume for candidate: {}", request.candidateName());
        ResumeProfile resume = new ResumeProfile();
        resume.setCandidateName(request.candidateName());
        resume.setSummary(request.summary());
        resume.setSkills(request.skills());
        resume.setYearsExperience(request.yearsExperience());
        ResumeProfile saved = resumeRepository.save(resume);
        candidateProfileService.registerCandidateProfile(saved);
        logger.info("Resume created with id: {}", saved.getId());
        return saved;
    }

    @Operation(summary = "List candidate profiles", description = "Show candidate-specific profile folders and whether each profile is active")
    @GetMapping("/candidate-profiles")
    public List<CandidateProfileResponse> listCandidateProfiles() {
        return candidateProfileService.listProfiles(resumeRepository.findAll());
    }

    @Operation(summary = "Select active candidate profile", description = "Activates a candidate profile so only that candidate's configuration is used for applications")
    @PostMapping("/candidate-profiles/select")
    public CandidateProfileResponse selectCandidateProfile(@Valid @RequestBody CandidateProfileSelectionRequest request) {
        ResumeProfile resume = resumeRepository.findById(request.resumeId())
                .orElseThrow(() -> new org.gulla.service.gulla.exception.ResourceNotFoundException("Resume", request.resumeId()));
        return candidateProfileService.activateProfile(resume);
    }

    @Operation(summary = "Create a new job posting", description = "Store a LinkedIn job posting for evaluation and application")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Job posting created successfully",
                    content = @Content(schema = @Schema(implementation = JobPosting.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public JobPosting createJob(@Valid @RequestBody JobPostingRequest request) {
        logger.info("Creating job posting: {}", request.title());
        JobPosting job = new JobPosting();
        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setRequiredSkills(request.requiredSkills());
        job.setMinExperience(request.minExperience());
        job.setLinkedinJobUrl(request.linkedinJobUrl());
        job.setActive(request.active());
        JobPosting saved = jobRepository.save(job);
        logger.info("Job posting created with id: {}", saved.getId());
        return saved;
    }

    @Operation(summary = "Evaluate and optionally apply to a job", 
            description = "Calculate match score between a resume and job, and optionally auto-apply via LinkedIn")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evaluation completed successfully",
                    content = @Content(schema = @Schema(implementation = ApplyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or missing credentials"),
            @ApiResponse(responseCode = "404", description = "Resume or job not found"),
            @ApiResponse(responseCode = "500", description = "Automation failed")
    })
    @PostMapping("/applications/evaluate")
    public ApplyResponse evaluateAndApply(@Valid @RequestBody ApplyRequest request) {
        logger.info("Evaluating application for resume {} and job {}", request.resumeId(), request.jobId());
        return orchestrationService.evaluateAndApply(
                request.resumeId(),
                request.jobId(),
                request.autoApply(),
                request.linkedinEmail(),
                request.linkedinPassword(),
                request.headless(),
                request.aiAnalysis());
    }
}
