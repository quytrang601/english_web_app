# 🌟 IELTS & English Mastery Platform — Fullstack Monorepo

[![Java](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Next.js](https://img.shields.io/badge/Next.js-16.2-black?style=for-the-badge&logo=next.js&logoColor=white)](https://nextjs.org/)
[![React](https://img.shields.io/badge/React-19.2-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17%20%2B%20pgvector-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-v4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)

A modern, production-grade web platform for comprehensive English language acquisition and IELTS Academic/General Training preparation. Built using a **Modular Monolith** architecture with Clean Architecture / DDD patterns on the backend (Java 21 LTS + Spring Boot 3.4.4) paired with a high-performance **Next.js 16 (React 19)** App Router frontend.

---

## 📑 Table of Contents

1. [Project Overview & Core Features](#-project-overview--core-features)
2. [Architectural Philosophy](#-architectural-philosophy)
3. [Monorepo Directory Structure & Navigation Guide](#-monorepo-directory-structure--navigation-guide)
4. [Technology Stack & Exact Pinned Versions](#-technology-stack--exact-pinned-versions)
5. [Getting Started & Local Development](#-getting-started--local-development)
6. [Development Roadmap & Team Division](#-development-roadmap--team-division)
7. [Engineering Standards & Git Guidelines](#-engineering-standards--git-guidelines)
8. [Recommended Architectural Best Practices](#-recommended-architectural-best-practices)

---

## 🎯 Project Overview & Core Features

The platform is designed to provide students and test-takers with an adaptive, feedback-driven IELTS learning ecosystem:

- 📝 **Full-Length IELTS Mock Tests:** Timed, authentic test simulation for Academic and General Training across Reading, Listening, Writing, and Speaking with automated scoring.
- 🗂️ **Spaced Repetition Flashcards (SM-2):** Cognitive science-backed vocabulary retention algorithm with difficulty ratings (0–5), ease-factor adjustments, and daily review queues.
- 🤖 **AI Writing Evaluation:** Automated band scoring (Task Achievement/Response, Coherence & Cohesion, Lexical Resource, Grammatical Range & Accuracy) with pinpoint line-by-line grammar feedback and rewrites.
- 🎙️ **Speaking Practice Room:** Real-time speech-to-text (STT) transcription, fluency analysis, pronunciation metrics, and band descriptor evaluation.
- 🎧 **Audio Dictation Drills:** Word-by-word transcription exercises using real-time audio playback and Levenshtein distance error-tracking for listening accuracy.
- ⚡ **Economy & Gamification:** "Sparks" virtual energy system, daily streak maintenance, Focus Bar mechanics, achievements, league leaderboards, and tiered subscriptions (Free with ads vs. Premium Pro).
- 🔍 **Hybrid Semantic Search:** Vector similarity search (`pgvector`) combined with full-text keyword indexing (`tsvector`) for rapid discovery across sample essays, vocabulary, and reading passages.

---

## 🏛️ Architectural Philosophy

The application follows a **Modular Monolith** pattern:

```
┌────────────────────────────────────────────────────────┐
│               Next.js 16 Client (React 19)             │
└───────────────────────────┬────────────────────────────┘
                            │ REST API / JWT / SSE
┌───────────────────────────▼────────────────────────────┐
│          Spring Boot 3.4.4 Application (Monolith)      │
│                                                        │
│  ┌───────────────────┐        ┌─────────────────────┐  │
│  │   Module: auth    │◄──────►│    Module: user     │  │
│  └───────────────────┘        └─────────────────────┘  │
│  ┌───────────────────┐        ┌─────────────────────┐  │
│  │   Module: content │◄──────►│ Module: assessment  │  │
│  └───────────────────┘        └─────────────────────┘  │
│  ┌───────────────────┐        ┌─────────────────────┐  │
│  │  Module: writing  │◄──────►│  Module: flashcard  │  │
│  └───────────────────┘        └─────────────────────┘  │
│             ▲                           ▲              │
│             └─────────────┬─────────────┘              │
│                           │                            │
│           ┌───────────────▼───────────────┐            │
│           │   Shared Kernel (`common/`)   │            │
│           │ BaseEntity, GlobalException,  │            │
│           │ Security, Mocks, DomainEvents │            │
│           └───────────────────────────────┘            │
└───────────────────────────┬────────────────────────────┘
                            │
     ┌──────────────────────┼──────────────────────┐
     │                      │                      │
┌────▼──────────────┐ ┌─────▼──────────┐ ┌─────────▼────────┐
│  PostgreSQL 17    │ │    Redis 7     │ │ S3 / MinIO Store │
│ (w/ pgvector)     │ │ (Cache & Limits│ │ (Audio / PDFs)   │
└───────────────────┘ └────────────────┘ └──────────────────┘
```

### Key Principles:
1. **Bounded Context Isolation:** Each business module (`auth`, `user`, `content`, `flashcard`, `assessment`, `writing`, etc.) is self-contained with its own API controllers, application services, domain models, and repositories. Modules never directly query another module's database tables or internal entities.
2. **Loosely Coupled Inter-Module Communication:** Cross-module interactions happen strictly via public service interfaces or asynchronous Spring application events (`DomainEventPublisher`).
3. **Shared Kernel (`common/`):** Contains cross-cutting infrastructure: `BaseEntity` (audit fields), `GlobalExceptionHandler` (RFC 7807 Problem Details), security filters, and provider mocks.
4. **Mock-First Integration Strategy:** External third-party providers (LLM for essay checking, STT for speaking, TTS for dictation audio, Stripe for payments) have local mock adapters out of the box (`common/infrastructure/*/mocks/`). Developers can boot and test the entire platform without paid external API keys.

---

## 📁 Monorepo Directory Structure & Navigation Guide

```text
english_web_app/                                        # REPOSITORY ROOT
├── README.md                                           # Master documentation (You are here)
├── structure.md                                        # Detailed Monorepo architecture & DDD boundaries (VN)
│
├── docs/                                               # 📚 CENTRAL DOCUMENTATION HUB
│   └── product-specifications/                         # Consolidated Product & Feature Specs
│       ├── feature-flows/                              # 11 End-to-End User & API Feature Flows
│       │   ├── 01-auth-and-user-profile-flow.md        # Authentication, JWT, profile lifecycle
│       │   ├── 02-subscription-and-ads-flow.md         # Stripe plans, Sparks billing, Free-tier ads
│       │   ├── 03-ielts-mock-tests-flow.md             # Timed 4-skill mock test runner
│       │   ├── 04-sample-essays-library-flow.md        # Band 8.0+ essays & vocabulary highlights
│       │   ├── 05-flashcards-spaced-repetition-flow.md # SuperMemo-2 (SM-2) spaced repetition algorithm
│       │   ├── 06-writing-check-ai-flow.md             # AI essay rubric scoring & inline correction
│       │   ├── 07-dictation-exercises-flow.md          # Word-level audio dictation & diff scorer
│       │   ├── 08-speaking-practice-room-flow.md       # Speaking room STT, fluency & pronunciation
│       │   ├── 09-cefr-reference-library-flow.md       # CEFR vocabulary & grammar reference tree
│       │   ├── 10-skill-practice-drills-flow.md        # Micro-drills (MCQ, Matching, Fill-in-blanks)
│       │   └── 11-semantic-search-and-i18n-flow.md     # pgvector embeddings & next-intl localization
│       ├── pricing-and-gamification/                   # 13 Pricing, Economy & Gamification Docs
│       │   ├── 00-index.md                             # Gamification architecture summary
│       │   ├── 01-roles-and-access.md                  # RBAC: GUEST, FREE, PRO, ADMIN
│       │   ├── 02-pricing-and-monetization.md          # Subscription pricing tiers & currency
│       │   ├── 03-sparks-credit-system.md              # Sparks credit replenishment & consumption
│       │   ├── 04-focus-bar.md                         # Daily study session timer & bonus XP
│       │   ├── 05-gamification-streaks-challenges.md   # Daily streaks, streak freeze, weekly challenges
│       │   ├── 06-achievements-and-titles.md           # 50+ unlockable badges & prestige titles
│       │   ├── 07-leaderboard-and-leagues.md           # Bronze-to-Diamond weekly league rankings
│       │   ├── 08-character-system.md                  # Learner avatars & level progression
│       │   ├── 09-minigames.md                         # Vocab Match, Word Scramble, Speed Quiz
│       │   ├── 10-skill-practice-module.md             # Micro-learning exercise blueprints
│       │   ├── 11-content-bundles.md                   # Specialized vocabulary & test pack bundles
│       │   └── 12-admin-system.md                      # Back-office moderation & analytics
│       └── feature-list.md                             # Comprehensive feature inventory & tech mapping
│
├── frontend/                                           # 💻 CLIENT WEB APPLICATION (NEXT.JS 16)
│   ├── app/                                            # Next.js App Router (pages & server layouts)
│   ├── components/                                     # UI Component Library (shadcn/ui + custom)
│   ├── hooks/                                          # Custom React hooks & TanStack Query hooks
│   ├── lib/                                            # API client, utilities, and constants
│   ├── public/                                         # Static assets (images, audio samples, icons)
│   ├── styles/                                         # Tailwind CSS v4 stylesheets
│   └── package.json                                    # Frontend dependencies & scripts
│
└── backend/                                            # ⚙️ BACKEND APPLICATION (SPRING BOOT 3.4.4)
    ├── pom.xml                                         # Maven build configuration & dependencies
    ├── task-mvp.md                                     # 12-Week MVP Development Roadmap (Person A & B)
    ├── programming-guidelines.md                       # Clean Code, Git Branching Strategy & Cheat Sheet
    └── src/
        ├── main/
        │   ├── java/com/ieltsplatform/
        │   │   ├── IeltsPlatformApplication.java       # Main Spring Boot Application Entrypoint
        │   │   │
        │   │   ├── common/                             # 🛡️ Shared Kernel (Used across all modules)
        │   │   │   ├── base/                           # BaseEntity (@MappedSuperclass with audit fields)
        │   │   │   ├── config/                         # SecurityConfig, RedisConfig, WebConfig
        │   │   │   ├── event/                          # DomainEventPublisher & base events
        │   │   │   ├── exception/                      # GlobalExceptionHandler, ApiException
        │   │   │   ├── infrastructure/                 # External service ports & local mock adapters
        │   │   │   │   ├── email/mocks/                # Local Email Sender Mock
        │   │   │   │   ├── llm/mocks/                  # Mock AI client for essay & speaking checks
        │   │   │   │   ├── media/mocks/                # Local/S3 storage & TTS audio generator mocks
        │   │   │   │   ├── payment/mocks/              # Mock Stripe payment gateway
        │   │   │   │   └── stt/mocks/                  # Mock Speech-to-Text transcriber
        │   │   │   └── security/                       # JwtAuthFilter, UserPrincipal, RateLimitFilter
        │   │   │
        │   │   └── modules/                            # 📦 Domain Modules (11 Bounded Contexts)
        │   │       ├── auth/                           # Registration, Login, JWT tokens, Passwords
        │   │       ├── user/                           # User Profiles, Roles, Preferences
        │   │       ├── content/                        # Reading Passages & Question Bank Management
        │   │       ├── listening/                      # Listening Sections, Audio Tracks & Transcripts
        │   │       ├── writing/                        # Essay Submissions & AI Scoring Engine
        │   │       ├── speaking/                       # Speaking Practice Rooms & Pronunciation Scorer
        │   │       ├── flashcard/                      # Vocab Decks, SM-2 Spaced Repetition Engine
        │   │       ├── assessment/                     # Timed Test Sessions & Objective MCQ Scorers
        │   │       ├── dictation/                      # Dictation audio items & Levenshtein Scorer
        │   │       ├── essaybank/                      # Band 8.0+ Sample Essays Library
        │   │       └── subscription/                   # Subscriptions, Sparks credits & Usage limits
        │   │
        │   └── resources/
        │       ├── application.yml                     # Central Spring Boot configuration
        │       └── db/migration/                       # Flyway SQL schema migrations
        │
        └── test/
            ├── java/com/ieltsplatform/
            │   ├── IeltsPlatformApplicationTests.java  # Health endpoint & context boot tests
            │   ├── common/                             # Unit tests for shared kernel components
            │   └── modules/                            # Module-specific unit & controller slice tests
            └── resources/
                └── application-test.yml                # In-memory test profile configuration
```

---

## 🚀 Technology Stack & Exact Pinned Versions

| Layer / Area | Technology | Pinned Version | Rationale & Responsibility |
| :--- | :--- | :--- | :--- |
| **Backend Runtime** | **Java** | `21 LTS` (`release 21`) | Modern LTS release with Virtual Threads, Pattern Matching, Record patterns |
| **Backend Framework** | **Spring Boot** | `3.4.4` | Latest stable Spring Boot 3 line built on Spring Framework 6.2 |
| **Build & Packaging** | **Apache Maven** | `3.9.x+` | Deterministic dependency resolution, Surefire test execution, standard JAR packaging |
| **Web Layer** | **Spring Web MVC** | `3.4.4` | High-throughput REST API controllers, content negotiation, HTTP status mapping |
| **Security & Auth** | **Spring Security** | `3.4.4` | Stateless JWT authentication, BCrypt password hashing, RBAC endpoint authorization |
| **ORM / Data Access** | **Spring Data JPA / Hibernate** | `6.6.x` | Strongly typed repositories, audited entities, automatic query generation |
| **Database Migrations** | **Flyway** | `11.x` | Forward-only, versioned SQL database migrations executed upon application startup |
| **Relational Database** | **PostgreSQL** | `17.x` | Primary ACID relational datastore for users, tests, submissions, and payments |
| **Vector Search** | **pgvector** | `0.8.x` | Vector embeddings storage & cosine distance search directly within PostgreSQL |
| **Cache & Rate Limiting**| **Redis** | `7.x (Alpine)` | High-speed cache for auth sessions, rate limiting, and weekly leaderboards |
| **Object Storage** | **S3-compatible (AWS / MinIO)**| — | Audio listening tracks, user speaking recordings, and exported essay PDFs |
| **Testing (Backend)** | **JUnit 5 (Jupiter)** | `5.11.x` | Core testing framework for unit tests and parameterized test suites |
| **Mocking Framework** | **Mockito** | `5.14.x` | Subclass mock-maker configured for Java 21/25 agent compatibility |
| **Web Slice Testing** | **Spring MockMvc** | `3.4.4` | Fast HTTP controller testing without full servlet container startup overhead |
| **Frontend Framework** | **Next.js (App Router)** | `16.2.x` | React Server Components, Turbopack bundler, edge-ready API routes |
| **UI Library** | **React** | `19.2.x` | Concurrent rendering, modern Actions, and latest React compiler optimizations |
| **Language (Frontend)**| **TypeScript** | `5.7+` | Strict mode enabled for comprehensive end-to-end type safety |
| **Styling Engine** | **Tailwind CSS** | `v4.x` | High-performance CSS-first utility styling paired with CSS variables |
| **Component Kit** | **shadcn/ui + Radix UI** | Latest | Accessible, headless UI primitives styled seamlessly with Tailwind |
| **Client State / Cache**| **TanStack Query (v5)** | `5.x` | Declarative server-state caching, background revalidation, and optimistic updates |
| **AI Assessment** | **Google Gemini Flash / Claude**| Current API | Low-latency essay scoring, grammar error classification, band estimates |
| **Speech-to-Text** | **Deepgram Nova-3 / Whisper** | Current API | Real-time audio transcription and phoneme alignment for Speaking drills |
| **Text-to-Speech** | **Cartesia Sonic 3.5 / Neural TTS**| Current API | Ultra-realistic, natural voice generation for listening & dictation exercises |
| **Payments** | **Stripe API** | Current API | Global credit card processing, recurring subscriptions, and webhooks |

---

## 🛠️ Getting Started & Local Development

### 1. Prerequisites
Ensure the following tools are installed on your machine:
- **Java Development Kit (JDK):** Version 21 LTS or newer (`java -version`)
- **Apache Maven:** Version 3.9.x or newer (`mvn -version`)
- **Node.js:** Version 22.x LTS (`node -v`)
- **Docker & Docker Compose:** Optional for local Postgres/Redis containers

### 2. Backend Setup & Commands

All backend commands are executed using system `mvn` from the `backend/` directory:

```bash
# 1. Navigate to the backend directory
cd backend

# 2. Compile source code without running tests
mvn clean compile

# 3. Run the automated test suite (Unit & MockMvc tests)
mvn clean test

# 4. Start the Spring Boot development server
mvn spring-boot:run
```

Once running, the backend server starts on `http://localhost:8080`. Verify system health:

```bash
curl http://localhost:8080/api/health
# Response: {"status":"UP","application":"ielts-platform-backend","timestamp":"..."}
```

### 3. Running Single Tests

```bash
# Run a specific test class
mvn test -Dtest=IeltsPlatformApplicationTests

# Run a specific test method
mvn test -Dtest=IeltsPlatformApplicationTests#healthEndpoint_ReturnsUpStatus
```

### 4. Database & Infrastructure (Docker Compose)

To spin up local PostgreSQL 17 (with `pgvector`) and Redis 7:

```bash
docker compose up -d postgres redis
```

---

## 🗺️ Development Roadmap & Team Division

The project is structured around a **12-Week MVP Roadmap** with full vertical feature slicing between two engineers:

- 👤 **Person A (Fresher Backend Engineer):** Core platform infrastructure, Authentication & Session Security, User Profiles, Audio Dictation Drills (Levenshtein Diff), and Subscription/Sparks limits.
- 👤 **Person B (Intern Backend Engineer):** Learning & Practice Engines, Reading Content Management, Assessment & Scoring Engine, SM-2 Spaced Repetition Flashcards, and AI Writing Submission & Scoring.

👉 **Read the full plan:** [backend/task-mvp.md](file:///Users/apple/Coding-projects/english_web_app/backend/task-mvp.md)

---

## 📖 Engineering Standards & Git Guidelines

To maintain clean code quality and zero Git merge conflicts:
- **Code Standards:** Explicit constructor injection (`@RequiredArgsConstructor`), zero wildcard imports (`java.util.*`), DTOs as Java `record`, and RFC 7807 unified error responses.
- **Git Branching Strategy:** `feature/person-a/<task-name>` and `feature/person-b/<task-name>`.
- **Commit Format:** Conventional Commits (`feat(...)`, `fix(...)`, `test(...)`, `refactor(...)`).
- **Cheat Sheet:** Daily workflow commands, conflict resolution via `git rebase`, and stash recipes.

👉 **Read the guidelines:** [backend/programming-guidelines.md](file:///Users/apple/Coding-projects/english_web_app/backend/programming-guidelines.md)

---

## 💡 Recommended Architectural Best Practices

1. **Keep Mock Adapters Active in Local Profiles:**
   Never block local backend development on external API keys. Keep the `default` / `dev` profile wired to `MockLlmClient`, `MockSttClient`, and `MockPaymentGateway`.
2. **Strict Boundary Verification:**
   Do not allow package-private classes from one module to be imported by another. Use Spring's `@ApplicationModuleListener` or modular testing patterns.
3. **Database Migration Discipline:**
   Never modify an existing Flyway migration file (`V1__...sql`) after it has been executed. Always create a new sequential version (`V2__...sql`, `V3__...sql`).
4. **Always Test Before Pushing:**
   Run `mvn clean test` before every `git push`. Keep test execution under 5 seconds so the feedback loop remains instant.
