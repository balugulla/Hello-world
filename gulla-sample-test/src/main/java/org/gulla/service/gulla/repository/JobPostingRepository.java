package org.gulla.service.gulla.repository;

import org.gulla.service.gulla.model.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
}
