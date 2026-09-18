# Student Grading App with LLMs — CLAUDE.md

## What this project does

An AI-powered educational chat assistant called **EduBot** that gives students formative feedback on their practice work. Students submit text prompts and/or images of their work; the app forwards the request to OpenAI's GPT-4o-mini, which responds as an educational assistant — estimating rubric scores and giving feedback — but is system-prompted to refuse help with live tests or exams.

## Architecture

Three-service Docker Compose stack:

| Service | Tech | Port |
|---------|------|------|
| `frontend` | Angular 17 (standalone), Tailwind CSS, served by Nginx | 4200 (80 inside) |
| `backend` | Spring Boot 3.4.4 / Java 17, Spring Security + JWT | 8080 |
| `db` | MySQL 8.0 | 3306 |

## Directory layout

```
.
├── backend/                    # Spring Boot app
│   ├── src/main/java/com/webapp/backend/
│   │   ├── controller/         # AuthController, ChatController, UserController
│   │   ├── model/              # User entity, UserRole enum (STUDENT | TEACHER)
│   │   ├── repository/         # UserRepository (JPA)
│   │   ├── service/            # AuthService, UserService + impls
│   │   ├── config/             # SecurityConfig, CorsConfig, JwtAuthenticationFilter
│   │   └── util/               # JWTUtil, LoginRequest, RegisterRequest
│   └── src/main/resources/application.properties
├── frontend/src/app/
│   ├── components/             # chat, landing, login, navbar, register
│   ├── service/                # AuthService, ChatService, UserService
│   ├── guard/                  # AuthGuard
│   ├── pipe/                   # MarkdownPipe (marked + DOMPurify)
│   ├── interface/              # User interface
│   ├── utils/consts.ts         # API base URLs
│   └── app.routes.ts           # /welcome, /login, /register, /chat
├── docker-compose.yml
├── Dockerfile-backend
├── Dockerfile-frontend
└── .env                        # secrets (see below)
```

## Running the project

### With Docker Compose (recommended)

Requires `.env` in the project root with these variables:

```
DB_NAME=student_db
DB_USERNAME=root
DB_PASSWORD=<password>
DDL_AUTO=update
SPRING_SECURITY=DEBUG
WEBAPP_BACKEND=DEBUG
JWT_SECRET=<long random string>
OPENAI_API_KEY=<your OpenAI key>
OPENAI_URL=https://api.openai.com/v1/chat/completions
```

```bash
docker-compose up --build
# Frontend → http://localhost:4200
# Backend  → http://localhost:8080
```

### Without Docker (local dev)

**Backend** — requires MySQL running locally on port 3306 with database `student_db`:
```bash
cd backend
./mvnw spring-boot:run
```
Set `openai.api.key` and `openai.api.url` in `backend/src/main/resources/application.properties` (or use environment variables).

**Frontend:**
```bash
cd frontend
npm install
npm start        # serves on http://localhost:4200
```

## Key API endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | none | Register (name, email, password, role) |
| POST | `/api/auth/login` | none | Login → returns JWT string |
| GET | `/api/auth/me` | JWT | Current user info |
| POST | `/api/chat` | JWT | Send prompt + optional images → AI response |

The chat endpoint accepts `multipart/form-data` with a `prompt` field and optional `images` files.

## How the LLM call works

`ChatController` (`backend/.../controller/ChatController.java`):
1. Validates JWT via Spring Security filter before the handler runs.
2. Builds an OpenAI `gpt-4o-mini` chat completion request with a fixed system message that constrains the assistant to formative feedback only.
3. Encodes uploaded images as base64 data URLs and attaches them as `image_url` content parts.
4. Returns `{ "response": "<assistant text>" }` — max 700 tokens.

## Frontend chat flow

`ChatComponent` sends the prompt + files via `ChatService.sendPrompt()`, which POSTs `multipart/form-data` with the JWT in the `Authorization` header. The response string is then "streamed" character-by-character via an RxJS `interval(5ms)` for a typewriter effect. AI responses are rendered as Markdown (via `marked`) and sanitized with DOMPurify before being set as `innerHTML`.

## Authentication

- Passwords hashed with BCrypt.
- JWT generated/validated by `JWTUtil` using the `JWT_SECRET` env var.
- `JwtAuthenticationFilter` intercepts every request; `/api/auth/**` is whitelisted.
- Frontend stores the JWT in `localStorage` and attaches it as a Bearer token on every API call.

## Frontend API base URLs

