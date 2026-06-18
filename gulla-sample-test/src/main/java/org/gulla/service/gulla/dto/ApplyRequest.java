package org.gulla.service.gulla.dto;

import jakarta.validation.constraints.NotNull;

public record ApplyRequest(
        @NotNull Long resumeId,
        @NotNull Long jobId,
        boolean autoApply,
        String linkedinEmail,
        String linkedinPassword,
        Boolean headless,
        Boolean aiAnalysis
) {
}
