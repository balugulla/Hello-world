package org.gulla.service.gulla.dto;

public record ApplyResponse(
        Long applicationId,
        double matchScore,
        String matchSummary,
        String status
) {
}
