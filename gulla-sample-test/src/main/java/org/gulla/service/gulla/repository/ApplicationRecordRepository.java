package org.gulla.service.gulla.repository;

import org.gulla.service.gulla.model.ApplicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRecordRepository extends JpaRepository<ApplicationRecord, Long> {
}
