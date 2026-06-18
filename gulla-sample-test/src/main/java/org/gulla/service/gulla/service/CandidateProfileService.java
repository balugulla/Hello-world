package org.gulla.service.gulla.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gulla.service.gulla.config.EasyApplyConfigProperties;
import org.gulla.service.gulla.dto.CandidateProfileResponse;
import org.gulla.service.gulla.exception.InvalidCandidateProfileException;
import org.gulla.service.gulla.model.ApplicationRecord;
import org.gulla.service.gulla.model.JobPosting;
import org.gulla.service.gulla.model.ResumeProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

@Service
public class CandidateProfileService {

    private static final Logger logger = LoggerFactory.getLogger(CandidateProfileService.class);
    private static final String PROFILE_DETAILS_FILE = "candidate-profile.json";
    private static final String PROFILE_CONFIG_FILE = "candidate-config.properties";
    private static final String CONFIRMATIONS_DIRECTORY = "confirmations";
    private static final String ACTIVE_PROFILE_FILE = "active-profile.json";

    private final EasyApplyConfigProperties config;
    private final ObjectMapper objectMapper;

    public CandidateProfileService(EasyApplyConfigProperties config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public CandidateProfileResponse registerCandidateProfile(ResumeProfile resume) {
        try {
            Path candidateDirectory = getCandidateDirectory(resume);
            ensureCandidateProfileStructure(candidateDirectory, resume);
            writeJson(candidateDirectory.resolve(PROFILE_DETAILS_FILE), Map.of(
                    "resumeId", resume.getId(),
                    "candidateName", resume.getCandidateName(),
                    "summary", resume.getSummary(),
                    "skills", resume.getSkills(),
                    "yearsExperience", resume.getYearsExperience(),
                    "updatedAt", Instant.now().toString()
            ));
            createConfigTemplateIfMissing(candidateDirectory.resolve(PROFILE_CONFIG_FILE), resume);
            return toResponse(resume, false);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to initialize candidate profile files", ex);
        }
    }

    public List<CandidateProfileResponse> listProfiles(List<ResumeProfile> resumes) {
        Long activeResumeId = readActiveProfile().map(ActiveProfile::resumeId).orElse(null);
        return resumes.stream()
                .map(resume -> {
                    ensureCandidateProfileStructure(getCandidateDirectory(resume), resume);
                    return toResponse(resume, resume.getId().equals(activeResumeId));
                })
                .toList();
    }

    public CandidateProfileResponse activateProfile(ResumeProfile resume) {
        registerCandidateProfile(resume);
        Instant selectedAt = Instant.now();
        try {
            writeJson(getRootDirectory().resolve(ACTIVE_PROFILE_FILE), Map.of(
                    "resumeId", resume.getId(),
                    "candidateName", resume.getCandidateName(),
                    "selectedAt", selectedAt.toString()
            ));
            writeJson(getCandidateDirectory(resume)
                    .resolve(CONFIRMATIONS_DIRECTORY)
                    .resolve("profile-selected-" + timestampSuffix(selectedAt) + ".json"), Map.of(
                    "event", "PROFILE_SELECTED",
                    "resumeId", resume.getId(),
                    "candidateName", resume.getCandidateName(),
                    "selectedAt", selectedAt.toString()
            ));
            logger.info("Activated candidate profile {}", resume.getId());
            return toResponse(resume, true);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to activate candidate profile", ex);
        }
    }

    public CandidateExecutionContext resolveExecutionContext(
            ResumeProfile resume,
            String email,
            String password,
            Boolean headless,
            Boolean aiAnalysis) {
        ActiveProfile activeProfile = readActiveProfile()
                .orElseThrow(() -> new InvalidCandidateProfileException(
                        "No candidate profile selected. Select a candidate profile before starting applications."));
        if (!resume.getId().equals(activeProfile.resumeId())) {
            throw new InvalidCandidateProfileException(
                    "Selected candidate profile does not match resumeId " + resume.getId() + ".");
        }

        CandidateConfig candidateConfig = readCandidateConfig(resume);
        return new CandidateExecutionContext(
                firstNonBlank(email, candidateConfig.linkedinEmail(), System.getenv("LINKEDIN_EMAIL")),
                firstNonBlank(password, candidateConfig.linkedinPassword(), System.getenv("LINKEDIN_PASSWORD")),
                headless != null ? headless : candidateConfig.headless(),
                aiAnalysis != null ? aiAnalysis : candidateConfig.aiAnalysis()
        );
    }

    public void writeApplicationConfirmation(ResumeProfile resume, JobPosting job, ApplicationRecord record) {
        registerCandidateProfile(resume);
        Instant createdAt = record.getCreatedAt() == null ? Instant.now() : record.getCreatedAt();
        String identifier = record.getId() == null
                ? "job-" + job.getId() + "-" + timestampSuffix(createdAt)
                : "application-" + record.getId();
        try {
            writeJson(getCandidateDirectory(resume)
                    .resolve(CONFIRMATIONS_DIRECTORY)
                    .resolve(identifier + ".json"), Map.of(
                    "applicationId", record.getId(),
                    "resumeId", resume.getId(),
                    "candidateName", resume.getCandidateName(),
                    "jobId", job.getId(),
                    "jobTitle", job.getTitle(),
                    "jobUrl", job.getLinkedinJobUrl(),
                    "status", record.getStatus(),
                    "matchScore", record.getMatchScore(),
                    "matchSummary", record.getMatchSummary(),
                    "createdAt", createdAt.toString()
            ));
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to write candidate confirmation file", ex);
        }
    }

    private CandidateConfig readCandidateConfig(ResumeProfile resume) {
        Path configFile = getCandidateDirectory(resume).resolve(PROFILE_CONFIG_FILE);
        ensureCandidateProfileStructure(getCandidateDirectory(resume), resume);
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configFile)) {
            properties.load(inputStream);
            return new CandidateConfig(
                    clean(properties.getProperty("linkedin.email")),
                    clean(properties.getProperty("linkedin.password")),
                    Boolean.parseBoolean(properties.getProperty("automation.headless", "true")),
                    Boolean.parseBoolean(properties.getProperty("automation.ai-analysis", "true"))
            );
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read candidate configuration", ex);
        }
    }

    private Optional<ActiveProfile> readActiveProfile() {
        Path activeProfileFile = getRootDirectory().resolve(ACTIVE_PROFILE_FILE);
        if (!Files.exists(activeProfileFile)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(activeProfileFile.toFile(), ActiveProfile.class));
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read active candidate profile", ex);
        }
    }

    private CandidateProfileResponse toResponse(ResumeProfile resume, boolean active) {
        Path candidateDirectory = getCandidateDirectory(resume);
        return new CandidateProfileResponse(
                resume.getId(),
                resume.getCandidateName(),
                candidateDirectory.toString(),
                candidateDirectory.resolve(PROFILE_CONFIG_FILE).toString(),
                candidateDirectory.resolve(CONFIRMATIONS_DIRECTORY).toString(),
                active
        );
    }

    private void createConfigTemplateIfMissing(Path configFile, ResumeProfile resume) throws IOException {
        if (Files.exists(configFile)) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("candidate.resume-id", String.valueOf(resume.getId()));
        properties.setProperty("candidate.name", resume.getCandidateName());
        properties.setProperty("linkedin.email", "");
        properties.setProperty("linkedin.password", "");
        properties.setProperty("automation.headless", "true");
        properties.setProperty("automation.ai-analysis", "true");
        try (OutputStream outputStream = Files.newOutputStream(configFile)) {
            properties.store(outputStream, "Candidate-specific automation settings");
        }
    }

    private void ensureCandidateProfileStructure(Path candidateDirectory, ResumeProfile resume) {
        try {
            Files.createDirectories(candidateDirectory.resolve(CONFIRMATIONS_DIRECTORY));
            createConfigTemplateIfMissing(candidateDirectory.resolve(PROFILE_CONFIG_FILE), resume);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to initialize candidate profile files", ex);
        }
    }

    private void writeJson(Path path, Object payload) throws IOException {
        Files.createDirectories(path.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), payload);
    }

    private Path getRootDirectory() {
        return Paths.get(config.getCandidateDataRoot()).toAbsolutePath().normalize();
    }

    private Path getCandidateDirectory(ResumeProfile resume) {
        if (resume.getId() == null) {
            throw new InvalidCandidateProfileException("Resume must be saved before candidate files can be created.");
        }
        return getRootDirectory().resolve(resume.getId() + "-" + slugify(resume.getCandidateName()));
    }

    private String slugify(String candidateName) {
        String normalized = candidateName == null ? "candidate" : candidateName.trim().toLowerCase();
        String slug = normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "candidate" : slug;
    }

    private String timestampSuffix(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant).replace(":", "-");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record CandidateExecutionContext(
            String linkedinEmail,
            String linkedinPassword,
            boolean headless,
            boolean aiAnalysis
    ) {
    }

    private record CandidateConfig(
            String linkedinEmail,
            String linkedinPassword,
            boolean headless,
            boolean aiAnalysis
    ) {
    }

    private record ActiveProfile(
            Long resumeId,
            String candidateName,
            String selectedAt
    ) {
    }
}
