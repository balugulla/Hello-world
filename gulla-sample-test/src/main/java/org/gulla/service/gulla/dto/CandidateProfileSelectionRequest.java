package org.gulla.service.gulla.dto;

import jakarta.validation.constraints.NotNull;

public record CandidateProfileSelectionRequest(@NotNull Long resumeId) {
}
