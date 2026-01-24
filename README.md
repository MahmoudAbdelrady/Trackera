# Trackera

Trackera is a web-based worklog tracking and task management tool designed to help teams and individuals monitor tasks, track time, and improve productivity. It integrates with Jira, supports Excel worklog uploads, and offers a modern UI.

🌐 **Live App:** [https://trackera.mdevs.cloud](https://trackera.mdevs.cloud)

---

## Table of Contents

1. Project Overview
2. Tech Stack
3. Features
4. Setup & Installation
5. Running Locally
6. Deployment
7. Frontend
8. API
9. Contribution Guidelines

---

## Project Overview

Trackera is designed to provide a centralized platform for managing worklogs, task estimation, and time tracking. It allows seamless integration with Jira to improve productivity.

### Architecture Diagram

![Architecture Diagram](/docs/arch-diagram.png)

---

## Tech Stack

- Frontend: React.js, Vite.
- Backend: Java Spring Boot
- Database: MariaDB
- Cache: Redis
- Message Broker: RabbitMQ (background job processing)
- Authentication: JWT, OAuth2 (Google - Jira)
- Deployment: Docker, Docker Compose, Nginx
- CI/CD: GitHub Actions

---

## Features

- OAuth2 & traditional authentication
- Jira integration for tasks
- Worklog tracking & month summaries
- Excel-based worklog uploads
- Rate-limiting & security features

---

## Setup & Installation

### Prerequisites

- Java 17+
- Node.js 20+
- Docker & Docker Compose
- Git

### Clone Repository

```bash
git clone https://github.com/mahmoudabdelrady/trackera.git
cd trackera
```

### Environment Variables

Copy the following `.env.example` to `.env` and fill in credentials:

```
# Database
DB_USERNAME=
DB_ROOT_PASSWORD=
DB_PASSWORD=
DB_DATABASE=

# Redis
REDIS_PASSWORD=

# Email
MAIL_USERNAME=
MAIL_PASSWORD=

# App Config
FRONTEND_URL=
APP_BASE_URL=
HASHER_SECRET_KEY=
HASHER_ENCRYPTION_KEY=
ACCESS_TOKEN_SECRET=
REFRESH_TOKEN_SECRET=
ACCESS_TOKEN_MAX_AGE=
REFRESH_TOKEN_MAX_AGE=
JOB_MAX_FAILURES=
JOB_BATCH_PAGE_SIZE=

# OAuth2
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
JIRA_CLIENT_ID=
JIRA_CLIENT_SECRET=

# RabbitMQ
RABBITMQ_USER=
RABBITMQ_PASS=
RABBITMQ_PREFETCH=20
RABBITMQ_CONCURRENT_CONSUMERS=5
RABBITMQ_MAX_CONCURRENT_CONSUMERS=10

# Frontend
VITE_TRACKERA_BACKEND_URL=
VITE_TRACKERA_MAX_FILE_SIZE=5
```

---

## Running Locally

Using Docker Compose:

```bash
docker-compose -f docker-compose.local.yml up --build
```

- Backend: `http://localhost:8080`
- Frontend: `http://localhost`

---

## Deployment

- Use `docker-compose.prod.yml` for production
- Configure Cloudfare for DNS + CDN and SSL via Let's Encrypt
- Configure Nginx for frontend SPA and reverse proxy backend
- CI/CD via GitHub Actions

---

## Frontend

See [FRONTEND.md](docs/FRONTEND.md) for:

- Folder structure
- Routing & state management
- Styling & theming
- Key components

---

## API

API documentation is available via Swagger UI at `/app-docs/swagger-ui.html` for interactive API documentation.

---

## Contribution Guidelines

- Follow code style conventions
- Use feature branches for new work
- Submit PRs to `dev` branch
- Test the feature before submitting

---
