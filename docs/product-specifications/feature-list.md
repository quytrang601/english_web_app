# **IELTS / English Learning Platform — Planning Document**

Scope: requirements + architecture + tech stack mapping only. No code. Stack anchor: Next.js (React, TypeScript) frontend, Java + Spring Boot + Maven backend, PostgreSQL primary DB.

---

## **1. Tech Stack Summary (stable, current as of July 2026)**

| Layer | Choice | Version (stable, July 2026) | Notes |
| ----- | ----- | ----- | ----- |
| Frontend framework | Next.js (App Router) | 16.2.x | Turbopack default bundler, Cache Components, requires Node 20+ |
| UI library | React | 19.2.x | Ships with Next 16.2 |
| Language (frontend) | TypeScript | 5.7+ | Strict mode on |
| Node runtime | Node.js | 22.x LTS | Required by Next.js 16 tooling |
| Styling | Tailwind CSS | 4.x | Utility-first, pairs with shadcn/ui |
| Component kit | shadcn/ui + Radix primitives | latest | Accessible primitives |
| State/data fetching | TanStack Query (React Query) | 5.x | Client cache for API calls |
| Backend framework | Spring Boot | 4.1.x / 3.x | Built on Spring Framework; Java 17+ minimum |
| Language (backend) | Java | 21 / 25 | Java 21 LTS / 25 configured |
| Build tool | Maven | 3.9.x | As requested |
| Core web layer | Spring MVC (Spring Web) | bundled w/ Boot | REST controllers |
| Security | Spring Security | 7.x / 6.x | OAuth2 Resource Server + JWT |
| Data access | Spring Data JPA + Hibernate | Hibernate 7.x | ORM over PostgreSQL |
| Migrations | Flyway | 11.x | Versioned SQL migrations |
| Primary database | PostgreSQL | 17.x | Relational + vector extension host |
| Vector search | pgvector extension on PostgreSQL | 0.8.x | No separate vector DB needed unless scaling demands it |
| Cache / session / rate-limit | Redis | 7.x | Sessions, rate limiting, leaderboard caches |
| Object storage | S3-compatible (AWS S3 / Cloudflare R2 / MinIO self-hosted) | — | Audio files, essay PDFs, user recordings |
| Search (non-semantic, keyword) | PostgreSQL full-text search (tsvector) or OpenSearch | OpenSearch 2.x if scaled | Filter/keyword search on top of semantic |
| Async/job queue | Spring Batch or Quartz Scheduler | bundled | Nightly jobs: word-frequency stats, ranking recompute |
| Message broker (if scaled) | Apache Kafka or RabbitMQ | 3.x | For scoring pipeline, notifications |
| AI/LLM tasks (writing/speaking scoring, embeddings) | Anthropic Claude API (Claude Sonnet/Haiku) or OpenAI API | current | Essay scoring, speaking transcript scoring, embeddings |
| Speech-to-text | Whisper (self-hosted) or cloud STT (Google Speech-to-Text, AWS Transcribe, Azure Speech) | current | Speaking room transcription |
| Text-to-speech (audio generation) | Azure/Google/AWS Neural TTS, or ElevenLabs | current | Listening/dictation audio generation |
| Ads | Google AdSense / Google Ad Manager | — | Free-tier monetization |
| Payments | Stripe | current API | Subscription tier |
| i18n | next-intl (frontend), Spring MessageSource (backend errors) | current | Multilanguage UI |
| CI/CD | GitHub Actions | — | Build/test/deploy pipelines |
| Containerization | Docker + Docker Compose (dev), Kubernetes (prod, optional) | — | Application containers |
| Observability | Spring Boot Actuator + Micrometer + Prometheus + Grafana | current | Metrics, health checks |
| Logging | SLF4J + Logback, structured JSON logs | current | Structured logging support |
| API docs | springdoc-openapi (OpenAPI 3 / Swagger UI) | current | Auto-generated API docs |
| Testing (backend) | JUnit 5, Mockito, Testcontainers | current | Testcontainers spins real Postgres for integration tests |
| Testing (frontend) | Vitest/Jest + Playwright | current | Unit + E2E |
| Hosting | Vercel (frontend) + AWS/GCP/Render/Fly.io (backend + Postgres) | — | Cloud hosting targets |

**Note on dependencies:** Spring Boot and Next.js version lines move fast; re-check `spring.io/blog` and `nextjs.org/blog` right before you start building, and pin exact patch versions in `pom.xml` / `package.json` rather than using version ranges.

---

## **2. High-Level Architecture**

