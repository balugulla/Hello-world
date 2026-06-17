CREATE TABLE IF NOT EXISTS resume_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    candidate_name VARCHAR(255) NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    skills VARCHAR(2000) NOT NULL,
    years_experience INT NOT NULL
);

CREATE TABLE IF NOT EXISTS job_postings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    required_skills VARCHAR(2000) NOT NULL,
    min_experience INT NOT NULL,
    linkedin_job_url VARCHAR(1000) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS application_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    match_score DOUBLE NOT NULL,
    match_summary VARCHAR(2000) NOT NULL,
    status VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_application_resume FOREIGN KEY (resume_id) REFERENCES resume_profiles(id),
    CONSTRAINT fk_application_job FOREIGN KEY (job_id) REFERENCES job_postings(id)
);
