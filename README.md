# LinkedIn Easy Apply Automation (Spring Boot + Playwright)

This repository now contains a complete Java Spring Boot project (`gulla-sample-test`) that provides:

- Resume and job ingestion APIs
- Database schema for resumes, job postings, and application records
- AI-style resume matching logic with weighted scoring
- Playwright-based LinkedIn Easy Apply browser automation workflow

## Tech stack

- Java 17
- Spring Boot 3.3.1
- Spring Web + Spring Data JPA + Validation
- H2 in-memory database with explicit SQL schema
- Playwright Java 1.46.0

## Run

```bash
cd gulla-sample-test
mvn spring-boot:run
```

## API endpoints

- `POST /api/resumes` – create a resume profile
- `POST /api/jobs` – create a LinkedIn job posting record
- `POST /api/applications/evaluate` – calculate match score and optionally auto-apply

### Example `POST /api/applications/evaluate`

```json
{
  "resumeId": 1,
  "jobId": 1,
  "autoApply": false,
  "linkedinEmail": "",
  "linkedinPassword": "",
  "headless": true
}
```

If `autoApply=true` and match score meets threshold (`easyapply.match-threshold`, default `0.70`), the app runs LinkedIn login and Easy Apply actions through Playwright.

## Database schema

Defined at:

- `gulla-sample-test/src/main/resources/schema.sql`

Tables:

- `resume_profiles`
- `job_postings`
- `application_records`

## Tests

```bash
cd gulla-sample-test
mvn test
```


Security note: for production use, prefer secret managers or injected runtime secrets for LinkedIn credentials; do not log credentials or commit them to source control.