```text
[Next.js Frontend] --HTTPS/REST/JSON--> [Spring Boot API Gateway/Monolith]  
                                          |-- Auth Module (Spring Security + JWT/OAuth2)  
                                          |-- User/Profile Module  
                                          |-- IELTS Mock Test Module (Listening/Reading/Writing/Speaking)  
                                          |-- Skill Practice & Training Module (Dictation, Speaking Drills, Writing Drills, Reading Drills, Grammar Quizzes)  
                                          |-- CEFR Reference Library & Knowledge Base (Grammar, Vocabulary, Tips & Tricks)  
                                          |-- Flashcard/Vocabulary Module  
                                          |-- Essay Bank Module  
                                          |-- Writing Check Module (calls LLM API)  
                                          |-- Speaking Room Module (calls STT + LLM API)  
                                          |-- Ads/Subscription Module  
                                          |-- Search Module (keyword + semantic via pgvector)  
                                          |  
                               [PostgreSQL + pgvector] [Redis] [S3/Object Storage]  
                                          |  
                          [External APIs: LLM (Claude/OpenAI), TTS, STT, Stripe, AdSense]
```

Start as a modular monolith (single Spring Boot app, package-by-feature) — split into microservices later only if a specific module (e.g., speaking/audio processing) needs independent scaling.

---

## **3. Functional Requirements (atomic, per feature) + Tech Mapping**

Each requirement is atomic: one testable statement. Grouped by feature area.

### **3.1 User Accounts & Auth**

* **FR-1.1:** User can sign up with email + password.  
  * *Tech:* Next.js form (React Hook Form + Zod validation) -> Spring Boot `AuthController` -> Spring Security `PasswordEncoder` (BCrypt) -> PostgreSQL `users` table.  
* **FR-1.2:** User can sign up via Google OAuth2.  
  * *Tech:* Spring Security OAuth2 Client, Google as provider.  
* **FR-1.3:** User can sign up via Facebook OAuth2.  
  * *Tech:* Spring Security OAuth2 Client, Facebook as provider.  
* **FR-1.4:** User can log in with email + password and receive a session.  
  * *Tech:* Spring Security + JWT access token (short-lived) + refresh token (httpOnly cookie, long-lived).  
* **FR-1.5:** User can log out, invalidating the refresh token.  
  * *Tech:* Refresh token blacklist/rotation stored in Redis.  
* **FR-1.6:** User can reset a forgotten password via emailed link.  
  * *Tech:* Spring Boot Mail (JavaMailSender) or transactional email provider (SendGrid/AWS SES); signed, expiring reset token.  
* **FR-1.7:** User can verify their email address after signup.  
  * *Tech:* Signed token link + email provider.  
* **FR-1.8:** User can update profile (name, avatar, native language, target band score).  
  * *Tech:* `UserController` PATCH endpoint; avatar upload to S3.  
* **FR-1.9:** User can view their full activity history (tests taken, scores, essays submitted, flashcards learned).  
  * *Tech:* `user_activity_log` table, paginated REST endpoint, React table component.  
* **FR-1.10:** System enforces password complexity rules at signup.  
  * *Tech:* Zod schema (frontend) + Bean Validation `@Pattern` (backend), enforced server-side regardless of client.  
* **FR-1.11:** System rate-limits login attempts to prevent brute force.  
  * *Tech:* Redis counter per IP/account + Spring Security lockout policy.  
* **FR-1.12:** User can delete their account and associated personal data.  
  * *Tech:* Soft-delete flag + scheduled hard-delete job (GDPR-style compliance).

### **3.2 Subscription & Ads (Free vs Paid)**

* **FR-2.1:** Free-tier (non-paying) users see ad placements on designated pages (dashboard, between test sections, essay bank).  
  * *Tech:* Google AdSense/Ad Manager script embedded conditionally in Next.js layout based on `user.subscriptionTier === 'free'`.  
* **FR-2.2:** Paying users see zero ads anywhere in the app.  
  * *Tech:* Server-side check on `subscriptionTier` before rendering ad slot component (avoid client-side-only hiding, which is trivially bypassed).  
* **FR-2.3:** User can subscribe to a paid plan via Stripe Checkout.  
  * *Tech:* Stripe Checkout Session created by Spring Boot, redirect from Next.js.  
* **FR-2.4:** System updates user's subscription status via Stripe webhook.  
  * *Tech:* Spring Boot `/webhooks/stripe` endpoint, signature verification.  
* **FR-2.5:** Free-tier users are capped on daily mock-test attempts; paid users are unlimited.  
  * *Tech:* Redis daily counter keyed by `userId:date`.

### **3.3 IELTS Mock Tests — 4 Skills (Official Exam Simulation Mode)**

**General**

* **FR-3.1:** User can select a full mock test (all 4 skills) or an individual skill mock test attempt.  
* **FR-3.2:** System records start time, end time, and per-part timing for each mock attempt.  
* **FR-3.3:** User can pause and resume a test session within a time limit (or disallow pausing for exam-simulation mode — configurable).  
* **FR-3.4:** System auto-submits the test when the official time limit expires.  
* **FR-3.5:** User receives a band score breakdown (listening/reading exact scores, speaking/writing estimated band scores) after submission.  
* **FR-3.6:** User can review a completed test with correct answers and explanations.  
  * *Tech (all above):* `test_attempts`, `test_answers` tables; Spring Boot `TestSessionController`; timer logic client-side (React) with server-side authoritative re-validation on submit.

