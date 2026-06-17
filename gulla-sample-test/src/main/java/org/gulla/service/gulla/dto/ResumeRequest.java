package org.gulla.service.gulla.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ResumeRequest(
        @NotBlank String candidateName,
        @NotBlank String summary,
        @NotBlank String skills,
        @Min(0) int yearsExperience
) {
}
