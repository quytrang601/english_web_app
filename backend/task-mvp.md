# **IELTS Platform — Backend-Only MVP Plan (4 Weeks)**

Frontend is explicitly out of scope for these 4 weeks — both people build it together later. This plan covers backend only: architecture, full folder structure, component reusability guidelines, and a file-by-file task guide.

## **Team & Split**

* **Person A:** Auth, User, Dictation, Listening + all shared infrastructure (Security, Google Gemini LLM, Cartesia TTS, Deepgram STT, S3 storage clients), vector indexing pipeline, and production security hardening.
* **Person B** *(new to SWE)*: Docker environment setup, Content (Reading Passages), Assessment Engine, Writing AI scoring framework, Speaking audio pipeline, and Flashcards (SM-2 spaced repetition). Mostly CRUD by design; the genuinely harder spots are flagged with 🔶 below, and Person A is free to pair on those from Week 3 onward.

---

## **1. Folder Structure, Base Entity & Reusability Mandates**

### **7-Folder Module Architecture**
Every module (both people's) follows the same 7-folder shape:

```text
modules/<module_name>/
├── entities/          # JPA @Entity classes — ALL entities extend BaseEntity (Base Entity for metadata)
├── repository/        # Spring Data JPA interfaces — extends JpaRepository<Entity, Id>
├── dtos/              # Data transfer objects
│   ├── request/       # What the client sends
│   └── response/      # What you send back — never the entity directly
├── mapper/            # entity <-> DTO conversion, static helper methods
├── ports/             # Interfaces only — the service contract (e.g., IFlashcardService)
├── services/impl/     # The concrete implementation of each port (e.g., FlashcardServiceImpl)
└── controllers/       # REST endpoints — depend on the port interface, never the impl class
```

**Base Entity Mandate:** Every `@Entity` class across all modules MUST extend `com.ieltsplatform.common.base.BaseEntity`. This ensures consistent metadata tracking (`id` UUID, `createdAt` Instant, `updatedAt` Instant) across the entire application without duplicating standard metadata columns.

**Why `ports/` is separate from `services/impl/`:** the controller only ever imports the interface (`IFlashcardService`), never the concrete class (`FlashcardServiceImpl`). Spring wires the real implementation in at runtime. This is Dependency Inversion — you can swap or mock an implementation without ever touching the controller.

---

### **6 Component Reusability & Extensibility Design Patterns**

To ensure MVP components directly power post-MVP features (Dictation Drills, Reading Speed Drills, CEFR Reference Library, Grammar Quizzes) without code duplication or major refactoring, all backend components MUST adhere to these 6 core design patterns:

1. **🎧 Audio Media & TTS Synthesis Reuse (`AudioContent`)**:
   - **Embeddable Contract**: Encapsulate audio properties in an `@Embeddable` class `common/base/AudioContent.java` (`audioUrl`, `transcript`, `duration`, `cefrLevel`).
   - **Entity Composition**: Embedded directly using `@Embedded` in `ListeningSection`, `DictationItem`, `SpeakingSession`, and post-MVP `ListeningDrill`.
   - **Pipeline Adapter Contract**: Shared ports `ITtsClient` (Cartesia Sonic 3.5 API) and `IStorageClient` (AWS S3) work in tandem:
     1. `ITtsClient.synthesize(script, voiceId)` generates raw PCM/MP3 audio bytes + duration.
     2. `IStorageClient.upload(key, bytes, contentType)` persists audio to S3.
     3. Public URL and duration are stored into `AudioContent`.
   - **Reusability**: Listening Mock Tests, Dictation Exercises, Isolated Listening Drills, and Speaking Voice Prompts reuse the exact same audio generation and storage pipeline.

2. **🎯 Generic Answer Scoring Engine (`IAnswerScorer`)**:
   - **Strategy Pattern Contract**: Define `IAnswerScorer` interface in `common/ports/IAnswerScorer.java` (`ScoreResult score(Question question, String userAnswer)` and `QuestionType getSupportedType()`).
   - **Concrete Strategies**: Implemented as stateless Spring `@Component` beans in `common/services/impl/`:
     - `McqScorerImpl`: Exact option key match + distractor analysis.
     - `TfNgScorerImpl`: Case-insensitive normalization (`TRUE`, `FALSE`, `NOT_GIVEN`).
     - `FillBlankScorerImpl`: Case-insensitive, typo-tolerant Levenshtein distance match (<= 1 edit distance allowed for minor spelling errors).
   - **Strategy Registry**: `AnswerScorerRegistry` in `common/services/impl/` injects `Map<QuestionType, IAnswerScorer>` for dynamic strategy lookup.
   - **Zero-Duplication Drill Reuse**: Full `TestAttempt` (mock exams) and lightweight `PracticeAttempt` (isolated Reading Drills, Dictation Exercises, Grammar Quizzes) execute the exact same scoring strategy classes and registry, guaranteeing ZERO code duplication.

3. **🤖 Structured AI Evaluation Pipeline (`LlmPromptTemplate`)**:
   - **Template Contract**: Decouple raw LLM client calls (`ILlmClient`) from domain prompt formatting via `common/prompt/LlmPromptTemplate.java`:
     - Encapsulates system prompt, user prompt template variables, and Jackson JSON output schema.
   - **Structured JSON Parsing**: Output is mandated as strictly valid JSON parsed via Jackson `ObjectMapper` into typed evaluation DTOs (`EssayFeedbackResponse`, `SpeakingFeedbackResponse`).
   - **Extensible Pipeline**: Full Writing essays, Task 1 paragraph drills, and Speaking transcripts share the exact same Gemini LLM client (`ILlmClient`) and JSON parsing engine, differing only in the injected `LlmPromptTemplate`.

4. **🎴 Universal Selection-to-Flashcard Pipeline (`IFlashcardService`)**:
   - **Standardized Port Interface**: Define universal method signature in `modules/flashcard/ports/IFlashcardService.java`:
     `FlashcardResponse addWord(UUID userId, String word, SourceTag sourceTag, UUID sourceEntityId)`.
   - **Universal `SourceTag` Enum**: Extensible enum supporting MVP and post-MVP contexts:
     `READING_PASSAGE`, `SAMPLE_ESSAY`, `DICTATION`, `SPEAKING`, `KNOWLEDGE_BASE`, `QUIZ`, `EXTERNAL`, `MANUAL`.
   - **Global Cached `WordDefinition` Entity**:
     - `WordDefinition` entity (`@Entity` extending `BaseEntity`: `word` [unique constraint], `lemma`, `definition`, `exampleSentence`).
     - When `addWord()` is invoked:
       1. Normalize `word` to base lemma using NLP/Spring helper.
       2. Query `WordDefinitionRepository`. If cached definition exists in `word_definitions` table, reuse immediately.
       3. If missing, invoke `ILlmClient` (Gemini) once to generate definition + example sentence, then persist to `word_definitions`.
       4. Create `Flashcard` linked to `userId`, `lemma`, `SourceTag`, and `sourceEntityId`.
     - Guarantees zero duplicate LLM definition requests across all platform users.

5. **🔍 Event-Driven Vector Search Indexing (`IEmbeddingIndexer`)**:
   - **Domain Event & Interface**:
     - Interface `common/base/VectorIndexable.java`: Methods `getVectorEntityId()`, `getEntityType()`, `getEmbeddableText()`.
     - Record `common/events/ContentCreatedEvent.java(VectorIndexable entity)` extending `ApplicationEvent`.
   - **Event-Driven Pipeline**:
     1. When any entity implementing `VectorIndexable` (Reading Passage, Sample Essay, Knowledge Base Article) is created/updated, publish `ContentCreatedEvent`.
     2. `VectorIndexingEventListener` (`@EventListener @Async`) in `common/listeners/` intercepts event.
     3. Calls `IEmbeddingIndexer.index(entity)` which computes text embeddings via `ILlmClient` (Gemini Embedding API) and saves to PostgreSQL `pgvector` table (`content_embeddings`) via `PgVectorEmbeddingIndexerImpl`.
   - **Extensibility**: Post-MVP content modules automatically inherit semantic vector search by simply implementing `VectorIndexable` and firing `ContentCreatedEvent`.

6. **🔌 Interface-Driven Ports for Deferred AI Features (`IEssayScorer` / `ISpeakingScorer`)**:
   - **Decoupled Architecture**: Define strict interface contracts in `modules/writing/ports/IEssayScorer.java` and `modules/speaking/ports/ISpeakingScorer.java`.
   - **Dual Adapter Implementations**:
     - `MockEssayScorerImpl` / `MockSpeakingScorerImpl`: Fast, deterministic mock evaluation (rubric scores based on word count/heuristics) for local dev/testing (`@Profile("dev")` or `@Fallback`).
     - `LlmEssayScorerImpl` / `LlmSpeakingScorerImpl`: Production AI evaluation utilizing `ILlmClient` + `LlmPromptTemplate` (`@Profile("prod")` or `@Primary`).
   - **Zero-Touch Controllers**: `WritingCheckServiceImpl` and `SpeakingServiceImpl` depend strictly on the interface ports. Swapping between mock and live AI evaluation requires zero changes to controllers, service orchestration, or JPA database schemas.

---

## **2. Full Backend Folder Tree**

```text
backend/
├── Dockerfile                                         # [Person B] Docker container image build file
├── docker-compose.yml                                 # [Person B] Local multi-container setup (PostgreSQL 17 + Redis 7)
├── docker-compose.prod.yml                            # [Person A] Production Docker Compose setup
│
├── src/main/java/com/ieltsplatform/
│   ├── IeltsPlatformApplication.java                  # Spring Boot entrypoint; also where @EnableJpaAuditing goes
│   │
│   ├── common/                                         # [Person A] Shared kernel & Reusable Ports
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── OpenApiConfig.java
│   │   │   └── RedisConfig.java
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── ApiError.java
│   │   │   └── DomainException.java
│   │   ├── dtos/
│   │   │   └── ScoreResult.java                       # Standard scoring output DTO for IAnswerScorer
│   │   ├── prompt/
│   │   │   └── LlmPromptTemplate.java                 # Reusable JSON prompt strategy for Writing & Speaking LLM calls
│   │   ├── events/
│   │   │   └── ContentCreatedEvent.java               # Spring ApplicationEvent published on new content creation
│   │   ├── listeners/
│   │   │   └── VectorIndexingEventListener.java       # Async event listener invoking IEmbeddingIndexer
│   │   ├── base/
│   │   │   ├── BaseEntity.java                   # BASE ENTITY — ALL JPA entities extend this class for metadata
│   │   │   ├── AudioContent.java                  # REUSABLE AUDIO EMBEDDABLE (audioUrl, transcript, cefrLevel)
│   │   │   └── VectorIndexable.java               # Interface for vector-indexable domain entities
│   │   ├── ports/
│   │   │   ├── ILlmClient.java                    # Shared Gemini LLM Port
│   │   │   ├── ITtsClient.java                    # Shared Cartesia TTS Port
│   │   │   ├── ISttClient.java                    # Shared Deepgram STT Port
│   │   │   ├── IStorageClient.java                # Shared S3 Storage Port
│   │   │   ├── IAnswerScorer.java                 # Generic Rule Engine Scoring Strategy (MCQ, TFNG, FillBlank)
│   │   │   └── IEmbeddingIndexer.java             # Event-Driven Vector Indexing Port (pgvector)
│   │   └── services/impl/
│   │       ├── GeminiLlmClientImpl.java          # [Person A] Google Gemini LLM API Adapter
│   │       ├── CartesiaTtsClientImpl.java        # [Person A] Cartesia Sonic 3.5 TTS API Adapter
│   │       ├── DeepgramSttClientImpl.java        # [Person A] Deepgram Nova-3 STT API Adapter
│   │       ├── S3StorageClientImpl.java          # [Person A] AWS S3 / MinIO Storage Adapter
│   │       ├── PgVectorEmbeddingIndexerImpl.java # [Person A] Event-driven pgvector indexing adapter
│   │       ├── AnswerScorerRegistry.java         # [Person B] Strategy registry for dispatching IAnswerScorer by QuestionType
│   │       ├── McqScorerImpl.java                # [Person B] Reusable MCQ Scoring Strategy
│   │       ├── TfNgScorerImpl.java               # [Person B] Reusable True/False/Not-Given Scoring Strategy
│   │       ├── FillBlankScorerImpl.java          # [Person B] Reusable Fill-in-Blank Scoring Strategy
│   │       └── RateLimitFilter.java              # [Person A] Redis token bucket rate limiting filter
│   │
│   └── modules/
│       ├── auth/                          # [Person A] entities/ repository/ dtos/ mapper/ (AuthMapper) ports/ services/impl/ controllers/
│       ├── user/                          # [Person A] entities/ repository/ dtos/ mapper/ (UserMapper) ports/ services/impl/ controllers/
│       ├── content/                       # [Person B] entities/ (ReadingPassage) repository/ dtos/ mapper/ (ContentMapper) ports/ services/impl/ controllers/
│       ├── assessment/                    # [Person B]
│       │   ├── entities/
│       │   │   ├── TestAttempt.java              # Full Mock Exam Attempt entity
│       │   │   ├── TestAnswer.java               # Mock Exam Question Answer entity
│       │   │   └── PracticeAttempt.java          # Lightweight Isolated Drill Attempt entity (Reuses IAnswerScorer)
│       │   ├── repository/
│       │   │   ├── TestAttemptRepository.java
│       │   │   ├── TestAnswerRepository.java
│       │   │   └── PracticeAttemptRepository.java
│       │   ├── dtos/
│       │   ├── mapper/
│       │   │   └── AssessmentMapper.java         # Reusable Assessment Mapper DTO <-> Entity
│       │   ├── ports/
│       │   │   └── IAssessmentService.java
│       │   ├── services/impl/
│       │   │   └── AssessmentServiceImpl.java
│       │   └── controllers/
│       ├── dictation/                     # [Person A] entities/ repository/ dtos/ mapper/ (DictationMapper) ports/ services/impl/ controllers/
│       ├── writing/                       # [Person B]
│       │   ├── entities/
│       │   │   ├── EssaySubmission.java          # Implements VectorIndexable
│       │   │   └── EssayFeedback.java
│       │   ├── repository/
│       │   ├── dtos/
│       │   ├── mapper/
│       │   │   └── WritingMapper.java            # Reusable Writing Mapper
│       │   ├── ports/
│       │   │   ├── IEssayScorer.java             # Decoupled Essay Scoring Port Interface
│       │   │   └── IWritingCheckService.java
│       │   ├── services/impl/
│       │   │   ├── MockEssayScorerImpl.java      # Fast Deterministic Stub (@Profile("dev"))
│       │   │   ├── LlmEssayScorerImpl.java       # Production Gemini AI Scorer (@Profile("prod") / @Primary)
│       │   │   └── WritingCheckServiceImpl.java
│       │   └── controllers/
│       ├── speaking/                      # [Person B]
│       │   ├── entities/
│       │   │   └── SpeakingSession.java          # Embeds AudioContent
│       │   ├── repository/
│       │   ├── dtos/
│       │   ├── mapper/
│       │   │   └── SpeakingMapper.java           # Reusable Speaking Mapper
│       │   ├── ports/
│       │   │   ├── ISpeakingScorer.java          # Decoupled Speaking Scoring Port Interface
│       │   │   └── ISpeakingService.java
│       │   ├── services/impl/
│       │   │   ├── MockSpeakingScorerImpl.java   # Fast Deterministic Stub (@Profile("dev"))
│       │   │   ├── LlmSpeakingScorerImpl.java    # Production Gemini AI Scorer (@Profile("prod") / @Primary)
│       │   │   └── SpeakingServiceImpl.java
│       │   └── controllers/
│       ├── listening/                     # [Person A] entities/ repository/ dtos/ mapper/ (ListeningMapper) ports/ services/impl/ controllers/
│       └── flashcard/                     # [Person B]
│           ├── entities/
│           │   ├── Flashcard.java                # Flashcard User Card entity
│           │   ├── FlashcardReview.java          # SM-2 Review Schedule entity
│           │   ├── SourceTag.java                # Universal Source Tag Enum
│           │   └── WordDefinition.java           # Global Cached Definition entity (word_definitions table)
│           ├── repository/
│           │   ├── FlashcardRepository.java
│           │   ├── FlashcardReviewRepository.java
│           │   └── WordDefinitionRepository.java # Repository for cached definitions
│           ├── dtos/
│           ├── mapper/
│           │   └── FlashcardMapper.java          # Reusable Flashcard Mapper
│           ├── ports/
│           │   ├── ISpacedRepetitionScheduler.java
│           │   └── IFlashcardService.java        # addWord(userId, word, sourceTag, sourceEntityId)
│           ├── services/impl/
│           │   ├── Sm2SchedulerImpl.java
│           │   └── FlashcardServiceImpl.java
│           └── controllers/
```

---

## **3. The Flashcard Feature, Defined (Person B's centerpiece)**

* **Add from anywhere:** a word/phrase can be added to the deck from a reading passage, an essay, a dictation transcript, a speaking transcript, a knowledge base article, or typed manually.  
* **Duplicate detection:** on add, normalize the word to its base form (lemma) and check if it already exists for that user before inserting.  
* **Source tagging:** every card remembers where it was learned using the `SourceTag` enum (`READING_PASSAGE, SAMPLE_ESSAY, DICTATION, SPEAKING, KNOWLEDGE_BASE, QUIZ, EXTERNAL, MANUAL`).  
* **Auto-generated definitions:** on add, call Google Gemini LLM for a definition + example sentence; cache by word in `word_definitions` so it's only generated once, not once per user.  
* **Spaced-repetition review:** SM-2 algorithm schedules the next review date based on how well the user recalled the card.  
* **Central vocabulary page (API only for now):** `GET /flashcards` lists everything, filterable by tag/mastery/date.  
* **Mastery tracking:** a card can be marked mastered to stop appearing in daily review.

**Data model (All entities extend `BaseEntity`):**

* `Flashcard` (Extends `BaseEntity`): `userId, word, lemma, definition, exampleSentence, sourceTag, masteryLevel` *(metadata: `id, createdAt, updatedAt` inherited)*  
* `FlashcardReview` (Extends `BaseEntity`, **one-to-one** with `Flashcard`): `flashcard (FK), nextReviewAt, easeFactor, intervalDays, lastReviewedAt` *(metadata: `id, createdAt, updatedAt` inherited)*

---

## **4. Week-by-Week File-Level Task Guide**

### **Week 1 — Foundation & Environment Setup**

> **Week 1 High-Level Overview:**  
> Establish core backend infrastructure, security, authentication, and generic scoring strategy interfaces (Person A), alongside Docker container environment setup, reading passage content serving, and initial test data seeding (Person B).

#### **Person A**

* **High-Level Task Overview:**
  * **Core Scope:** Construct the shared kernel (`common/` package) — Spring Security filter chain, JWT authentication, OpenAPI/Swagger configuration, Redis connection template, global exception handling (`GlobalExceptionHandler`), standard base entity (`BaseEntity`), reusable audio embeddable (`AudioContent`), vector indexing interface (`VectorIndexable`), JSON prompt template builder (`LlmPromptTemplate`), content event (`ContentCreatedEvent`), standardized scoring DTO (`ScoreResult`), and shared ports (`ILlmClient`, `ITtsClient`, `ISttClient`, `IStorageClient`, `IAnswerScorer`, `IEmbeddingIndexer`). Implement the full `auth` module (signup, login, token refresh, logout) using `AuthMapper`, and `user` entity schema.
  * **Key Goal:** Secure the API foundation and deliver working authentication endpoints verified through Swagger UI.

| File | What to do |
| ----- | ----- |
| `common/config/SecurityConfig.java` | `SecurityFilterChain` bean: permit `/auth/**` and Swagger paths, require auth on everything else; register the JWT filter; `@EnableMethodSecurity` for later `@PreAuthorize` use; configure CORS allowed origins. |
| `common/config/OpenApiConfig.java` | `OpenAPI` bean with title/version + a Bearer-token security scheme so Swagger UI has an "Authorize" button. |
| `common/config/RedisConfig.java` | `RedisConnectionFactory` (Lettuce) + `RedisTemplate<String,String>` beans — used for rate limiting and session state. |
| `common/exception/ApiError.java` | Fields: `timestamp, status, error, message, path`. Plain data class. |
| `common/exception/DomainException.java` | Abstract class extending `RuntimeException`, with an `errorCode` field every module's own exceptions will set. |
| `common/exception/GlobalExceptionHandler.java` | `@ControllerAdvice`; `@ExceptionHandler` methods for `DomainException` -> 400, `MethodArgumentNotValidException` -> 400 with field errors, generic `Exception` -> 500. Always return `ApiError`. |
| `common/base/BaseEntity.java` | `@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`: Base entity for metadata — `id` (`@Id @GeneratedValue UUID`), `createdAt` (`@CreatedDate`), `updatedAt` (`@LastModifiedDate`). **All module JPA entities MUST extend this class.** |
| `common/base/AudioContent.java` | `@Embeddable` reusable class: `audioUrl (String), transcript (Text), duration (Integer), cefrLevel (String)`. Shared by Listening, Dictation, & Speaking. |
| `common/base/VectorIndexable.java` | Interface defining `getVectorEntityId()`, `getEntityType()`, and `getEmbeddableText()`. Base contract for event-driven vector search indexing via `IEmbeddingIndexer`. |
| `common/prompt/LlmPromptTemplate.java` | Reusable JSON prompt template strategy builder encapsulating system prompt, template variables, and Jackson JSON output schema for `ILlmClient`. |
| `common/events/ContentCreatedEvent.java` | Spring `ApplicationEvent` carrying created entity metadata (`VectorIndexable entity`) for asynchronous vector search indexing. |
| `common/dtos/ScoreResult.java` | Standardized scoring output DTO (`boolean isCorrect`, `double score`, `String feedback`, `String normalizedAnswer`) for `IAnswerScorer`. |
| `common/ports/ILlmClient.java` | Shared Gemini LLM Port: `String generate(String prompt)`. |
| `common/ports/ITtsClient.java` | Shared Cartesia TTS Port: `AudioResult synthesize(String script, String voiceId)` — returns audio bytes + duration. |
| `common/ports/ISttClient.java` | Shared Deepgram STT Port: `String transcribe(byte[] audioBytes)`. |
| `common/ports/IStorageClient.java` | Shared S3 Storage Port: `String upload(String key, byte[] content, String contentType)`, `String presignedUrl(String key)`, `void delete(String key)`. |
| `common/ports/IAnswerScorer.java` | Generic scoring strategy port interface: `ScoreResult score(Question question, String userAnswer)` and `QuestionType getSupportedType()` — reusable by `TestAttempt` (mock exams) and `PracticeAttempt` (isolated Reading Drills, Dictation, Grammar Quizzes). |
| `common/ports/IEmbeddingIndexer.java` | Event-driven vector search indexing port interface: `void index(VectorIndexable entity)` for pgvector insertion. |
| `modules/auth/entities/RefreshToken.java` | Extends `BaseEntity`. Fields: `userId (UUID), tokenHash (String), expiresAt (Instant), revoked (boolean)`. |
| `modules/auth/repository/RefreshTokenRepository.java` | `extends JpaRepository<RefreshToken, UUID>` + `Optional<RefreshToken> findByTokenHash(String hash)`. |
| `modules/auth/dtos/request/SignupRequest.java` | `email` (`@Email @NotBlank`), `password` (`@NotBlank @Size(min=8)`), `name`. |
| `modules/auth/dtos/request/LoginRequest.java` | `email`, `password`. |
| `modules/auth/dtos/response/AuthTokenResponse.java` | `accessToken, refreshToken, expiresIn`. |
| `modules/auth/mapper/AuthMapper.java` | Static mapper helper converting Auth DTOs and token entities. |
| `modules/auth/ports/IAuthService.java` | `AuthTokenResponse signup(SignupRequest)`, `login(LoginRequest)`, `refresh(String refreshToken)`, `void logout(String refreshToken)`. |
| `modules/auth/services/impl/AuthServiceImpl.java` | Implements `IAuthService` using `AuthMapper`. `signup()`: check email uniqueness via `UserRepository`, hash password (`PasswordEncoder`), save `User`, issue tokens. `login()`: look up by email, verify password, issue tokens. `refresh()`: validate stored `RefreshToken`, rotate it, issue a new access token. `logout()`: mark token revoked. |
| `modules/auth/controllers/AuthController.java` | `POST /api/auth/signup`, `/login`, `/refresh`, `/logout` — each calls `IAuthService` and wraps the result in `ResponseEntity`. |
| `modules/user/entities/User.java` | Extends `BaseEntity`. Fields: `email (unique), passwordHash, name, role (UserRole), locale, avatarUrl`. |
| `modules/user/entities/UserRole.java` | Enum: `FREE, PAID, ADMIN`. |
| `modules/user/repository/UserRepository.java` | `extends JpaRepository<User, UUID>` + `Optional<User> findByEmail(String email)`. |

#### **Person B**

* **High-Level Task Overview:**
  * **Core Scope:** Set up local Docker containerization (`Dockerfile` and `docker-compose.yml` for PostgreSQL 17 & Redis 7). Build the `content` module to serve IELTS reading passages filterable by CEFR level (`GET /api/content/passages`) using `ReadingPassage` and `ContentMapper`. Seed initial reading passage test data (~10 real passages across levels).
  * **Key Goal:** Provide a working local Docker environment, and a browsable reading content API by level. `VectorIndexable`, vector search integration, and flashcard extraction will be wired in Week 2 once Person A has built the required infrastructure (`IEmbeddingIndexer`, `PgVectorEmbeddingIndexerImpl`).

| File | What to do |
| ----- | ----- |
| `Dockerfile` | Multi-stage Docker build file for Spring Boot Java 21 app (builder stage + slim JDK runtime stage). |
| `docker-compose.yml` | Multi-container compose configuration defining `postgres` (PostgreSQL 17 on port 5432) and `redis` (Redis 7 on port 6379) with healthchecks and persistent data volumes. |
| `modules/content/entities/ReadingPassage.java` | Extends `BaseEntity`. Fields: `title, body (Text), level (String, e.g., "B1"), topic, wordCount`. Leave `VectorIndexable` implementation for Week 2 — Person A's `IEmbeddingIndexer` does not exist yet. |
| `modules/content/repository/ReadingPassageRepository.java` | `extends JpaRepository<ReadingPassage, UUID>` + `List<ReadingPassage> findByLevel(String level)`. |
| `modules/content/dtos/response/ReadingPassageResponse.java` | `id, title, body, level, topic`. |
| `modules/content/mapper/ContentMapper.java` | Reusable mapper converting `ReadingPassage` entity to `ReadingPassageResponse` DTO (`toResponse(ReadingPassage entity)`). |
| `modules/content/ports/IContentService.java` | `List<ReadingPassageResponse> getPassages(String level)`, `ReadingPassageResponse getPassageById(UUID id)`. |
| `modules/content/services/impl/ContentServiceImpl.java` | Implements `IContentService` using `ContentMapper`. Throw `DomainException` (404) if `id` not found. No event publishing yet — that is added in Week 2 after Person A delivers `IEmbeddingIndexer`. |
| `modules/content/controllers/ContentController.java` | `GET /api/content/passages?level=`, `GET /api/content/passages/{id}`. No flashcard endpoint yet — `IFlashcardService` is not built until Week 2. |
| `(seed data)` | A `data.sql` or a small `CommandLineRunner` bean that inserts ~10 real reading passages across levels (A1, B1, B2, C1) as test data. |

**Deliverable:** Local Docker environment running Postgres & Redis; reading passages seeded and retrievable by level via Swagger. Vector search, flashcard extraction, and event publishing are wired in Week 2 after Person A delivers the required infrastructure.

---

### **Week 2 — AI Service Adapters (A) / Assessment Engine (B)**

> **Week 2 High-Level Overview:**  
> Integrate external AI/cloud adapters for Google Gemini, Cartesia TTS, Deepgram STT, S3 Storage, and pgvector vector search indexing (Person A), while constructing the core mock test assessment engine for Reading & Listening tests using generic scoring strategies and `PracticeAttempt` support (Person B).

#### **Person A**

* **High-Level Task Overview:**
  * **Core Scope:** Implement concrete external service adapters implementing shared ports: `GeminiLlmClientImpl` (`ILlmClient`) using `LlmPromptTemplate` for JSON formatting, `CartesiaTtsClientImpl` (`ITtsClient`) populating `AudioContent`, `DeepgramSttClientImpl` (`ISttClient`), `S3StorageClientImpl` (`IStorageClient`), and `PgVectorEmbeddingIndexerImpl` (`IEmbeddingIndexer`). Create `VectorIndexingEventListener` (`@EventListener @Async`) to process `ContentCreatedEvent`. Expand `user` module with `UserMapper`, profile updates, and avatar image uploads to S3.
  * **Key Goal:** Deliver operational external AI, storage, and vector indexing adapters, along with user profile management.

| File | What to do |
| ----- | ----- |
| `common/services/impl/GeminiLlmClientImpl.java` | Implements `ILlmClient` port interface — HTTP call to Google Gemini API. Formats prompt requests using `LlmPromptTemplate` for JSON output parsing across Writing and Speaking. |
| `common/services/impl/CartesiaTtsClientImpl.java` | Implements `ITtsClient` port interface — calls Cartesia Sonic 3.5 TTS API for ultra-low latency audio synthesis. Populates `AudioContent` embeddables reusable by Listening, Dictation, & Speaking. |
| `common/services/impl/DeepgramSttClientImpl.java` | Implements `ISttClient` port interface — calls Deepgram Nova-3 STT API, returns transcript string. Reusable by Speaking & Dictation audio processing. |
| `common/services/impl/S3StorageClientImpl.java` | Implements `IStorageClient` port interface — AWS SDK (or MinIO client) calls for upload/presign/delete. Stores audio assets for `AudioContent`. |
| `common/services/impl/PgVectorEmbeddingIndexerImpl.java` | Implements `IEmbeddingIndexer` port interface — generates vector embeddings for `VectorIndexable` entities via `ILlmClient` and writes to `pgvector` store (`content_embeddings`). |
| `common/listeners/VectorIndexingEventListener.java` | `@Component` listener with `@EventListener @Async` handling `ContentCreatedEvent`. Invokes `IEmbeddingIndexer.index(event.getEntity())` asynchronously to ensure non-blocking HTTP transactions. |
| `modules/user/dtos/request/UpdateProfileRequest.java`, `modules/user/dtos/response/UserResponse.java` | `UpdateProfileRequest`: `name, locale`. `UserResponse`: everything except `passwordHash`. |
| `modules/user/mapper/UserMapper.java` | Reusable mapper converting `User` entity to `UserResponse` DTO (`toResponse(User)`). |
| `modules/user/ports/IUserService.java`, `modules/user/ports/IPasswordResetService.java` | `IUserService`: `getProfile(userId)`, `updateProfile(userId, request)`, `uploadAvatar(userId, bytes)`. `IPasswordResetService`: `requestReset(email)`, `confirmReset(token, newPassword)`. |
| `modules/user/services/impl/UserServiceImpl.java` | Implements `IUserService` using `UserMapper`. `uploadAvatar()` calls `IStorageClient.upload()` and saves the returned URL onto the `User` entity. |
| `modules/user/services/impl/PasswordResetServiceImpl.java` | Implements `IPasswordResetService`; generates a signed/expiring token, stores or encodes it, sends via email. |
| `modules/user/controllers/UserController.java` | `GET /api/users/me`, `PATCH /api/users/me`, `POST /api/users/me/avatar`. |

#### **Person B**

* **High-Level Task Overview:**
  * **Core Scope:** Build the central `assessment` engine including full mock exams (`TestAttempt`) and lightweight isolated skill drills (`PracticeAttempt`), using `AssessmentMapper`. Implement concrete `IAnswerScorer` strategies (`McqScorerImpl`, `TfNgScorerImpl`, `FillBlankScorerImpl`) and `AnswerScorerRegistry` in `common/services/impl/`. In `AssessmentServiceImpl`, inject `AnswerScorerRegistry` to score both `TestAttempt` mock exams and `PracticeAttempt` drills using the exact same strategy beans, ensuring **zero code duplication**. Enforce server-side elapsed time validation.
  * **Key Goal:** Enable starting, answering, submitting, and scoring Reading mock tests and practice drills via Swagger under authentic exam time constraints, using reusable scoring strategies.

| File | What to do |
| ----- | ----- |
| `modules/assessment/entities/TestAttempt.java` | Extends `BaseEntity`. Fields: `userId, skill (READING/LISTENING), startedAt, submittedAt, overallScore`. Represents full mock examination sessions. |
| `modules/assessment/entities/PracticeAttempt.java` | Extends `BaseEntity`. Fields: `userId, moduleType (READING/LISTENING/GRAMMAR/DICTATION), topic, score, durationSeconds`. Lightweight entity for isolated skill drills (Reading drills, Dictation drills, Grammar quizzes). Reuses `IAnswerScorer`. |
| `modules/assessment/entities/TestAnswer.java` | Extends `BaseEntity`. Fields: `attemptId (FK, ManyToOne to TestAttempt/PracticeAttempt), questionId, userAnswer, isCorrect, score`. |
| `modules/assessment/repository/TestAttemptRepository.java`, `PracticeAttemptRepository.java`, `TestAnswerRepository.java` | Standard `JpaRepository` interfaces for mock exam attempts and isolated drill practice attempts. |
| `modules/assessment/dtos/request/SubmitAnswerRequest.java` | `questionId, userAnswer, attemptType (TEST/PRACTICE)`. |
| `modules/assessment/dtos/response/TestAttemptResponse.java`, `modules/assessment/dtos/response/ScoreResponse.java` | Attempt state; final score breakdown. |
| `modules/assessment/mapper/AssessmentMapper.java` | Static mapper helper converting `TestAttempt` -> `TestAttemptResponse`, `PracticeAttempt` -> `PracticeAttemptResponse`, and `TestAnswer` -> `ScoreResponse`. |
| `common/services/impl/McqScorerImpl.java` | Implements `IAnswerScorer` port interface — exact match against correct option. Reusable by `TestAttempt` (Mock Tests) & `PracticeAttempt` (Grammar/Reading Quizzes). |
| `common/services/impl/TfNgScorerImpl.java` | Implements `IAnswerScorer` port interface — exact match against True/False/Not-Given. Reusable by `TestAttempt` & `PracticeAttempt` Reading Drills. |
| `common/services/impl/FillBlankScorerImpl.java` | Implements `IAnswerScorer` port interface — case-insensitive, typo-tolerant match (Levenshtein distance <= 1). Reusable by `TestAttempt`, `PracticeAttempt` Dictation & Listening. |
| `common/services/impl/AnswerScorerRegistry.java` | Strategy registry bean injecting `Map<QuestionType, IAnswerScorer>` for dynamic strategy dispatch by question type. Shared kernel bean. |
| `modules/assessment/ports/IAssessmentService.java` | `startAttempt(userId, skill, attemptType)`, `submitAnswer(attemptId, request)`, `submitAttempt(attemptId)`. |
| `modules/assessment/services/impl/AssessmentServiceImpl.java` | 🔶 Implements `IAssessmentService` using `AssessmentMapper` and explicitly injecting `AnswerScorerRegistry` / `Map<QuestionType, IAnswerScorer>`. Scores both `TestAttempt` mock exam answers and `PracticeAttempt` skill drill answers using the same strategy beans, ensuring **zero code duplication**. Enforces server-side time limits. |
| `modules/assessment/controllers/AssessmentController.java` | `POST /api/assessment/attempts`, `POST /api/assessment/attempts/{id}/answers`, `POST /api/assessment/attempts/{id}/submit`. |
| `(seed data)` | Seed one full Reading test and 5 isolated practice drill questions against Week 1's reading passages. |

**Deliverable:** All AI and vector adapters (Gemini, Cartesia, Deepgram, S3, pgvector) are operational. Reading mock tests and practice drills can be started, answered, time-validated, and scored via Swagger using reusable scoring strategies.

---

### **Week 3 — Dictation & Security (A) / Writing & Speaking Audio (B)**

> **Week 3 High-Level Overview:**  
> Build the dictation exercise module and Redis rate limiting (Person A), while constructing decoupled AI writing evaluation and speaking audio recording pipelines (Person B).

#### **Person A**

* **High-Level Task Overview:**
  * **Core Scope:** Build the full `dictation` exercise module using `DictationMapper`. `DictationItem` entity embeds `@Embedded AudioContent audio` (`audioUrl, transcript, cefrLevel`) powered by `ITtsClient` (Cartesia) and `IStorageClient` (S3). Implement word-level Levenshtein diff scoring in `DictationScorerImpl` reusing `FillBlankScorerImpl` strategy from `IAnswerScorer`. Evaluate dictation submissions as a `PracticeAttempt`. Enable word selection into flashcards via `IFlashcardService.addWord()` with `SourceTag.DICTATION` and `word_definitions` cache table lookup. Implement Redis-based rate limiting (`RateLimitFilter`) on sensitive endpoints, and build unit test coverage.
  * **Key Goal:** Deliver operational dictation exercises, enforce rate limiting on AI/auth endpoints, and write core unit tests.

| File | What to do |
| ----- | ----- |
| `modules/dictation/entities/DictationItem.java` | Extends `BaseEntity`. Embeds `@Embedded AudioContent audio` (`audioUrl, transcript, cefrLevel`). Shared audio structure powered by `ITtsClient` and `IStorageClient`. Reusable for dictation `PracticeAttempt` drills. |
| `modules/dictation/repository/DictationItemRepository.java` | `extends JpaRepository<DictationItem, UUID>` + `List<DictationItem> findByLevel(String level)`. |
| `modules/dictation/dtos/request/SubmitDictationRequest.java`, `modules/dictation/dtos/response/DictationResultResponse.java` | Request: `userInput (String)`. Response: `correctWordCount, totalWordCount, diffHighlights (List)`. |
| `modules/dictation/mapper/DictationMapper.java` | Reusable mapper converting `DictationItem` entity to `DictationItemResponse` DTO and scoring results to `DictationResultResponse`. |
| `modules/dictation/ports/IDictationService.java`, `modules/dictation/ports/IDictationScorer.java` | `IDictationService`: `getItem(id)`, `submit(id, request)`. `IDictationScorer`: `ScoreResult score(String transcript, String userInput)`. |
| `modules/dictation/services/impl/DictationScorerImpl.java` | Implements `IDictationScorer` (reusing `FillBlankScorerImpl` strategy from `IAnswerScorer`) — word-level diff (Levenshtein distance) between `transcript` and `userInput`. |
| `modules/dictation/services/impl/DictationServiceImpl.java` | Implements `IDictationService` using `DictationMapper`. Synthesizes missing audio via `ITtsClient` (Cartesia) and stores via `IStorageClient` (S3). Evaluates submission as a `PracticeAttempt`. Enables direct word selection into flashcards via `IFlashcardService.addWord()` with `SourceTag.DICTATION` and `word_definitions` cache table lookup. |
| `modules/dictation/controllers/DictationController.java` | `GET /api/dictation/{id}`, `POST /api/dictation/{id}/submit`, `POST /api/dictation/{id}/flashcard` (adds target word using `SourceTag.DICTATION` & `word_definitions` cache). |
| `common/services/impl/RateLimitFilter.java` | Security filter applying Redis token bucket rate limiting on `/auth/**` and AI scoring endpoints. |
| `(unit tests)` | Write unit tests for `AuthServiceImpl`, `UserServiceImpl`, and `DictationScorerImpl`. |

#### **Person B**

* **High-Level Task Overview:**
  * **Core Scope:** Construct the `writing` module using `WritingMapper`. `EssaySubmission` extends `BaseEntity` and implements `VectorIndexable`. Define decoupled port interface `IEssayScorer` with dual adapters: `MockEssayScorerImpl` (`@Profile("dev")`) and `LlmEssayScorerImpl` (`@Profile("prod")` or `@Primary`) injecting `ILlmClient` and `LlmPromptTemplate` for structured JSON evaluation. `WritingCheckServiceImpl` saves submission, calls `IEssayScorer`, fires `ContentCreatedEvent` -> `IEmbeddingIndexer` for pgvector indexing, and enables vocabulary selection via `IFlashcardService.addWord()` with `SourceTag.SAMPLE_ESSAY` and `word_definitions` cache lookup. Support both `TestAttempt` essays and Task 1 `PracticeAttempt` drills. Construct the `speaking` module using `SpeakingMapper`. `SpeakingSession` embeds `@Embedded AudioContent audio`. Define decoupled port interface `ISpeakingScorer` with dual adapters: `MockSpeakingScorerImpl` (`@Profile("dev")`) and `LlmSpeakingScorerImpl` (`@Profile("prod")` or `@Primary`) using `ILlmClient` + `LlmPromptTemplate`. `SpeakingServiceImpl` uploads audio via `IStorageClient` (S3), transcribes via `ISttClient` (Deepgram Nova-3), populates `AudioContent`, evaluates via `ISpeakingScorer`, and enables vocabulary selection via `IFlashcardService.addWord()` with `SourceTag.SPEAKING` and `word_definitions` cache.
  * **Key Goal:** Allow users to submit written essays and speaking audio responses evaluated via decoupled AI ports, structured prompt templates, vector search indexing, and flashcard integration.

| File | What to do |
| ----- | ----- |
| `modules/writing/entities/EssaySubmission.java` | Extends `BaseEntity`, implements `VectorIndexable`. Fields: `userId, promptId, essayText (Text), wordCount, attemptType (TEST/PRACTICE), submittedAt`. Fires `ContentCreatedEvent` on save for vector indexing. |
| `modules/writing/entities/EssayFeedback.java` | Extends `BaseEntity`. Fields: `submissionId (FK, ManyToOne), criterion (String), score (Double), comments (Text/JSON)`. |
| `modules/writing/repository/EssaySubmissionRepository.java`, `modules/writing/repository/EssayFeedbackRepository.java` | Standard JpaRepository interfaces. |
| `modules/writing/dtos/request/SubmitEssayRequest.java`, `modules/writing/dtos/response/EssaySubmissionResponse.java` | DTO request/response schemas. |
| `modules/writing/mapper/WritingMapper.java` | Reusable mapper converting `EssaySubmission` and `EssayFeedback` entities to `EssaySubmissionResponse` DTOs. |
| `modules/writing/ports/IEssayScorer.java` | Interface port contract: `List<EssayFeedback> score(String essayText, String promptText)`. |
| `modules/writing/services/impl/MockEssayScorerImpl.java` | Implements `IEssayScorer`. Fast deterministic scoring mock based on word count/heuristics (`@Profile("dev")`). |
| `modules/writing/services/impl/LlmEssayScorerImpl.java` | Implements `IEssayScorer`. Production AI scoring using `ILlmClient` (Gemini) + `LlmPromptTemplate` with Jackson JSON schema parsing (`@Profile("prod")` or `@Primary`). |
| `modules/writing/ports/IWritingCheckService.java`, `modules/writing/services/impl/WritingCheckServiceImpl.java` | Implements `IWritingCheckService` using `WritingMapper`. Calculates word count, saves `EssaySubmission`, calls `IEssayScorer` interface port (decoupled from concrete scoring implementation), and publishes `ContentCreatedEvent` -> `IEmbeddingIndexer`. Enables vocabulary extraction via `IFlashcardService` with `SourceTag.SAMPLE_ESSAY` and `word_definitions` cache lookup. Handles both `TestAttempt` and `PracticeAttempt`. |
| `modules/writing/controllers/WritingController.java` | `POST /api/writing/submissions`, `GET /api/writing/submissions/{id}`, `POST /api/writing/submissions/{id}/flashcard` (extracts word using `SourceTag.SAMPLE_ESSAY` & `word_definitions` cache). |
| `modules/speaking/entities/SpeakingSession.java` | Extends `BaseEntity`. Embeds `@Embedded AudioContent audio` (`audioUrl, transcript, cefrLevel`). Reusable for full `TestAttempt` speaking tests and `PracticeAttempt` voice drills. |
| `modules/speaking/repository/SpeakingSessionRepository.java` | Standard `JpaRepository`. |
| `modules/speaking/dtos/response/SpeakingResultResponse.java` | `transcript, criteriaScores, overallBand`. |
| `modules/speaking/mapper/SpeakingMapper.java` | Reusable mapper converting `SpeakingSession` entity to `SpeakingResultResponse` DTO. |
| `modules/speaking/ports/ISpeakingScorer.java` | Interface port contract: `List<CriterionScore> score(String transcript)`. |
| `modules/speaking/services/impl/MockSpeakingScorerImpl.java` | Implements `ISpeakingScorer`. Fast deterministic mock scorer (`@Profile("dev")`). |
| `modules/speaking/services/impl/LlmSpeakingScorerImpl.java` | Implements `ISpeakingScorer`. Production AI scorer using `ILlmClient` + `LlmPromptTemplate` for structured JSON evaluation (`@Profile("prod")` or `@Primary`). |
| `modules/speaking/services/impl/SpeakingServiceImpl.java` | 🔶 Implements `ISpeakingService` using `SpeakingMapper`. Uploads audio via `IStorageClient` (S3), transcribes via `ISttClient` (Deepgram Nova-3), populates `@Embedded AudioContent`, and evaluates via `ISpeakingScorer` interface port. Enables vocabulary extraction via `IFlashcardService` with `SourceTag.SPEAKING` and `word_definitions` cache. Supports `TestAttempt` and `PracticeAttempt`. |
| `modules/speaking/controllers/SpeakingController.java` | `POST /api/speaking/sessions` (upload + transcribe), `GET /api/speaking/sessions/{id}`, `POST /api/speaking/sessions/{id}/flashcard` (extracts word with `SourceTag.SPEAKING` & `word_definitions` cache). |

**Deliverable:** Dictation working via Swagger with rate limiting. Writing essays scored via decoupled `IEssayScorer` (`LlmPromptTemplate` + Gemini), indexed into pgvector via `ContentCreatedEvent`. Speaking responses uploaded to S3, transcribed via Deepgram, and scored via decoupled `ISpeakingScorer`. Flashcard extraction enabled for both.

---

### **Week 4 — Listening Module & Hardening (A) / Flashcards & QA (B)**

> **Week 4 High-Level Overview:**  
> Build the complete Mock Listening module and perform backend security/production hardening (Person A), while completing the site-wide spaced-repetition vocabulary feature and conducting full regression testing (Person B).

#### **Person A**

* **High-Level Task Overview:**
  * **Core Scope:** Construct the complete `listening` module using `ListeningMapper`. `ListeningSection` entity embeds `@Embedded AudioContent audio` (`audioUrl, transcript, cefrLevel, topic, questionsJson`). `ListeningServiceImpl` calls `ITtsClient` (`CartesiaTtsClientImpl`) to synthesize audio bytes, uploads via `IStorageClient` (`S3StorageClientImpl`), and persists URL into `AudioContent`. Question scoring reuses `IAnswerScorer` strategies (`McqScorerImpl`, `FillBlankScorerImpl`) for both `TestAttempt` and `PracticeAttempt`. Enable flashcard extraction via `IFlashcardService` using `SourceTag.LISTENING` and `word_definitions` cache table lookup. Finalize `PgVectorEmbeddingIndexerImpl` event listener integration, environmentalize secrets in `application-prod.yml`, and set up production Docker Compose deployment targets (`docker-compose.prod.yml`).
  * **Key Goal:** Deliver the complete Listening test module, flashcard integration, and ensure a secure, prod-ready containerized backend.

| File | What to do |
| ----- | ----- |
| `modules/listening/entities/ListeningSection.java` | Extends `BaseEntity`. Embeds `@Embedded AudioContent audio` (`audioUrl, transcript, cefrLevel, topic, questionsJson`). Reusable by `TestAttempt` Mock Tests & `PracticeAttempt` Listening Drills. |
| `modules/listening/repository/ListeningSectionRepository.java` | Standard `JpaRepository` + `findByLevel`. |
| `modules/listening/dtos/response/ListeningSectionResponse.java` | `id, audioUrl, level, topic, questions`. |
| `modules/listening/mapper/ListeningMapper.java` | Reusable mapper converting `ListeningSection` entity to `ListeningSectionResponse` DTO (`toResponse`). |
| `modules/listening/ports/IListeningService.java` | `getSection(id)`, `generateSection(script, level)`. |
| `modules/listening/services/impl/ListeningServiceImpl.java` | 🔶 Implements `IListeningService` using `ListeningMapper`. `generateSection()` calls `ITtsClient` (`CartesiaTtsClientImpl`) for audio synthesis, uploads via `IStorageClient` (`S3StorageClientImpl`), saves `ListeningSection` (`AudioContent`). Integrates question scoring via `IAnswerScorer` strategies (`McqScorerImpl`, `FillBlankScorerImpl`) for both `TestAttempt` and `PracticeAttempt`. Enables flashcard extraction via `IFlashcardService` using `SourceTag.LISTENING` and `word_definitions` cache. |
| `modules/listening/controllers/ListeningController.java` | `GET /api/listening/{id}`, `POST /api/listening/generate`, `POST /api/listening/{id}/flashcard` (saves word with `SourceTag.LISTENING` & `word_definitions` cache). |
| `common/services/impl/PgVectorEmbeddingIndexerImpl.java` | Finalize Spring `@EventListener` wiring `ContentCreatedEvent` via `VectorIndexingEventListener` to `PgVectorEmbeddingIndexerImpl` for automated vector search indexing across all content entities. |
| `resources/application-prod.yml` | Environmentalize DB credentials, Redis URLs, and API keys (`${GEMINI_API_KEY}`, `${CARTESIA_API_KEY}`, `${DEEPGRAM_API_KEY}`). |
| `docker-compose.prod.yml` | Production Docker Compose target launching Spring Boot container linked to PostgreSQL and Redis services. |
| `(security hardening)` | Finalize CORS allowed origins, security headers, and rate limits across all endpoints. |

#### **Person B**

* **High-Level Task Overview:**
  * **Core Scope:** Build the site-wide `flashcard` module using `FlashcardMapper`. Create global cached entity `WordDefinition` (`word_definitions` table), `WordDefinitionRepository`, and universal `SourceTag` enum (`READING_PASSAGE, SAMPLE_ESSAY, DICTATION, SPEAKING, KNOWLEDGE_BASE, QUIZ, EXTERNAL, MANUAL`). Standardize `IFlashcardService.addWord(userId, word, sourceTag, sourceEntityId)`: normalize `word` to base lemma, query `WordDefinitionRepository` to reuse cached definitions, or invoke `ILlmClient` (Gemini API) once to generate definition + example sentence and persist to `word_definitions`. Build SM-2 spaced repetition algorithm in `Sm2SchedulerImpl`. Conduct full regression testing pass across all backend modules.
  * **Key Goal:** Deliver a central vocabulary tracker with global definition caching, SM-2 spaced repetition, and verify all module endpoints via Swagger.

| File | What to do |
| ----- | ----- |
| `modules/flashcard/entities/Flashcard.java` | Extends `BaseEntity`. Fields: `userId, word, lemma, definition, exampleSentence, sourceTag, masteryLevel`. |
| `modules/flashcard/entities/FlashcardReview.java` | Extends `BaseEntity`. Fields: `flashcard (FK, OneToOne), nextReviewAt, easeFactor, intervalDays, lastReviewedAt`. |
| `modules/flashcard/entities/SourceTag.java` | Enum: `READING_PASSAGE, SAMPLE_ESSAY, DICTATION, SPEAKING, KNOWLEDGE_BASE, QUIZ, EXTERNAL, MANUAL`. Universal tag enum supporting all MVP and post-MVP modules. |
| `modules/flashcard/entities/WordDefinition.java` | Extends `BaseEntity`. Global cached definition entity (`word` [unique constraint], `lemma`, `definition`, `exampleSentence`). Mapped to `word_definitions` table. |
| `modules/flashcard/repository/FlashcardRepository.java`, `FlashcardReviewRepository.java`, `WordDefinitionRepository.java` | `FlashcardRepository`: `Optional<Flashcard> findByUserIdAndLemma(userId, lemma)`. `FlashcardReviewRepository`: `List<FlashcardReview> findByFlashcard_UserIdAndNextReviewAtBefore(userId, now)`. `WordDefinitionRepository`: `Optional<WordDefinition> findByLemma(String lemma)`. |
| `modules/flashcard/dtos/request/AddFlashcardRequest.java`, `FlashcardResponse.java` | DTO request/response schemas. |
| `modules/flashcard/mapper/FlashcardMapper.java` | Reusable mapper converting `Flashcard` and `FlashcardReview` entities to `FlashcardResponse` DTOs. |
| `modules/flashcard/ports/ISpacedRepetitionScheduler.java` | `ReviewSchedule computeNext(FlashcardReview current, int recallQuality)`. |
| `modules/flashcard/services/impl/Sm2SchedulerImpl.java` | 🔶 Implements SM-2 algorithm: adjusts `easeFactor` based on `recallQuality` (0-5), computes next `intervalDays`, sets `nextReviewAt`. |
| `modules/flashcard/ports/IFlashcardService.java`, `modules/flashcard/services/impl/FlashcardServiceImpl.java` | Implements `IFlashcardService` using `FlashcardMapper`. Standardizes `addWord(userId, word, sourceTag, sourceEntityId)`: normalizes word to `lemma`, checks shared `WordDefinitionRepository` (`word_definitions` cache table) before executing `ILlmClient` (Gemini API) definition request, records provided `SourceTag` enum, and saves `Flashcard` + initial `FlashcardReview`. `review()` updates schedule using `Sm2SchedulerImpl`. |
| `modules/flashcard/controllers/FlashcardController.java` | `POST /api/flashcards`, `GET /api/flashcards`, `GET /api/flashcards/due`, `POST /api/flashcards/{id}/review`. |
| `(regression testing)` | Full regression pass across all modules via Swagger UI. |

**Both:**
* Full backend integration test end-to-end via Swagger: signup -> take Reading test -> generate & take Listening test -> submit Writing essay -> record Speaking response -> add flashcard -> review flashcard -> Dictation exercise.
* Fix bugs found; finalize OpenAPI docs; write a short backend runbook.

**Deliverable:** Fully working, fully tested backend for all 6 modules — reachable and demoable entirely through Swagger.

---

## **5. Notes**

* Every 🔶 item above shares the same shape: call a shared interface Person A built (`ILlmClient`, `ISttClient`, `ITtsClient`), and handle the response. None of them require building a new integration from scratch.

---

## **6. Appendix: Why We Need Database Migrations (Flyway)**

### **What is a Database Migration Tool?**
A database migration tool (like **Flyway**) is a version-control system for your relational database schema. Instead of executing manual SQL scripts or relying on JPA Hibernate auto-ddl (`spring.jpa.hibernate.ddl-auto=update`), Flyway executes versioned SQL files (e.g., `V1__init_users.sql`, `V2__init_content.sql`) sequentially against your database and keeps a ledger table (`flyway_schema_history`) tracking which scripts have been applied.

### **Why Do Production Systems Need Migrations?**
1. **Team Alignment & Sync:** When Developer A adds a new table or column, Developer B receives the SQL file via Git, and Flyway automatically updates Developer B's local database upon application startup.
2. **Automated CI/CD & Deployments:** Production databases cannot be reset or re-created. Flyway applies delta updates safely during deployment pipelines without human intervention.
3. **Audit Trail & Rollbacks:** Provides an exact audit log of who changed what database schema at what point in time.
4. **Data Integrity:** Avoids destructive JPA auto-ddl behavior in production environments that could accidentally drop columns or tables.

*During the 4-week MVP development phase, tables are created via Spring Data JPA auto-generation (`ddl-auto: update`) to speed up iteration. Flyway migrations will be introduced as the project transitions toward production staging.*