**Listening (Mock Test)**

* **FR-3.7:** User can play an audio clip once (per official IELTS constraints) per section.  
  * *Tech:* `<audio>` element streaming from S3/CDN signed URL; Next.js audio player component.  
* **FR-3.8:** System scores listening answers automatically (multiple choice, fill-in-blank, matching).  
  * *Tech:* Rule-based exact/fuzzy string match in Spring Boot for fill-in-blank (Levenshtein distance tolerance for minor typos).

**Reading (Mock Test)**

* **FR-3.9:** User can view a reading passage alongside its questions (split-screen).  
  * *Tech:* Next.js two-pane layout component.  
* **FR-3.10:** System scores reading answers automatically (MCQ, T/F/NG, matching headings, fill-in-blank).  
  * *Tech:* Rule-based comparison in Spring Boot.  
* **FR-3.11:** User can highlight/note/delete highlight passage text during the test (non-scored, UX aid).  
  * *Tech:* Client-side only (React state), not persisted unless requested.

**Writing (Mock Test — Task 1 & Task 2)**

* **FR-3.12:** User can view a Task 1 prompt (chart/graph/diagram/letter) and Task 2 prompt (essay).  
  * *Tech:* Prompt images stored in S3, served via Next.js `<Image>`.  
* **FR-3.13:** User can type an essay response in a basic editor with live word count.  
  * *Tech:* React text editor (e.g., Tiptap), word-count computed client-side.  
* **FR-3.14:** User's essay is submitted to the Writing Check module for scoring (see Section 3.6).  
* **FR-3.15:** User can see time remaining and word-count warnings (under/over IELTS thresholds).

**Speaking (Mock Test)**

* **FR-3.16:** User can complete simulated Part 1/2/3 speaking test with recorded responses.  
  * *Tech:* Browser `MediaRecorder` API (React) -> upload audio blob to S3 via Spring Boot presigned URL.  
* **FR-3.17:** User's spoken response is transcribed and scored (reuses Speaking Practice Room pipeline).  
* **FR-3.18:** System gives user 1 minute prep time for Part 2 cue card, tracked with visible countdown.  
  * *Tech:* Client-side timer component.

### **3.4 Sample Essays (Reading Library)**

* **FR-4.1:** User can browse a library of sample IELTS essays filtered by band score (6/7/8/9), topic, and task type.  
  * *Tech:* PostgreSQL `sample_essays` table with indexed columns (`band`, `topic`, `task_type`); Next.js filter UI.  
* **FR-4.2:** User can search sample essays by keyword or topic meaning (semantic), not just exact text match.  
  * *Tech:* pgvector similarity search — see Section 5.  
* **FR-4.3:** User can view a sample essay with band-score annotations (why it scored that band, highlighted strong phrases).  
  * *Tech:* Pre-annotated JSON stored per essay (curated or LLM-generated once, human-reviewed), rendered with inline highlight components.  
* **FR-4.4:** User can save a sample essay to a personal "favorites" list.  
  * *Tech:* `user_saved_essays` join table.  
* **FR-4.5:** User can copy useful vocabulary/phrases from a sample essay directly into their flashcard deck.  
  * *Tech:* Selection-to-flashcard action, reuses Flashcard module API (see Section 3.5).

### **3.5 Flashcard Feature (Site-Wide Vocabulary Tracker)**

* **FR-5.1:** User can add a new word/phrase to their personal flashcard deck from anywhere in the site (reading passage, essay, dictation transcript, speaking room, manual entry).  
  * *Tech:* Reusable "Add to Flashcards" React component (highlight-to-add) triggers a shared API call regardless of source page.  
* **FR-5.2:** When a user adds a word, system checks whether that word already exists in the user's deck and warns/merges instead of duplicating.  
  * *Tech:* Backend lookup by normalized lemma (lowercase, lemmatized form) before insert; use a lemmatization library or an LLM call for normalization.  
* **FR-5.3:** User can tag each flashcard with where it was learned (e.g., "Reading Passage #14", "Speaking Room", "External — TV show", "External — book", custom free-text tag).  
  * *Tech:* `flashcards.source_tag` column (enum + free-text override).  
* **FR-5.4:** User can view all flashcards in one central "My Vocabulary" page, filterable by tag, date added, mastery level.  
  * *Tech:* `flashcards` table indexed by `user_id`, paginated + filtered React table.  
* **FR-5.5:** User can review flashcards using spaced-repetition scheduling (SM-2 algorithm or similar).  
  * *Tech:* `flashcard_reviews` table storing next-review-date, ease-factor, interval; scheduling logic in Spring Boot service layer.  
