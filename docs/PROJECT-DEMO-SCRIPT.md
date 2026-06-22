# Task Management System Demo Script

Use this script for a `7-10` minute recording.

## 0:00 - 0:30 | Intro

**Say:**
"Hi, I’m Shivam, and this is my Task Management System backend project.  
It’s a Spring Boot application focused on production-style backend engineering, not just CRUD."

**Show:**
- `README.md`
- Highlight features section quickly

## 0:30 - 1:20 | Architecture Overview

**Say:**
"The stack includes Spring Boot, PostgreSQL, Redis, Kafka, JWT security, RBAC, rate limiting, audit logging, soft delete, Swagger, Docker, and CI/CD.  
The architecture is layered: controller, service, repository, with event-driven notifications using Kafka."

**Show:**
- `compose.yaml`
- `src/main/java/.../service/TaskService.java`
- `src/main/java/.../event/TaskEventPublisher.java`

## 1:20 - 2:00 | Local Infrastructure Up

**Say:**
"I run all dependencies locally with Docker Compose: Postgres, Redis, Kafka, and Zookeeper."

**Show terminal:**
```bash
docker compose up -d
docker compose ps
```

## 2:00 - 2:40 | Start Application

**Say:**
"Now I’ll start the Spring Boot app and verify health."

**Show terminal:**
```bash
./mvnw spring-boot:run
```

**In another terminal:**
```bash
curl http://localhost:8080/actuator/health
```

## 2:40 - 3:20 | Swagger + Auth

**Say:**
"I use Swagger for API testing and documentation. Security is JWT-based."

**Show in browser:**
- `http://localhost:8080/swagger-ui/index.html`
- `POST /api/auth/login`

**Say:**
"I’ll log in and use the JWT in protected endpoints."

## 3:20 - 4:30 | RBAC Demo (USER vs ADMIN)

**Say:**
"RBAC is enforced in service/security layers.  
A USER cannot access admin APIs, while ADMIN can."

**Show:**
1. Login as USER and call `GET /api/admin/tasks/deleted` -> `403`
2. Login as ADMIN and call same endpoint -> `200`

**Fallback line if needed:**
"If this fails, it usually means wrong role mapping in token claims."

## 4:30 - 5:20 | Task Workflow + Status Engine

**Say:**
"Tasks follow validated transitions: `TODO -> IN_PROGRESS -> COMPLETED`."

**Show:**
1. Create task via `POST /api/users/{userId}/tasks`
2. Update status via `PATCH /api/tasks/{id}/status` to `IN_PROGRESS`
3. Try invalid transition (for example `TODO -> COMPLETED`) -> `400`

**Say:**
"This is protected by business validation in the service layer."

## 5:20 - 6:00 | Cache-Aside + Eviction

**Say:**
"For reads, I use cache-aside with Redis.  
Repeated reads avoid DB hits, and writes evict task cache to prevent stale data."

**Show:**
- `TaskService` annotations: `@Cacheable` and `@CacheEvict`
- Mention tests for cache miss and eviction behavior

## 6:00 - 6:50 | Rate Limiting

**Say:**
"The API uses Bucket4j-based rate limiting per IP.  
When request volume exceeds capacity, the API returns HTTP 429 with structured JSON."

**Show:**
- `RateLimitingFilter.java`
- Optional rapid request test in Postman runner

## 6:50 - 7:40 | Audit Logging + Soft Delete

**Say:**
"Important actions are persisted in audit logs for accountability.  
Delete is soft by default, with restore and admin hard-delete support."

**Show:**
- Create/update/delete task
- `GET /api/audit-logs`
- `GET /api/admin/tasks/deleted`

## 7:40 - 8:30 | Async Event Flow (Kafka -> Digest)

**Say:**
"Task updates publish events asynchronously.  
Consumers store pending notifications, and a scheduler creates digest emails."

**Show:**
- `TaskEventPublisher.java`
- `NotificationConsumer.java`
- `NotificationDigestScheduler.java`

**Say:**
"This decouples core task writes from notification delivery."

## 8:30 - 9:20 | Testing + CI/CD + Terraform

**Say:**
"I added focused tests for security boundaries, state transitions, caching, and async flow.  
CI/CD runs on GitHub Actions, and I provision AWS EC2 using Terraform."

**Show:**
```bash
./mvnw test
```

Open:
- `.github/workflows/ci-cd.yml`
- `infrastructure/terraform/main.tf`
- `docs/AWS-EC2-TERRAFORM-DEPLOY.md`

## 9:20 - 9:40 | Closing

**Say:**
"This project demonstrates secure API design, production concerns like caching and observability, and event-driven backend architecture.  
Thanks for watching."

---

## Pre-Recording Checklist

Run before recording:

```bash
docker compose up -d
./mvnw clean test
./mvnw spring-boot:run
```

Open these tabs in advance:
- Swagger UI
- Postman collection
- `TaskService.java`
- `SpringSecurity.java`
- `RateLimitingFilter.java`
- `TaskEventPublisher.java`
- GitHub Actions workflow
- Terraform files

## Backup Plan (If Live Demo Breaks)

If runtime issue appears, say:
"The runtime dependency is temporarily unavailable, so I’ll continue with the verified test suite and code walkthrough that validates the same behavior."

Then show:
```bash
./mvnw -Dtest=AdminControllerSecurityTest,RateLimitingFilterTest,TaskServiceStateEngineTest,TaskServiceCachingTest,NotificationConsumerTest,NotificationDigestSchedulerTest test
```
