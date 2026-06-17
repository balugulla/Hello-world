package org.gulla.service.gulla.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record JobPostingRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String requiredSkills,
        @Min(0) int minExperience,
        @NotBlank String linkedinJobUrl,
        boolean active
) {
}