* **FR-5.6:** User can mark a flashcard as "mastered" to stop it appearing in daily review.  
* **FR-5.7:** User can edit or delete a flashcard.  
* **FR-5.8:** System auto-suggests example sentences and definitions for a newly added word.  
  * *Tech:* LLM call (Claude/OpenAI) at creation time, cached in `word_definitions` table so repeated additions of the same word across users don't re-call the LLM.  
* **FR-5.9:** User can see word frequency/importance indicator (e.g., "appears in 12% of Task 2 essays") to prioritize learning.  
  * *Tech:* Precomputed nightly batch job (Spring Batch) over corpus of essays/passages, stored in `word_frequency_stats`.

### **3.6 Writing Check (AI Essay Evaluation)**

* **FR-6.1:** User submits an essay (from mock test or standalone practice) for automated scoring.  
* **FR-6.2:** System returns an estimated band score broken down by the 4 IELTS writing criteria: Task Achievement/Response, Coherence & Cohesion, Lexical Resource, Grammatical Range & Accuracy.  
* **FR-6.3:** System returns inline feedback (highlighted spans with comments) on grammar errors, awkward phrasing, and repeated vocabulary.  
* **FR-6.4:** System returns 2-3 concrete improvement suggestions per criterion.  
* **FR-6.5:** User can view essay-scoring history and track band-score trend over time.  
  * *Tech (3.6 overall):* Spring Boot `WritingCheckService` calls an LLM (Claude Sonnet recommended for quality on nuanced scoring) with a structured prompt requesting JSON output (criteria scores + spans + suggestions); response persisted in `essay_submissions` + `essay_feedback` tables; optionally supplement with a dedicated grammar-checking library (LanguageTool, self-hostable Java-based grammar checker) for fast, deterministic grammar flags alongside the LLM's holistic scoring.

### **3.7 Dictation Exercises (Skill Practice Module)**

* **FR-7.1:** User can select a dictation exercise by CEFR level (A1–C2) or IELTS band-equivalent.  
* **FR-7.2:** User listens to an audio clip and types what they hear.  
* **FR-7.3:** User can replay the audio a limited number of times (configurable per level — fewer replays at higher levels).  
* **FR-7.4:** System scores the dictation by comparing typed text to the transcript, tolerating minor punctuation/casing differences.  
  * *Tech:* Backend diff algorithm (word-level Levenshtein / diff-match-patch) highlighting missed/incorrect words.  
* **FR-7.5:** User can slow down audio playback speed (0.75x, 0.5x) for lower levels.  
  * *Tech:* `<audio playbackRate>` control in React.  
* **FR-7.6:** System shows a word-by-word correction view after submission.

### **3.8 Speaking Practice Room & Shadowing (Skill Practice Module)**

* **FR-8.1:** User can start a speaking session and receive an AI-generated or bank-sourced question (Part 1/2/3 style) or topic drill.  
* **FR-8.2:** User records an audio response via microphone or practices sentence shadowing against official audio models.  
* **FR-8.3:** System transcribes the audio to text.  
  * *Tech:* STT service (Whisper self-hosted for cost control, or cloud STT for reliability).  
* **FR-8.4:** System scores the response against IELTS speaking criteria: Fluency & Coherence, Lexical Resource, Grammatical Range & Accuracy, Pronunciation.  
  * *Tech:* LLM call on the transcript (+ optionally audio-level pronunciation features via a pronunciation-scoring API/library) — see Section 3.6-style pipeline.  
* **FR-8.5:** User receives a follow-up question dynamically based on their previous answer (conversational Part 3 simulation).  
  * *Tech:* LLM-driven conversation loop, session context stored server-side (Redis for short-lived session state).  
* **FR-8.6:** User can play back their own recording alongside the transcript.  
* **FR-8.7:** User can view historical speaking session scores and recordings (subject to storage retention policy).

### **3.9 CEFR Reference Library & Knowledge Base (A1–C2 Levels)**

* **FR-9.1:** User can browse a comprehensive learning reference library organized by CEFR level (A1–C2) and topic category (Grammar Rules, Topic Vocabulary Lists, Exam Tips & Tricks, Skill Strategies).  
* **FR-9.2:** Each level section displays expected grammar structures (e.g. Present Perfect at B1, Inversion at C1), topic word lists with definitions/examples, and tactical exam tips.  
* **FR-9.3:** User can mark a knowledge-base article or topic as "completed" to track learning progress.  
* **FR-9.4:** System recommends next-level content once a user completes prerequisites or hits a mock-test band threshold.  
  * *Tech:* `knowledge_base_articles` table with level & category tags; `user_progress` table; rule engine in Spring Boot (if band >= X and articles-complete >= Y -> unlock next level UI badge).  
