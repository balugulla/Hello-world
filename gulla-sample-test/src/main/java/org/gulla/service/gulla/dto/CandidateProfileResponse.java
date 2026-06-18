package org.gulla.service.gulla.dto;

public record CandidateProfileResponse(
        Long resumeId,
        String candidateName,
        String folderPath,
        String configFilePath,
        String confirmationsPath,
        boolean active
) {
}
