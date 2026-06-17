package org.gulla.service.gulla.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.gulla.service.gulla.model.JobPosting;
import org.gulla.service.gulla.model.ResumeProfile;
import org.springframework.stereotype.Service;

@Service
public class ResumeMatchingService {

    private static final double SKILL_WEIGHT = 0.6;
    private static final double EXPERIENCE_WEIGHT = 0.25;
    private static final double CONTEXT_WEIGHT = 0.15;

    public ResumeMatchResult score(ResumeProfile resume, JobPosting job) {
        Set<String> resumeSkills = normalize(resume.getSkills());
        Set<String> requiredSkills = normalize(job.getRequiredSkills());

        long overlapCount = requiredSkills.stream().filter(resumeSkills::contains).count();
        double skillScore = requiredSkills.isEmpty() ? 1.0 : (double) overlapCount / requiredSkills.size();

        double experienceScore = resume.getYearsExperience() >= job.getMinExperience()
                ? 1.0
                : Math.max(0.0, (double) resume.getYearsExperience() / Math.max(1, job.getMinExperience()));

        String combinedText = (resume.getSummary() + " " + job.getDescription()).toLowerCase(Locale.ROOT);
        long matchedTerms = requiredSkills.stream().filter(combinedText::contains).count();
        double contextScore = requiredSkills.isEmpty() ? 1.0 : (double) matchedTerms / requiredSkills.size();

        double finalScore = (skillScore * SKILL_WEIGHT) + (experienceScore * EXPERIENCE_WEIGHT) + (contextScore * CONTEXT_WEIGHT);
        double normalizedScore = Math.round(finalScore * 100.0) / 100.0;

        String matchedSkills = requiredSkills.stream()
                .filter(resumeSkills::contains)
                .collect(Collectors.joining(", "));

        String summary = String.format(
                "Matched %d/%d required skills (%s); candidate experience: %d years; minimum needed: %d years.",
                overlapCount,
                requiredSkills.size(),
                matchedSkills.isBlank() ? "none" : matchedSkills,
                resume.getYearsExperience(),
                job.getMinExperience());

        return new ResumeMatchResult(normalizedScore, summary);
    }

    private Set<String> normalize(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(skill -> skill.toLowerCase(Locale.ROOT).trim())
                .filter(skill -> !skill.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