* **FR-9.5:** User can search knowledge-base articles semantically across grammar, vocabulary, and tips (e.g., "how to describe pie charts" or "when to use past perfect" surfaces the exact reference article).  
  * *Tech:* pgvector semantic search — see Section 5.

### **3.10 Writing Practice & Drills (Skill Practice Module)**

* **FR-10.1:** User can select standalone, untimed writing drills including sentence building, paragraph structure practice, Task 1 chart description drills, and common grammar error spotters.  
* **FR-10.2:** User receives real-time AI feedback on isolated paragraphs or Task 1 chart descriptions without having to complete a full 60-minute essay test.  
* **FR-10.3:** System provides template sentence structures (e.g. "Comparing data over time", "Expressing opinion") for instant practice.  
* **FR-10.4:** User can compare their drafted paragraph against high-band (Band 8+) reference model paragraphs.  
* **FR-10.5:** User can save key phrases generated in writing drills directly to their flashcard deck.

### **3.11 Reading Speed & Isolated Question Drills (Skill Practice Module)**

* **FR-11.1:** User can practice isolated question types (e.g. practicing *only* "Matching Headings", *only* "True/False/Not Given", or *only* "Summary Completion") without taking a full passage exam.  
* **FR-11.2:** User can engage in speed-reading drills with guided visual timers to improve reading words-per-minute (WPM) speed.  
* **FR-11.3:** System provides immediate answer explanations and text-span evidence highlighting directly after each question attempt.  
* **FR-11.4:** User can track accuracy percentages per question type to identify personal weak spots (e.g., 85% on MCQ, 45% on T/F/NG).  
* **FR-11.5:** User can highlight unknown words in reading drill passages and add them directly to their flashcard deck.

### **3.12 Grammar & Vocabulary Exercises (Skill Practice Module)**

* **FR-12.1:** User can complete interactive, level-based grammar and vocabulary quizzes (fill-in-the-blank, error identification, collocation matching).  
* **FR-12.2:** Quizzes are organized by CEFR levels (A1–C2) and tagged to matching articles in the CEFR Reference Library (Section 3.9).  
* **FR-12.3:** System provides immediate rule explanations for incorrect answers, linking back to the relevant Grammar Reference Library guide.  
* **FR-12.4:** System tracks user mastery score per grammar topic (e.g., Conditionals 90%, Relative Clauses 60%).  
* **FR-12.5:** User can add failed vocabulary quiz words directly into their personal flashcard review deck.

### **3.13 Multilanguage Support**

* **FR-13.1:** User can switch the site UI language (e.g., English, Vietnamese, Spanish, etc.) from a persistent selector.  
  * *Tech:* `next-intl` with locale routing (`/en/...`, `/vi/...`); translation JSON files per locale.  
* **FR-13.2:** System remembers the user's language preference across sessions.  
  * *Tech:* Stored in `users.locale` column + cookie fallback for anonymous users.  
* **FR-13.3:** Backend error messages and emails are localized based on user's stored locale.  
  * *Tech:* Spring `MessageSource` with locale-specific `.properties` bundles.  
* **FR-13.4:** Core learning content (IELTS materials) remains in English by design; only UI chrome, instructions, and navigation are translated.

### **3.14 Landing Page (Marketing)**

* **FR-14.1:** Landing page displays value proposition, feature highlights, and pricing tiers to unauthenticated visitors.  
* **FR-14.2:** Landing page includes a signup call-to-action above the fold.  
* **FR-14.3:** Landing page includes social proof (testimonials, band-score improvement stats).  
* **FR-14.4:** Landing page is fully server-rendered/static for SEO.  
  * *Tech:* Next.js Static Site Generation (SSG) or ISR for the marketing route group; separate from the authenticated app shell.  
* **FR-14.5:** Landing page tracks conversion analytics (signup funnel).  
  * *Tech:* Analytics tool (PostHog, Plausible, or GA4) event tracking.

### **3.15 Semantic Search (Cross-Cutting)**

* **FR-15.1:** User can search across reading passages, sample essays, knowledge-base articles, and flashcards using natural-language queries that match by meaning, not just keyword.  
* **FR-15.2:** Search results are ranked by relevance combining semantic similarity and keyword match (hybrid search).  
* **FR-15.3:** New content (essays, passages, articles) is automatically embedded and indexed on creation/update (covered in detail in Section 5).

---

## **4. Non-Functional Requirements (atomic)**

### **Performance**

* **NFR-1:** API p95 response time under 300ms for non-AI endpoints (excludes LLM calls).  
* **NFR-2:** LLM-backed endpoints (writing check, speaking scoring) return within 15 seconds, with a loading state shown to the user.  
* **NFR-3:** Audio files are served via CDN with sub-second time-to-first-byte.  
* **NFR-4:** Frontend pages achieve a Lighthouse performance score >= 85 on the landing page.

### **Scalability**

