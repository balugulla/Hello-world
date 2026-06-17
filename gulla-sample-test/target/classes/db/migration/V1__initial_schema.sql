-- Resume Profiles Table
CREATE TABLE resume_profiles (
    id BIGSERIAL PRIMARY KEY,
    candidate_name VARCHAR(255) NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    skills VARCHAR(2000) NOT NULL,
    years_experience INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Job Postings Table
CREATE TABLE job_postings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    required_skills VARCHAR(2000) NOT NULL,
    min_experience INT NOT NULL,
    linkedin_job_url VARCHAR(1000) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Application Records Table
CREATE TABLE application_records (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    match_score DOUBLE PRECISION NOT NULL,
    match_summary VARCHAR(2000) NOT NULL,
    status VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    CONSTRAINT fk_application_resume FOREIGN KEY (resume_id) REFERENCES resume_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_application_job FOREIGN KEY (job_id) REFERENCES job_postings(id) ON DELETE CASCADE,
    CONSTRAINT unique_resume_job UNIQUE (resume_id, job_id)
);

-- Indexes for Performance
CREATE INDEX idx_resume_profiles_created_at ON resume_profiles(created_at);
CREATE INDEX idx_job_postings_active ON job_postings(active);
CREATE INDEX idx_job_postings_created_at ON job_postings(created_at);
CREATE INDEX idx_application_records_resume_id ON application_records(resume_id);
CREATE INDEX idx_application_records_job_id ON application_records(job_id);
CREATE INDEX idx_application_records_status ON application_records(status);
CREATE INDEX idx_application_records_created_at ON application_records(created_at);