Defined in `frontend/src/app/utils/consts.ts` — hardcoded to `http://localhost:8080`. Update these if deploying to a non-localhost environment.

## Database

Single `users` table managed by Hibernate (`ddl-auto=update`). Columns: `id`, `name`, `email`, `password` (bcrypt hash), `role` (enum string: `STUDENT` or `TEACHER`).

## Build artifacts

- Backend JAR: `backend/target/backend-0.0.1-SNAPSHOT.jar`
- Frontend build output: `frontend/dist/frontend/browser/` (Angular 17 application builder; served by Nginx in Docker)

---

## CI/CD — GitHub Actions

Pipeline file: `.github/workflows/ci-cd.yml`

**Trigger:** every push or PR to `main`.

**Jobs:**

| Job | What it does |
|-----|-------------|
| `build-and-push` | Builds both Docker images and pushes to DockerHub (`howlinman/backend`, `howlinman/frontend`). Tagged with `:latest` and `:<git-sha>`. Runs on `ubuntu-latest`. |
| `deploy` (CD bonus) | Applies all K8s manifests to the local Minikube cluster. Runs on `self-hosted` (this Mac), since a GitHub-hosted cloud runner can't reach a cluster on `localhost`. |

**GitHub repository secrets required:**

| Secret | Purpose |
|--------|---------|
| `DOCKERHUB_USERNAME` | `howlinman` |
| `DOCKERHUB_TOKEN` | DockerHub access token (not password) |
| `DB_USERNAME` | MySQL root username |
| `DB_PASSWORD` | MySQL root password |
| `JWT_SECRET` | JWT signing secret |
| `OPENAI_API_KEY` | OpenRouter API key (OpenAI-compatible) |

> The `deploy` job runs directly on the Minikube host as a **self-hosted runner**, so it reads `~/.kube/config` (already set up by `minikube start`) instead of a stored kubeconfig secret — that config would go stale every time Minikube regenerates its certs. One-time setup: repo Settings → Actions → Runners → "New self-hosted runner" (macOS), follow the generated `./config.sh` command, then run `./run.sh` (or install it as a background service with `./svc.sh install && ./svc.sh start`) on this machine while Minikube is running.

---

## Kubernetes (Minikube)

All manifests live in `k8s/`. Namespace: **`edueval`**.

### Prerequisites

```bash
minikube start
minikube addons enable ingress
# Add to /etc/hosts:
echo "$(minikube ip)  edueval.local" | sudo tee -a /etc/hosts
```

### Apply manifests (manual)

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
# Fill in real values in k8s/secret.yaml first, then:
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/mysql-service.yaml
kubectl apply -f k8s/mysql-statefulset.yaml
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/backend-service.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl apply -f k8s/frontend-service.yaml
kubectl apply -f k8s/ingress.yaml
```

App is reachable at `http://edueval.local` once all pods are ready:
```bash
kubectl get pods -n edueval -w
```

### Manifest overview

| File | Kind | Description |
|------|------|-------------|
| `namespace.yaml` | Namespace | `edueval` |
| `configmap.yaml` | ConfigMap | Non-sensitive config (DB name, log levels, OpenAI URL) |
| `secret.yaml` | Secret | Sensitive values — **fill before applying, do not commit real values** |
| `mysql-statefulset.yaml` | StatefulSet | MySQL 8.0 with 2 Gi PVC |
| `mysql-service.yaml` | Service (headless) | Stable DNS for MySQL pod (`mysql-0.mysql.edueval`) |
| `backend-deployment.yaml` | Deployment | Spring Boot API — env vars from ConfigMap + Secret |
| `backend-service.yaml` | Service | ClusterIP on port 8080 |
| `frontend-deployment.yaml` | Deployment | Angular SPA served by Nginx |
| `frontend-service.yaml` | Service | ClusterIP on port 80 |
| `ingress.yaml` | Ingress | `edueval.local/api` → backend; `edueval.local/` → frontend |

### How API routing works in K8s

`consts.ts` uses relative URLs (`/api/auth`, `/api/chat`, `/api/users`). The Ingress NGINX controller routes `/api/*` traffic directly to the backend Service, and `/` to the frontend Service. The frontend Nginx container's `proxy_pass` for `/api` is only active in Docker Compose (where there is no Ingress).

### Local dev without Docker

```bash
cd frontend && npm start          # uses proxy.conf.json → proxies /api to localhost:8080
cd backend && ./mvnw spring-boot:run
```