* **NFR-5:** Backend is stateless (JWT-based auth, no server-side session objects) to allow horizontal scaling behind a load balancer.  
* **NFR-6:** Database read replicas can be added for reporting/analytics workloads without touching the write path.  
* **NFR-7:** Audio/media storage scales independently of the application server (object storage, not local disk).

### **Security**

* **NFR-8:** All traffic is served over HTTPS/TLS 1.2+.  
* **NFR-9:** Passwords are hashed with BCrypt (or Argon2) and never stored/logged in plaintext.   
* **NFR-10:** All API inputs are validated server-side (Bean Validation annotations), independent of client-side validation.  
* **NFR-11:** JWT access tokens expire within 15 minutes; refresh tokens are rotated on use and stored in httpOnly, Secure, SameSite cookies.  
* **NFR-12:** Role-based access control distinguishes at least: anonymous, free user, paid user, admin.  
* **NFR-13:** Sensitive endpoints (payment, account deletion, admin) require re-authentication or step-up verification.  
* **NFR-14:** All dependencies are scanned for known CVEs in CI (OWASP Dependency-Check or GitHub Dependabot).  
* **NFR-15:** Rate limiting is applied per-user and per-IP on auth and AI-scoring endpoints to prevent abuse and cost overrun.  
* **NFR-16:** File uploads (audio, avatars) are validated by MIME type and size limit, and scanned before being served publicly.  
* **NFR-17:** CORS policy restricts API access to known frontend origins.  
* **NFR-18:** Secrets (API keys, DB credentials) are stored in a secrets manager (AWS Secrets Manager/Vault), never committed to source control.

### **Reliability**

* **NFR-19:** System has automated daily database backups with a tested restore procedure.  
* **NFR-20:** LLM/STT/TTS provider calls have a fallback or graceful degradation path (e.g., queue-and-retry) if the provider is down.  
* **NFR-21:** Uptime target of 99.5% for the core application (excludes third-party AI provider outages).

### **Usability / Accessibility**

* **NFR-22:** UI meets WCAG 2.1 AA color contrast and keyboard-navigation standards.  
* **NFR-23:** Mock test timers and audio controls are usable via keyboard alone.  
* **NFR-24:** Mobile-responsive layout for all core learning features (tests may recommend desktop for best experience but must remain usable on tablet/mobile).

### **Maintainability**

* **NFR-25:** Backend follows a modular package-by-feature structure to allow future extraction into microservices.  
* **NFR-26:** All public API endpoints are documented via OpenAPI/Swagger.  
* **NFR-27:** CI pipeline runs unit + integration tests on every pull request before merge.

### **Compliance / Data Privacy**

* **NFR-28:** User data deletion requests are fulfilled within a defined SLA (e.g., 30 days), supporting GDPR-style "right to be forgotten."  
* **NFR-29:** Audio recordings of user speech are retained only as long as needed for feature function/review, per a documented retention policy, and deletable on request.  
* **NFR-30:** Cookie consent banner is shown to comply with regional privacy law (GDPR/ePrivacy) before non-essential tracking cookies are set.

### **Cost Control (AI-specific)**

* **NFR-31:** LLM and STT/TTS API calls are cached where inputs repeat (e.g., same word definition, same TTS sentence) to control unit cost.  
* **NFR-32:** Free-tier usage caps (Section 3.2) limit AI-scoring cost exposure per unpaid user.

---

## **5. Where Semantic Search / Vector Database Is Used**

**Recommendation: use PostgreSQL + `pgvector` extension rather than a separate vector database.** At the scale of an IELTS learning platform (thousands to low millions of content rows and embeddings, not billions), pgvector inside your existing Postgres gives you transactional consistency with your relational data, one less system to operate, and is fast enough with an HNSW or IVFFlat index. Only consider a dedicated vector DB (Pinecone, Weaviate, Qdrant, Milvus) if you later need extreme-scale ANN search, multi-region replication of vectors specifically, or you outgrow what a single tuned Postgres instance can do.

Concrete places to store and query embeddings:

1. **Sample essay library search (FR-4.2):** Embed each essay's full text (or a summary) once on upload; store vector in `sample_essays.embedding`. Query: embed the user's search phrase, run cosine-similarity `ORDER BY embedding <=> :query_vector LIMIT n`.  
2. **Reading passage / knowledge-base semantic search (FR-9.5, FR-15.1):** Same pattern — embed article/passage chunks (split long articles into ~300-500 token chunks for better retrieval granularity), store in a `content_embeddings` table with a foreign key to the source content type + id.  
3. **Flashcard duplicate detection (FR-5.2):** Primarily solved via lemma/string normalization, but embeddings can catch near-duplicate phrases with different wording (e.g., "make a decision" vs "decide") if you want fuzzy duplicate detection beyond exact lemma match.  
4. **Word/phrase definition retrieval cache (FR-5.8):** Optional — embed word+context to detect "have we already generated a definition for a word used in a similar context" before calling the LLM again, saving cost.  
5. **Speaking/Writing feedback retrieval-augmented examples:** When generating improvement suggestions, you can retrieve similar high-band sample sentences via embedding similarity to show the user a concrete "here's how a band-8 response phrased this" example.  
6. **Hybrid search ranking (FR-15.2):** Combine Postgres full-text search (`ts_rank`) with vector similarity score in a weighted formula, or do candidate retrieval via `tsvector` and re-rank the top-N by embedding similarity (cheaper than pure vector search over the whole corpus).

