# LinkedIn Easy Apply Automation - Enterprise Edition

[![CI/CD Pipeline](https://github.com/balugulla/Hello-world/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/balugulla/Hello-world/actions/workflows/ci-cd.yml)

A production-ready Spring Boot application for automating LinkedIn Easy Apply job applications with intelligent resume matching, comprehensive security, and enterprise-grade architecture.

## 🚀 Features

- **Resume & Job Management**: REST APIs for storing and managing candidate resumes and job postings
- **Intelligent Matching**: AI-powered resume-to-job matching with configurable scoring thresholds
- **Browser Automation**: Playwright-based LinkedIn Easy Apply automation with retry logic
- **Security**: API key authentication, encrypted credentials, secure configuration management
- **Observability**: Comprehensive logging, metrics, health checks, and monitoring
- **Database**: PostgreSQL with Flyway migrations, audit trails, and optimized indexes
- **API Documentation**: Interactive OpenAPI/Swagger documentation
- **Containerization**: Docker and Docker Compose support for easy deployment
- **CI/CD**: Automated testing, security scanning, and deployment pipelines

## 📋 Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Security](#security)
- [Deployment](#deployment)
- [Monitoring](#monitoring)
- [Development](#development)
- [Testing](#testing)
- [Contributing](#contributing)

## 🏗️ Architecture

### Layered Architecture

```
┌─────────────────────────────────────────┐
│         API Layer (Controllers)         │
│  - REST endpoints                       │
│  - Request validation                   │
│  - OpenAPI documentation                │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│       Service Layer (Business Logic)    │
│  - Application orchestration            │
│  - Resume matching                      │
│  - Browser automation                   │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│     Repository Layer (Data Access)      │
│  - JPA repositories                     │
│  - Database queries                     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│          Database (PostgreSQL)          │
│  - Resume profiles                      │
│  - Job postings                         │
│  - Application records                  │
└─────────────────────────────────────────┘
```

### Key Components

- **Controllers**: Handle HTTP requests, validation, and response formatting
- **Services**: Implement business logic, orchestration, and automation
- **Repositories**: Manage data persistence and queries
- **Models**: JPA entities with audit trails
- **DTOs**: Data transfer objects for API contracts
- **Configuration**: Profile-based settings (dev, test, prod)
- **Security**: API key authentication and authorization
- **Monitoring**: Actuator endpoints for health and metrics

## 🛠️ Tech Stack

### Core Framework
- **Java 17** - Modern Java LTS version
- **Spring Boot 3.3.1** - Application framework
- **Spring Web** - RESTful web services
- **Spring Data JPA** - Data persistence
- **Spring Security** - Authentication and authorization
- **Spring Boot Actuator** - Production monitoring

### Database
- **PostgreSQL** - Production database
- **H2** - In-memory database for development/testing
- **Flyway** - Database migration management

### Automation & Testing
- **Playwright Java 1.46.0** - Browser automation
- **JUnit 5** - Testing framework
- **Maven** - Build and dependency management

### Documentation & Monitoring
- **SpringDoc OpenAPI 2.2.0** - API documentation
- **SLF4J + Logback** - Logging framework
- **Micrometer** - Metrics collection

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration
- **GitHub Actions** - CI/CD pipeline

## 📦 Prerequisites

- **Java 17** or higher
- **Maven 3.9+**
- **PostgreSQL 15** (for production)
- **Docker & Docker Compose** (optional, for containerized deployment)

## 🚀 Getting Started

### Local Development

1. **Clone the repository**
```bash
git clone https://github.com/balugulla/Hello-world.git
cd Hello-world
```

2. **Run with H2 (in-memory database)**
```bash
cd gulla-sample-test
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

3. **Access the application**
- Application: http://localhost:8080
- API Documentation: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console
- Health Check: http://localhost:8080/actuator/health

### Docker Deployment

1. **Using Docker Compose** (recommended)
```bash
# Set environment variables
export API_KEY=your-secure-api-key
export DB_PASSWORD=your-db-password

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

2. **Build and run Docker image manually**
```bash
docker build -t easyapply-app .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host:5432/easyapply \
  -e API_KEY=your-api-key \
  easyapply-app
```

## ⚙️ Configuration

### Environment Profiles

The application supports three profiles:

- **dev** - Development (H2 database, verbose logging, no security)
- **test** - Testing (H2 database, minimal logging)
- **prod** - Production (PostgreSQL, security enabled, optimized settings)

Set profile via:
```bash
export SPRING_PROFILES_ACTIVE=prod
# OR
java -jar app.jar --spring.profiles.active=prod
```

### Configuration Properties

| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `easyapply.match-threshold` | Minimum score for auto-apply | 0.70 | No |
| `easyapply.automation-timeout-ms` | Playwright timeout | 15000 | No |
| `easyapply.max-retry-attempts` | Retry count for failures | 3 | No |
| `easyapply.api.key` | API key for authentication | - | Yes (prod) |
| `DATABASE_URL` | PostgreSQL connection URL | - | Yes (prod) |
| `DATABASE_USERNAME` | Database username | - | Yes (prod) |
| `DATABASE_PASSWORD` | Database password | - | Yes (prod) |
| `LINKEDIN_EMAIL` | LinkedIn account email | - | Optional |
| `LINKEDIN_PASSWORD` | LinkedIn account password | - | Optional |

### Database Configuration

**Development/Testing** (H2):
```properties
spring.datasource.url=jdbc:h2:mem:easyapplydb
```

**Production** (PostgreSQL):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/easyapply
spring.datasource.username=easyapply_user
spring.datasource.******
spring.flyway.enabled=true
```

## 📡 API Endpoints

### Base URL
```
Development: http://localhost:8080
Production:  https://api.yourdomain.com
```

### Authentication (Production Only)
All requests require `X-API-Key` header:
```bash
curl -H "X-API-Key: your-api-key" http://localhost:8080/api/v1/resumes
```

### Endpoints

#### Create Resume
```http
POST /api/v1/resumes
Content-Type: application/json

{
  "candidateName": "John Doe",
  "summary": "Experienced software engineer...",
  "skills": "Java, Spring Boot, PostgreSQL",
  "yearsExperience": 5
}
```

#### Create Job Posting
```http
POST /api/v1/jobs
Content-Type: application/json

{
  "title": "Senior Java Developer",
  "description": "We are looking for...",
  "requiredSkills": "Java, Spring, Microservices",
  "minExperience": 3,
  "linkedinJobUrl": "https://www.linkedin.com/jobs/view/...",
  "active": true
}
```

#### Evaluate and Apply
```http
POST /api/v1/applications/evaluate
Content-Type: application/json

{
  "resumeId": 1,
  "jobId": 1,
  "autoApply": false,
  "linkedinEmail": "",
  "linkedinPassword": "",
  "headless": true
}
```

**Response:**
```json
{
  "applicationId": 1,
  "matchScore": 0.85,
  "matchSummary": "Strong match: 5 years experience, Java and Spring skills align well...",
  "status": "MATCHED"
}
```

### Status Values
- `MATCHED` - Score meets threshold
- `SKIPPED_LOW_MATCH` - Score below threshold
- `APPLIED` - Successfully submitted via automation

## 🔒 Security

### Production Security Features

1. **API Key Authentication**
   - All endpoints protected (except health/docs)
   - Configure via `easyapply.api.key` property
   - Passed in `X-API-Key` header

2. **Credentials Management**
   - Never store LinkedIn credentials in code
   - Use environment variables or secret managers
   - Passwords never logged

3. **Database Security**
   - Prepared statements prevent SQL injection
   - Connection pooling with secure credentials
   - Audit trails on all entities

4. **Input Validation**
   - All inputs validated with Bean Validation
   - Comprehensive error messages without exposing internals

### Security Best Practices

```bash
# Use strong API keys
export API_KEY=$(openssl rand -base64 32)

# Store credentials securely
# Option 1: Environment variables
export LINKEDIN_EMAIL=user@example.com
export LINKEDIN_PASSWORD=secure_password

# Option 2: Secret manager (AWS, Vault, etc.)
# Configure in application-prod.properties
```

## 🐳 Deployment

### Docker Compose (Recommended)

Includes PostgreSQL, application, and optional Prometheus/Grafana:

```bash
# Start with monitoring
docker-compose --profile monitoring up -d

# Access services
# - App: http://localhost:8080
# - Prometheus: http://localhost:9090
# - Grafana: http://localhost:3000
```

### Kubernetes

Example deployment:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: easyapply-app
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: app
        image: easyapply-app:latest
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: API_KEY
          valueFrom:
            secretKeyRef:
              name: easyapply-secrets
              key: api-key
```

## 📊 Monitoring

### Actuator Endpoints

- `/actuator/health` - Application health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

### Logging

Structured logging with SLF4J:
```
2024-06-17 10:30:45 - INFO  - Creating resume for candidate: John Doe
2024-06-17 10:30:46 - INFO  - Resume created with id: 1
2024-06-17 10:31:00 - INFO  - Evaluating application: resumeId=1, jobId=1
2024-06-17 10:31:02 - INFO  - Match score calculated: 0.85
```

### Metrics

- Application uptime
- Request rates and latencies
- Match score distribution
- Automation success rates
- Database connection pool metrics

## 💻 Development

### Building
```bash
cd gulla-sample-test
mvn clean install
```

### Running Tests
```bash
mvn test
```

### Code Quality
```bash
mvn verify
```

### Database Migrations

Create new migration:
```bash
# Create file: src/main/resources/db/migration/V2__description.sql
```

Flyway automatically applies migrations on startup.

## 🧪 Testing

### Unit Tests
```bash
cd gulla-sample-test
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Manual API Testing

Use provided Postman collection or curl:
```bash
# Health check
curl http://localhost:8080/actuator/health

# Create resume
curl -X POST http://localhost:8080/api/v1/resumes \
  -H "Content-Type: application/json" \
  -d '{"candidateName":"Test User","summary":"Test","skills":"Java","yearsExperience":5}'
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## ⚠️ Important Notes

### LinkedIn Terms of Service

**Warning**: Automated interaction with LinkedIn may violate their Terms of Service. This tool is provided for educational purposes. Use at your own risk. Consider:

- Manual review before submission
- Rate limiting to avoid detection
- Compliance with LinkedIn's automation policies
- Legal implications in your jurisdiction

### Data Privacy

- Comply with GDPR and local data protection laws
- Implement data retention policies
- Secure storage of personal information
- User consent for data processing

## 📄 License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Playwright team for browser automation capabilities
- Open source community for various dependencies

## 📞 Support

For issues and questions:
- GitHub Issues: https://github.com/balugulla/Hello-world/issues
- Email: support@example.com

---

**Built with ❤️ using Spring Boot and modern Java practices**