**Embedding model choice:** Use a hosted embedding API (e.g., OpenAI `text-embedding-3-small/large`, or Voyage AI embeddings which are commonly paired with Anthropic-based pipelines) called from the Spring Boot backend at content-creation time; store the resulting float array in a pgvector column (dimension must match the model, e.g., 1536 or 1024).

---

## **6. Security Architecture (Spring Ecosystem Specifics)**

* **Spring Security 7.x / 6.x** as the core auth framework.  
* **Authentication:** Username/password via `DaoAuthenticationProvider` + BCrypt; OAuth2 login via `spring-boot-starter-oauth2-client` for Google/Facebook.  
* **Authorization:** JWT bearer tokens validated via `spring-boot-starter-oauth2-resource-server` (Nimbus JWT decoder), method-level security with `@PreAuthorize` for role checks (`ROLE_FREE`, `ROLE_PAID`, `ROLE_ADMIN`).  
* **CSRF:** Disabled for stateless JWT API (standard practice), but enabled for any server-rendered form endpoints if used.  
* **Password storage:** `BCryptPasswordEncoder` (or `Argon2PasswordEncoder` for stronger hashing if you want to invest the CPU cost).  
* **Secrets management:** Spring Cloud Vault or AWS Secrets Manager integration; never hardcode in `application.yml`.  
* **Input validation:** `spring-boot-starter-validation` (Jakarta Bean Validation) on all DTOs.  
* **Security headers:** Spring Security's header customization for `Content-Security-Policy`, `X-Content-Type-Options`, `Strict-Transport-Security`.  
* **Audit logging:** Spring Data Envers or a custom `@EntityListener` to track who-changed-what for admin/moderation actions.  
* **Dependency vulnerability scanning:** `owasp-dependency-check-maven` plugin run in CI.  
* **API rate limiting:** Bucket4j (Java rate-limiting library) integrated with Redis as the backing store, applied via a Spring filter.  
* **File upload security:** Validate content-type/magic-bytes server-side (not just file extension) before accepting to S3.

---

## **7. Where to Get Data (Content Sourcing)**

| Content type | Sourcing options |
| ----- | ----- |
| Reading passages | (a) License content from established IELTS prep publishers (requires licensing/permission, do not scrape/reproduce copyrighted book content without a license); (b) commission original passages from ESL content writers/freelancers matched to IELTS passage style; (c) use royalty-free public-domain texts adapted into IELTS-style questions by your own editorial/LLM-assisted process. |
| Sample essays | (a) Commission model essays from qualified IELTS tutors/examiners (best quality + legally clean); (b) generate first drafts with an LLM at target band levels, then have a human IELTS-certified reviewer edit and band-score-verify before publishing. |
| Listening scripts/audio | See Section 8 below (TTS generation is the practical route for original, licensable content). |
| Speaking test questions | Commission from ESL curriculum writers, or adapt publicly known IELTS speaking topic categories into original questions. |
| Vocabulary/word-frequency data | Public corpora: COCA (Corpus of Contemporary American English) academic/spoken data, British National Corpus, or the Oxford 3000/5000 word lists (check individual licensing terms) to seed word difficulty/frequency tiers. |
| Grammar/CEFR level content | CEFR official descriptors (Council of Europe, publicly published) as a curriculum skeleton; write your own explanatory content against those descriptors rather than copying textbook prose. |
| Dictation transcripts | Write your own short scripts per level, or license short-form audio scripts; do not use copyrighted podcast/movie transcripts without a license. |

**General rule across all content types:** anything that will be shown to end users at scale (passages, essays, scripts) should either be (1) originally written/commissioned by you, (2) properly licensed from a rights holder, or (3) sourced from explicitly public-domain/open-licensed material. Avoid scraping copyrighted IELTS prep sites or textbooks directly.

---

## **8. Audio for Listening / Dictation**

**Two viable approaches, often combined:**

1. **Generate new audio via Text-to-Speech (recommended primary approach for full control and to avoid licensing audio content):**  
   * Write your own listening scripts (monologues, conversations, lectures) matching IELTS listening section formats (Section 1: everyday conversation, Section 2: monologue, Section 3: academic discussion, Section 4: academic lecture).  
   * Feed scripts into a neural TTS provider with multiple distinct voices/accents to simulate multi-speaker conversations:  
     * **Azure AI Speech** (wide accent/voice selection, good for British/Australian/American IELTS-style accents)  
     * **Google Cloud Text-to-Speech** (Neural2/Studio voices)  
     * **AWS Polly** (Neural voices)  
     * **ElevenLabs** (very natural, good multi-voice/emotion control, higher cost)  
   * Batch-generate audio server-side (a Spring Boot job or a dedicated Python/Node script) at content-creation time, store the resulting MP3/OGG in S3, and reference the S3 URL from the `listening_sections`/`dictation_items` tables. You do not need to generate audio live per user request — generate once, serve many times.  
2. **License or record real human audio for higher authenticity (secondary/premium content):**  
   * Hire voice actors (Fiverr, Voices.com, or local ESL teachers) to record scripts in target accents — better prosody/naturalness than TTS for flagship content, at higher cost and slower turnaround.  
   * Use for a curated set of "premium" mock tests while TTS covers the long tail of practice/dictation content.

**Processing pipeline regardless of source:** normalize audio levels, transcode to a consistent format/bitrate (e.g., ffmpeg to 128kbps MP3 + a compressed streaming-friendly format), store both the audio file and its exact transcript (needed for dictation auto-scoring and for generating captions/answer keys).

---

## **9. High-Level Data Model (Key Entities)**

> **Base Entity Rule:** Every JPA `@Entity` in the application extends `com.ieltsplatform.common.base.BaseEntity`.  
> Metadata fields (`id` UUID, `created_at` Instant, `updated_at` Instant) are automatically inherited by all entities listed below.

* `users` (extends `BaseEntity`): `email`, `password_hash`, `oauth_provider`, `locale`, `subscription_tier`, `role`, `native_language`, `target_band`  
* `test_attempts` (extends `BaseEntity`): `user_id`, `skill`, `mode`, `started_at`, `submitted_at`, `overall_band_estimate`  
* `test_answers` (extends `BaseEntity`): `attempt_id`, `question_id`, `user_answer`, `is_correct`, `score`  
* `reading_passages` / `listening_sections` (extends `BaseEntity`): `level`, `topic`, `content_or_audio_url`, `transcript`, `embedding`  
* `sample_essays` (extends `BaseEntity`): `band_score`, `topic`, `task_type`, `content`, `embedding`, `source`  
* `essay_submissions` (extends `BaseEntity`): `user_id`, `essay_text`, `prompt_id`, `submitted_at`  
* `essay_feedback` (extends `BaseEntity`): `submission_id`, `criterion`, `score`, `comments_json`  
* `flashcards` (extends `BaseEntity`): `user_id`, `word`, `lemma`, `definition`, `example_sentence`, `source_tag`, `mastery_level`  
* `flashcard_reviews` (extends `BaseEntity`): `flashcard_id`, `next_review_at`, `ease_factor`, `interval_days`, `last_reviewed_at`  
* `dictation_items` (extends `BaseEntity`): `level`, `audio_url`, `transcript`  
* `speaking_sessions` (extends `BaseEntity`): `user_id`, `question_id`, `audio_url`, `transcript`, `feedback_json`  
* `knowledge_base_articles` (extends `BaseEntity`): `level`, `category` (GRAMMAR, VOCABULARY, TIPS_TRICKS, SKILLS), `title`, `content`, `embedding`  
* `practice_exercises` (extends `BaseEntity`): `module_type` (DICTATION, SPEAKING, WRITING, READING, GRAMMAR), `skill`, `cefr_level`, `title`, `content_json`  
* `practice_attempts` (extends `BaseEntity`): `user_id`, `exercise_id`, `user_answer`, `feedback_json`, `score`  
* `user_progress` (extends `BaseEntity`): `user_id`, `article_id_or_level`, `completed_at`  
* `content_embeddings` (extends `BaseEntity`): `content_type`, `content_id`, `embedding` — optional generic table if separate from content tables  
* `subscriptions` (extends `BaseEntity`): `user_id`, `stripe_customer_id`, `status`, `current_period_end`

---

## **10. Suggested Build Order (Planning-Level Only)**

1. Auth + user accounts + landing page (foundation, needed by everything else).  
2. CEFR Reference Library + Knowledge Base (simplest content-serving features, validates content pipeline).  
3. Flashcard module (cross-cutting, needed early since other features feed into it).  
4. Skill Practice Module — Reading & Dictation Drills (interactive exercises, rule-based).  
5. Reading + Listening mock tests (rule-based scoring, exam simulation).  
6. Semantic search (once content rows exist across library & passages).  
7. Skill Practice Module — Writing Drills & AI Writing Check (introduces LLM evaluation).  
8. Skill Practice Module — Speaking Drills & Practice Room (most complex: recording, STT, LLM scoring, conversational loop).  
9. Skill Practice Module — Grammar & Vocabulary interactive quizzes.  
10. Ads + subscription/payment gating.  
11. Multilanguage UI pass.
