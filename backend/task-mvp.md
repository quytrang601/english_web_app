# 🚀 IELTS Platform — 12-Week Deep Technical MVP Roadmap

> **Engineering Team:**  
> • **Person A (Fresher):** Platform Infrastructure, Auth/User, Assessment Engine (Exam Timer & Scoring), AI Writing Evaluation, and Frontend Exam/Writing UI.  
> • **Person B (Intern):** Docker Containerization, Reading Content Management, Flashcards (SM-2 Spaced Repetition), Dictation Engine, and Frontend Practice UI.  
>  
> **Cadence:** **Weeks 1–9:** Backend Modular Monolith (Spring Boot 3.4.4, Java 21 LTS, PostgreSQL 17, Redis 7).  
> **Weeks 10–12:** Frontend Client (Next.js 16, React 19, TypeScript, Tailwind CSS, shadcn/ui).  
>  
> **Core Principle:** **100% Vertical Feature Ownership.** Each person owns their features from database schema, repository, domain logic, and REST controllers to automated tests and frontend UI. There are **NO mixed tables** and **NO horizontal handoff bottlenecks**.

---

## 🏛️ Architectural Standards & Vertical Package Isolation

Every backend feature module lives in its own package under `com.ieltsplatform.modules.<feature_name>` and follows the strict 7-folder vertical layout:

```text
com.ieltsplatform/
├── common/                               # SHARED KERNEL (Maintained by Person A, consumed by both)
│   ├── base/                             # BaseEntity, AudioContent
│   ├── config/                           # SecurityConfig, OpenApiConfig, RedisConfig
│   ├── exception/                        # GlobalExceptionHandler, ApiError, DomainException
│   ├── ports/                            # Generic Strategy Ports (ILlmClient, ITtsClient, IStorageClient, IAnswerScorer)
│   └── services/impl/                    # Client Adapters & Strategy Registries
│
└── modules/
    ├── [Person A Packages]               # modules/auth, modules/user, modules/assessment, modules/writing
    └── [Person B Packages]               # modules/content, modules/flashcard, modules/dictation
```

### Strict Non-Overlapping Git Rules:
1. **Zero File Collisions:** Person A only touches `modules/auth/`, `modules/user/`, `modules/assessment/`, and `modules/writing/`. Person B only touches `modules/content/`, `modules/flashcard/`, and `modules/dictation/`.
2. **Shared Kernel Protocol:** Only Person A updates `common/`. If Person B requires a change to a shared interface or DTO, they align in standup, Person A pushes to `main`, and Person B pulls.
3. **Automated Testing Gate:** Every weekly task requires both unit tests for services/algorithms and `MockMvc` integration tests for controllers before a pull request can be merged.

---

# 📅 WEEK-BY-WEEK TECHNICAL SPECIFICATIONS

---

## 🟢 WEEK 1: Docker Environment, Base Kernel & Entity Setup

### 🧑‍💻 Person A (Fresher)
* **🎯 Weekly Goal:** Establish the shared backend kernel (`BaseEntity`, global error handling, open security configuration) and model the `User` identity domain.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`common/base/BaseEntity.java`**:
     - Class: `public abstract class BaseEntity` annotated with `@MappedSuperclass` and `@EntityListeners(AuditingEntityListener.class)`.
     - Fields:
       - `@Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "id", updatable = false, nullable = false) private UUID id;`
       - `@CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;`
       - `@LastModifiedDate @Column(name = "updated_at", nullable = false) private Instant updatedAt;`
       - `@Version @Column(name = "version", nullable = false) private Long version;`
     - Methods: Implement `equals(Object o)` and `hashCode()` strictly comparing non-null `id`.
  2. **`common/exception/ApiError.java` & `DomainException.java`**:
     - `ApiError`: Fields `Instant timestamp`, `int status`, `String error`, `String message`, `String path`, `Map<String, String> validationErrors`.
     - `DomainException`: Abstract `RuntimeException` with `HttpStatus status` and `String errorCode`.
     - Subclasses: `ResourceNotFoundException` (404), `DuplicateResourceException` (409), `BadRequestException` (400).
  3. **`common/exception/GlobalExceptionHandler.java`**:
     - `@RestControllerAdvice` class intercepting:
       - `DomainException` -> returns `ApiError` with specific status.
       - `MethodArgumentNotValidException` -> parses `fieldErrors` into `validationErrors` map (HTTP 400).
       - `Exception` -> logs error and returns generic `INTERNAL_SERVER_ERROR` (HTTP 500).
  4. **`common/config/SecurityConfig.java` (Open Phase)**:
     - `@Configuration @EnableWebSecurity` defining `SecurityFilterChain` bean:
     - Permissive rule for Weeks 1–2: `.authorizeHttpRequests(auth -> auth.requestMatchers("/api/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll().anyRequest().authenticated())`.
     - Enables CORS with allowed origins `http://localhost:3000` and disables CSRF for stateless REST.
  5. **`modules/user/entities/User.java` & `UserRole.java`**:
     - `User` extends `BaseEntity`. Annotate with `@Entity @Table(name = "users")`.
     - Fields: `email` (unique, nullable=false), `passwordHash` (nullable=false), `name` (nullable=false), `role` (`@Enumerated(EnumType.STRING)`), `targetBandScore` (Double), `avatarUrl` (String).
     - `UserRole` enum: `FREE`, `PAID`, `ADMIN`.
  6. **`modules/user/repository/UserRepository.java`**:
     - `public interface UserRepository extends JpaRepository<User, UUID>` with `Optional<User> findByEmail(String email)` and `boolean existsByEmail(String email)`.
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/common/GlobalExceptionHandlerTest.java`:
    - Test that throwing `ResourceNotFoundException` returns HTTP 404 with structured `ApiError` JSON.
    - Test that invalid request body returns HTTP 400 with field-specific validation error details.
  - Verification Command: `mvn test -Dtest=GlobalExceptionHandlerTest` (Must pass with 0 errors).

---

### 🧑‍💻 Person B (Intern)
* **🎯 Weekly Goal:** Set up the multi-container Docker environment (PostgreSQL 17 + Redis 7) and build the `ReadingPassage` entity and repository.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`docker-compose.yml`**:
     - Service `postgres`: Image `postgres:17-alpine`, container name `ielts-postgres`, environment `POSTGRES_DB=ielts_db`, `POSTGRES_USER=postgres`, `POSTGRES_PASSWORD=postgres`, ports `"5432:5432"`, healthcheck `pg_isready -U postgres`, persistent named volume `postgres_data:/var/lib/postgresql/data`.
     - Service `redis`: Image `redis:7-alpine`, container name `ielts-redis`, ports `"6379:6379"`, volume `redis_data:/data`.
  2. **`backend/src/main/resources/application.yml`**:
     - Connect Spring Boot to Docker Postgres:
       ```yaml
       spring:
         datasource:
           url: jdbc:postgresql://localhost:5432/ielts_db
           username: postgres
           password: postgres
           driver-class-name: org.postgresql.Driver
         jpa:
           hibernate:
             ddl-auto: update
           show-sql: true
           properties:
             hibernate.format_sql: true
       ```
  3. **`modules/content/entities/ReadingPassage.java`**:
     - Extends `BaseEntity`. Annotate with `@Entity @Table(name = "reading_passages")`.
     - Fields:
       - `title` (`@Column(nullable = false)`),
       - `body` (`@Column(columnDefinition = "TEXT", nullable = false)`),
       - `level` (`@Column(length = 10, nullable = false)` - e.g. "B1", "B2", "C1"),
       - `topic` (`@Column(length = 50, nullable = false)` - e.g. "Environment", "Technology", "History"),
       - `wordCount` (`@Column(nullable = false)`).
  4. **`modules/content/repository/ReadingPassageRepository.java`**:
     - `public interface ReadingPassageRepository extends JpaRepository<ReadingPassage, UUID>` with:
       - `List<ReadingPassage> findByLevel(String level);`
       - `List<ReadingPassage> findByTopic(String topic);`
       - `Page<ReadingPassage> findByLevelAndTopic(String level, String topic, Pageable pageable);`
* **✅ Expected Results & Automated Test Gate:**
  - Execute `docker compose up -d` — both containers status `healthy`.
  - Automated Tests in `src/test/java/com/ieltsplatform/modules/content/ReadingPassageRepositoryTest.java`:
    - Test saving a `ReadingPassage` and querying by `findByLevel("B2")`.
    - Verify inherited `BaseEntity` fields (`id`, `createdAt`, `version`) are automatically populated.
  - Verification Command: `mvn test -Dtest=ReadingPassageRepositoryTest` (Must pass with 0 errors).

---

## 🟢 WEEK 2: Authentication Service & Reading Passage Content Service

### 🧑‍💻 Person A (Fresher)
* **🎯 Weekly Goal:** Implement the full Authentication engine (Signup, Password Hashing, Login, JWT Token generation, and Token Refresh).
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`modules/auth/entities/RefreshToken.java` & `RefreshTokenRepository.java`**:
     - Extends `BaseEntity`. `@Entity @Table(name = "refresh_tokens")`.
     - Fields: `userId` (UUID, nullable=false), `tokenHash` (String, unique=true), `expiresAt` (Instant, nullable=false), `revoked` (boolean, default=false).
     - Repo: `Optional<RefreshToken> findByTokenHash(String hash);` and `void deleteByUserId(UUID userId);`.
  2. **`modules/auth/dtos/`**:
     - `SignupRequest`: `email` (`@Email @NotBlank`), `password` (`@NotBlank @Size(min = 8)`), `name` (`@NotBlank`).
     - `LoginRequest`: `email` (`@Email @NotBlank`), `password` (`@NotBlank`).
     - `RefreshTokenRequest`: `refreshToken` (`@NotBlank`).
     - `AuthTokenResponse`: `String accessToken`, `String refreshToken`, `long expiresIn`, `String tokenType`.
  3. **`modules/auth/services/JwtTokenProvider.java`**:
     - Uses `io.jsonwebtoken` (jjwt): Generates HMAC-SHA256 signed access tokens (15-min expiration) containing `userId` and `roles`. Generates cryptographically secure random refresh tokens (7-day expiration).
  4. **`modules/auth/ports/IAuthService.java` & `AuthServiceImpl.java`**:
     - `AuthTokenResponse signup(SignupRequest req)`: Checks `userRepository.existsByEmail(req.getEmail())` (throws `DuplicateResourceException` if taken); hashes password via `BCryptPasswordEncoder`; saves `User`; issues access + refresh tokens.
     - `AuthTokenResponse login(LoginRequest req)`: Finds user by email; validates `passwordEncoder.matches()`; revokes old refresh tokens; saves new `RefreshToken`; returns `AuthTokenResponse`.
     - `AuthTokenResponse refresh(RefreshTokenRequest req)`: Hashes provided token; finds valid non-revoked non-expired `RefreshToken`; rotates refresh token; issues new access token.
  5. **`modules/auth/controllers/AuthController.java`**:
     - Endpoints: `POST /api/auth/signup`, `POST /api/auth/login`, `POST /api/auth/refresh`. Returns HTTP 200/201 with `AuthTokenResponse`.
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/modules/auth/AuthServiceTest.java` (Unit tests with Mockito):
    - `signup_WhenEmailExists_ThrowsDuplicateException()`
    - `login_WithValidCredentials_ReturnsTokens()`
    - `login_WithInvalidPassword_ThrowsBadCredentials()`
  - Integration Tests in `src/test/java/com/ieltsplatform/modules/auth/AuthControllerTest.java`:
    - `MockMvc` testing `POST /api/auth/signup` -> HTTP 201 with `accessToken`.
  - Verification Command: `mvn test -Dtest=AuthServiceTest,AuthControllerTest` (Must pass with 0 errors).

---

### 🧑‍💻 Person B (Intern)
* **🎯 Weekly Goal:** Implement the complete Reading Passage Content Service with CEFR level/topic filtering and pagination.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`modules/content/dtos/` & `ContentMapper.java`**:
     - `CreatePassageRequest`: `title` (`@NotBlank`), `body` (`@NotBlank`), `level` (`@Pattern(regexp = "^(A1|A2|B1|B2|C1|C2)$")`), `topic` (`@NotBlank`).
     - `ReadingPassageResponse`: `UUID id`, `String title`, `String body`, `String level`, `String topic`, `int wordCount`, `Instant createdAt`.
     - `ContentMapper`: Static utility converting `ReadingPassage` to `ReadingPassageResponse`. Automatically computes `wordCount = body.trim().split("\\s+").length`.
  2. **`modules/content/ports/IContentService.java`**:
     - `Page<ReadingPassageResponse> getPassages(String level, String topic, Pageable pageable);`
     - `ReadingPassageResponse getPassageById(UUID id);`
     - `ReadingPassageResponse createPassage(CreatePassageRequest request);`
  3. **`modules/content/services/impl/ContentServiceImpl.java`**:
     - `getPassages`: Dynamically queries `ReadingPassageRepository` based on whether `level` and `topic` filters are provided.
     - `getPassageById`: Queries by ID; throws `ResourceNotFoundException("Passage not found with ID: " + id)` if missing.
     - `createPassage`: Calculates word count, maps to entity, saves, and returns response.
  4. **`modules/content/controllers/ContentController.java`**:
     - Endpoints:
       - `GET /api/content/passages?level=B2&topic=Technology&page=0&size=10` -> HTTP 200 with paginated passage summaries.
       - `GET /api/content/passages/{id}` -> HTTP 200 with full passage body.
       - `POST /api/content/passages` -> HTTP 201 with created passage.
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/modules/content/ContentServiceTest.java`:
    - Test `getPassageById_NotFound_Throws404()`.
    - Test `createPassage_ComputesWordCountAccurately()`.
  - Controller Tests in `src/test/java/com/ieltsplatform/modules/content/ContentControllerTest.java`:
    - `MockMvc` verifying `GET /api/content/passages` returns status 200 and JSON array.
  - Verification Command: `mvn test -Dtest=ContentServiceTest,ContentControllerTest` (Must pass with 0 errors).

---

## 🟢 WEEK 3: User Profile Security & Reading Question Modeling

### 🧑‍💻 Person A (Fresher)
* **🎯 Weekly Goal:** Activate Spring Security JWT filter, secure user profile endpoints (`GET /api/users/me`, `PATCH /api/users/me`), and enforce RBAC.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`common/security/JwtAuthFilter.java`**:
     - Extends `OncePerRequestFilter`. Extracts header `Authorization: Bearer <token>`.
     - Validates token via `JwtTokenProvider`. Extracts `userId` and `roles`.
     - Builds `UsernamePasswordAuthenticationToken` with principal `UserPrincipal(userId, email, roles)` and sets into `SecurityContextHolder`.
  2. **`common/config/SecurityConfig.java` (Hardened)**:
     - Register `JwtAuthFilter` before `UsernamePasswordAuthenticationFilter`.
     - Rules:
       - Permit: `/api/auth/**`, `GET /api/content/**`, `/swagger-ui/**`, `/v3/api-docs/**`.
       - Require Authentication: `/api/users/**`, `/api/assessment/**`, `/api/writing/**`, `/api/flashcards/**`, `/api/dictation/**`.
  3. **`modules/user/dtos/` & `UserMapper.java`**:
     - `UpdateProfileRequest`: `String name`, `Double targetBandScore`, `String avatarUrl`.
     - `UserProfileResponse`: `UUID id`, `String email`, `String name`, `UserRole role`, `Double targetBandScore`, `String avatarUrl`, `Instant createdAt`.
  4. **`modules/user/ports/IUserService.java` & `UserServiceImpl.java`**:
     - `UserProfileResponse getProfile(UUID userId)`: Fetches user; throws 404 if missing; returns DTO (never exposes password hash).
     - `UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest req)`: Updates mutable fields; saves and returns updated DTO.
  5. **`modules/user/controllers/UserController.java`**:
     - `@RestController @RequestMapping("/api/users")`:
       - `GET /api/users/me`: Reads authenticated `userId` from security context; returns `UserProfileResponse`.
       - `PATCH /api/users/me`: Validates request; updates and returns `UserProfileResponse`.
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/modules/user/UserControllerTest.java`:
    - Test `GET /api/users/me` without Bearer token -> HTTP 403 / 401 Forbidden.
    - Test `GET /api/users/me` with valid mock JWT -> HTTP 200 with user profile JSON.
  - Verification Command: `mvn test -Dtest=UserControllerTest` (Must pass with 0 errors).

---

### 🧑‍💻 Person B (Intern)
* **🎯 Weekly Goal:** Model passage questions (MCQ, True/False/Not-Given), link them to reading passages, and create the initial IELTS data seeder.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`modules/content/entities/QuestionType.java` & `PassageQuestion.java`**:
     - `QuestionType` enum: `MULTIPLE_CHOICE`, `TRUE_FALSE_NOT_GIVEN`.
     - `PassageQuestion` extends `BaseEntity`. `@Entity @Table(name = "passage_questions")`.
     - Fields:
       - `passage` (`@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "passage_id", nullable = false) ReadingPassage passage;`),
       - `questionNumber` (`@Column(nullable = false)`),
       - `type` (`@Enumerated(EnumType.STRING) @Column(nullable = false)`),
       - `prompt` (`@Column(columnDefinition = "TEXT", nullable = false)`),
       - `optionsJson` (`@Column(columnDefinition = "TEXT")` - for MCQ options e.g. `{"A":"...", "B":"..."}`),
       - `correctAnswer` (`@Column(nullable = false)` - e.g. "A", "TRUE", "NOT_GIVEN"),
       - `explanation` (`@Column(columnDefinition = "TEXT")`).
  2. **`modules/content/repository/PassageQuestionRepository.java`**:
     - `List<PassageQuestion> findByPassageIdOrderByQuestionNumberAsc(UUID passageId);`
  3. **`modules/content/dtos/`**:
     - `PassageQuestionResponse`: `UUID id`, `int questionNumber`, `QuestionType type`, `String prompt`, `Map<String, String> options`, `String explanation` (omits `correctAnswer` during active test mode).
  4. **`modules/content/seed/ContentDataSeeder.java`**:
     - `@Component @Profile("dev")` implementing `CommandLineRunner`:
     - Checks `if (readingPassageRepository.count() == 0)`.
     - Inserts 5 realistic IELTS Reading Passages (B1, B2, C1) with 8–10 questions each (MCQ and TFNG).
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/modules/content/PassageQuestionRepositoryTest.java`:
    - Save passage with 3 questions; verify cascading fetch and order by `questionNumber`.
  - Test `GET /api/content/passages/{id}/questions` returns question list.
  - Verification Command: `mvn test -Dtest=PassageQuestionRepositoryTest` (Must pass with 0 errors).

---

## 🟡 WEEK 4: Scoring Strategy Engine & Flashcard Domain Foundation

### 🧑‍💻 Person A (Fresher)
* **🎯 Weekly Goal:** Implement the generic `IAnswerScorer` strategy engine and build shared cloud port contracts with deterministic local mocks (`ILlmClient`, `ITtsClient`, `IStorageClient`).
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`common/dtos/ScoreResult.java` & `common/ports/IAnswerScorer.java`**:
     - `ScoreResult`: `boolean isCorrect`, `double score` (1.0 or 0.0), `String feedback`, `String normalizedAnswer`.
     - `IAnswerScorer` interface:
       - `ScoreResult score(PassageQuestion question, String userAnswer);`
       - `QuestionType getSupportedType();`
  2. **`common/services/impl/McqScorerImpl.java`**:
     - Implements `IAnswerScorer` for `MULTIPLE_CHOICE`.
     - Normalizes: `userAnswer.trim().toUpperCase()`.
     - Checks if `normalized.equals(question.getCorrectAnswer().trim().toUpperCase())`.
  3. **`common/services/impl/TfNgScorerImpl.java`**:
     - Implements `IAnswerScorer` for `TRUE_FALSE_NOT_GIVEN`.
     - Normalizes: maps "T" -> "TRUE", "F" -> "FALSE", "NG" -> "NOT_GIVEN".
     - Compares normalized token against `question.getCorrectAnswer()`.
  4. **`common/services/impl/AnswerScorerRegistry.java`**:
     - `@Component`: Injects `List<IAnswerScorer> scorers`. Populates `Map<QuestionType, IAnswerScorer>`.
     - Method `IAnswerScorer getScorer(QuestionType type)`: Returns corresponding scorer or throws `UnsupportedOperationException`.
  5. **`common/ports/ILlmClient.java` & `MockLlmClientImpl.java`**:
     - Interface: `String generate(String prompt);`
     - `MockLlmClientImpl` (`@Service @Profile("dev")`): Returns deterministic JSON outputs for essay evaluations and dictionary word definitions without making internet calls.
  6. **`common/ports/ITtsClient.java` & `IStorageClient.java`**:
     - Define `ITtsClient` (`byte[] synthesize(String text)`) and `IStorageClient` (`String upload(String key, byte[] bytes)`). Provide mock implementations for local testing.
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/common/AnswerScorerTest.java`:
    - Test `McqScorerImpl` with correct, wrong, and whitespace-padded inputs.
    - Test `TfNgScorerImpl` with abbreviations ("T", "NG") and casing.
    - Test `AnswerScorerRegistry` correctly resolves strategies by `QuestionType`.
  - Verification Command: `mvn test -Dtest=AnswerScorerTest` (Must pass with 0 errors).

---

### 🧑‍💻 Person B (Intern)
* **🎯 Weekly Goal:** Build the Flashcard domain entities, repository, and global `WordDefinition` caching entity.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`modules/flashcard/entities/WordDefinition.java` & `WordDefinitionRepository.java`**:
     - Extends `BaseEntity`. `@Entity @Table(name = "word_definitions")`.
     - Fields:
       - `word` (`@Column(unique = true, nullable = false)`),
       - `lemma` (`@Column(nullable = false)`),
       - `partOfSpeech` (String),
       - `definition` (`@Column(columnDefinition = "TEXT", nullable = false)`),
       - `exampleSentence` (`@Column(columnDefinition = "TEXT")`).
     - Repo: `Optional<WordDefinition> findByLemma(String lemma);` and `Optional<WordDefinition> findByWordIgnoreCase(String word);`.
  2. **`modules/flashcard/entities/SourceTag.java` & `Flashcard.java`**:
     - `SourceTag` enum: `READING_PASSAGE`, `DICTATION`, `MANUAL`.
     - `Flashcard` extends `BaseEntity`. `@Entity @Table(name = "flashcards")`.
     - Fields:
       - `userId` (`@Column(nullable = false)`),
       - `word` (`@Column(nullable = false)`),
       - `lemma` (`@Column(nullable = false)`),
       - `definition` (`@Column(columnDefinition = "TEXT", nullable = false)`),
       - `exampleSentence` (`@Column(columnDefinition = "TEXT")`),
       - `sourceTag` (`@Enumerated(EnumType.STRING)`),
       - `sourceEntityId` (UUID - optional link to passage or dictation ID),
       - `masteryLevel` (int, default 0).
  3. **`modules/flashcard/repository/FlashcardRepository.java`**:
     - `Optional<Flashcard> findByUserIdAndLemma(UUID userId, String lemma);`
     - `Page<Flashcard> findByUserId(UUID userId, Pageable pageable);`
     - `long countByUserId(UUID userId);`
  4. **`modules/flashcard/dtos/` & `FlashcardMapper.java`**:
     - `AddFlashcardRequest`: `String word` (`@NotBlank`), `SourceTag sourceTag`, `UUID sourceEntityId`.
     - `FlashcardResponse`: `UUID id`, `String word`, `String lemma`, `String definition`, `String exampleSentence`, `SourceTag sourceTag`, `int masteryLevel`, `Instant createdAt`.
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/modules/flashcard/FlashcardRepositoryTest.java`:
    - Test saving a `WordDefinition` and verifying unique constraint on `word`.
    - Test saving a `Flashcard` and querying by `findByUserIdAndLemma()`.
  - Verification Command: `mvn test -Dtest=FlashcardRepositoryTest` (Must pass with 0 errors).

---

## 🟡 WEEK 5: Mock Exam Assessment Lifecycle & SM-2 Algorithm

### 🧑‍💻 Person A (Fresher)
* **🎯 Weekly Goal:** Implement the IELTS Mock Exam attempt lifecycle (`TestAttempt`), answer saving, and server-side timer enforcement.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`modules/assessment/entities/AttemptStatus.java`, `TestAttempt.java`, `TestAnswer.java`**:
     - `AttemptStatus` enum: `IN_PROGRESS`, `COMPLETED`, `EXPIRED`.
     - `TestAttempt` extends `BaseEntity`. `@Entity @Table(name = "test_attempts")`:
       - `userId` (UUID, nullable=false),
       - `passageId` (UUID, nullable=false),
       - `startedAt` (Instant, nullable=false),
       - `submittedAt` (Instant),
       - `timeLimitMinutes` (int, default 20),
       - `bandScore` (Double),
       - `status` (`@Enumerated(EnumType.STRING)`).
     - `TestAnswer` extends `BaseEntity`. `@Entity @Table(name = "test_answers")`:
       - `attempt` (`@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "attempt_id") TestAttempt attempt;`),
       - `questionId` (UUID, nullable=false),
       - `userAnswer` (String),
       - `isCorrect` (Boolean),
       - `score` (Double).
  2. **`modules/assessment/repository/TestAttemptRepository.java` & `TestAnswerRepository.java`**:
     - `Optional<TestAttempt> findByIdAndUserId(UUID id, UUID userId);`
     - `List<TestAnswer> findByAttemptId(UUID attemptId);`
     - `Optional<TestAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);`
  3. **`modules/assessment/ports/IAssessmentService.java` & `AssessmentServiceImpl.java` (Part 1)**:
     - `TestAttemptResponse startAttempt(UUID userId, UUID passageId)`:
       - Verifies passage exists. Creates `TestAttempt` with `startedAt = Instant.now()`, `status = IN_PROGRESS`.
     - `void saveAnswer(UUID userId, UUID attemptId, UUID questionId, String answer)`:
       - Validates attempt belongs to `userId` and `status == IN_PROGRESS`.
       - Enforces server-side timer: `Duration.between(attempt.getStartedAt(), Instant.now()).toMinutes() <= attempt.getTimeLimitMinutes()`. If expired, marks `status = EXPIRED` and throws `BadRequestException("Exam time limit exceeded")`.
       - Upserts `TestAnswer`.
  4. **`modules/assessment/controllers/AssessmentController.java`**:
     - `POST /api/assessment/attempts` (Body: `{ "passageId": "..." }`) -> HTTP 201.
     - `PUT /api/assessment/attempts/{id}/answers` (Body: `{ "questionId": "...", "answer": "B" }`) -> HTTP 200.
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/modules/assessment/AssessmentServiceAttemptTest.java`:
    - Test `startAttempt()` creates session with active timer.
    - Test `saveAnswer()` rejects submissions when elapsed time > time limit (HTTP 400).
    - Test upserting answers updates existing answer instead of duplicating rows.
  - Verification Command: `mvn test -Dtest=AssessmentServiceAttemptTest` (Must pass with 0 errors).

---

### 🧑‍💻 Person B (Intern)
* **🎯 Weekly Goal:** Implement the mathematical SuperMemo SM-2 Spaced Repetition scheduler algorithm with full unit test coverage.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`modules/flashcard/entities/FlashcardReview.java` & `FlashcardReviewRepository.java`**:
     - Extends `BaseEntity`. `@Entity @Table(name = "flashcard_reviews")`.
     - Fields:
       - `flashcard` (`@OneToOne @JoinColumn(name = "flashcard_id", unique = true) Flashcard flashcard;`),
       - `nextReviewAt` (Instant, nullable=false),
       - `intervalDays` (int, default 1),
       - `easeFactor` (double, default 2.5),
       - `repetitions` (int, default 0),
       - `lastReviewedAt` (Instant).
     - Repo: `List<FlashcardReview> findByFlashcard_UserIdAndNextReviewAtBefore(UUID userId, Instant date);`.
  2. **`modules/flashcard/dtos/ReviewSchedule.java`**:
     - Record `ReviewSchedule(int nextIntervalDays, double nextEaseFactor, int nextRepetitions, Instant nextReviewAt)`.
  3. **`modules/flashcard/ports/ISpacedRepetitionScheduler.java` & `Sm2SchedulerImpl.java`**:
     - Pure mathematical SM-2 implementation:
       ```java
       public ReviewSchedule computeNext(FlashcardReview current, int quality) {
           // quality: 0 to 5
           if (quality < 3) {
               // Failed recall: reset repetitions to 0, review tomorrow
               return new ReviewSchedule(1, current.getEaseFactor(), 0, Instant.now().plus(1, ChronoUnit.DAYS));
           }
           int reps = current.getRepetitions() + 1;
           int interval;
           if (reps == 1) interval = 1;
           else if (reps == 2) interval = 6;
           else interval = (int) Math.round(current.getIntervalDays() * current.getEaseFactor());

           double newEase = current.getEaseFactor() + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
           if (newEase < 1.3) newEase = 1.3; // SM-2 absolute minimum ease factor

           return new ReviewSchedule(interval, newEase, reps, Instant.now().plus(interval, ChronoUnit.DAYS));
       }
       ```
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/modules/flashcard/Sm2SchedulerTest.java` (Pure JUnit 5):
    - Test recall quality = 5 increases ease factor and scales interval (1 -> 6 -> 15 days).
    - Test recall quality < 3 resets repetitions to 0 and interval to 1 day.
    - Test ease factor never drops below 1.3 even after consecutive failures.
  - Verification Command: `mvn test -Dtest=Sm2SchedulerTest` (Must pass with 0 errors).

---

## 🟡 WEEK 6: Assessment Automated Grading & Flashcard Review Loop

### 🧑‍💻 Person A (Fresher)
* **🎯 Weekly Goal:** Implement exam completion, automated grading via `AnswerScorerRegistry`, and conversion to official IELTS Band Scores (0.0–9.0).
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`modules/assessment/services/impl/IeltsBandScoreConverter.java`**:
     - Utility converting raw Reading score (out of 40 or percentage) to IELTS 9-band scale:
       - >= 88% -> Band 8.5–9.0
       - >= 75% -> Band 7.5–8.0
       - >= 62% -> Band 6.5–7.0
       - >= 50% -> Band 5.5–6.0
       - Below 50% -> Band 4.0–5.0
  2. **`modules/assessment/services/impl/AssessmentServiceImpl.java` (Part 2 - Grading)**:
     - `AssessmentResultResponse submitAttempt(UUID userId, UUID attemptId)`:
       - Validates attempt; loads all `PassageQuestion` records for the passage.
       - Iterates questions: retrieves user's `TestAnswer`; fetches appropriate `IAnswerScorer` from `AnswerScorerRegistry` by `question.getType()`; computes `ScoreResult`.
       - Updates `TestAnswer`: `isCorrect = scoreResult.isCorrect()`, `score = scoreResult.getScore()`.
       - Computes total correct count, calculates IELTS Band Score via `IeltsBandScoreConverter`.
       - Marks `attempt.setStatus(AttemptStatus.COMPLETED)` and `attempt.setSubmittedAt(Instant.now())`.
  3. **`modules/assessment/dtos/`**:
     - `AssessmentResultResponse`: `UUID attemptId`, `double bandScore`, `int correctCount`, `int totalQuestions`, `int timeSpentSeconds`, `List<QuestionFeedbackDto> questions`.
  4. **`modules/assessment/controllers/AssessmentController.java` (Finalization)**:
     - `POST /api/assessment/attempts/{id}/submit`: Triggers grading; returns `AssessmentResultResponse`.
     - `GET /api/assessment/attempts/{id}/result`: Retrieves result breakdown for review.
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/modules/assessment/AssessmentGradingTest.java`:
    - Full end-to-end exam grading test with MockMvc.
    - Test scoring a mix of MCQ and TFNG answers -> accurately computes Band Score.
  - Verification Command: `mvn test -Dtest=AssessmentGradingTest` (Must pass with 0 errors).

---

### 🧑‍💻 Person B (Intern)
* **🎯 Weekly Goal:** Implement the complete Flashcard service (add word with lemma normalization, definition caching, and daily review workflow).
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`modules/flashcard/ports/IFlashcardService.java`**:
     - `FlashcardResponse addWord(UUID userId, AddFlashcardRequest request);`
     - `List<FlashcardReviewResponse> getDueReviews(UUID userId);`
     - `FlashcardResponse reviewCard(UUID userId, UUID cardId, int qualityScore);`
  2. **`modules/flashcard/services/impl/FlashcardServiceImpl.java`**:
     - `addWord()`:
       - Normalizes word (lowercases, trims, basic lemmatization).
       - Checks `flashcardRepository.findByUserIdAndLemma(userId, lemma)` (throws `DuplicateResourceException` if card already in user deck).
       - Checks `wordDefinitionRepository.findByLemma(lemma)`:
         - If found: reuses definition & example sentence.
         - If missing: calls Person A's `ILlmClient` mock to generate definition; saves to `WordDefinition` table.
       - Saves `Flashcard` + initial `FlashcardReview` (`nextReviewAt = now`).
     - `getDueReviews()`: Queries `flashcardReviewRepository.findByFlashcard_UserIdAndNextReviewAtBefore(userId, Instant.now())`.
     - `reviewCard()`: Injects `ISpacedRepetitionScheduler`; computes next schedule; updates `FlashcardReview`; increments `masteryLevel` if quality >= 4.
  3. **`modules/flashcard/controllers/FlashcardController.java`**:
     - `POST /api/flashcards`: Add new word.
     - `GET /api/flashcards/due`: List cards due for review today.
     - `POST /api/flashcards/{id}/review` (Body: `{ "quality": 4 }`): Submit review score.
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/modules/flashcard/FlashcardServiceTest.java`:
    - Test duplicate word prevention per user.
    - Test dictionary cache reuse (verifies LLM client is NOT called if word is already in `word_definitions`).
    - Test `reviewCard()` correctly updates `nextReviewAt`.
  - Verification Command: `mvn test -Dtest=FlashcardServiceTest` (Must pass with 0 errors).

---

## 🟠 PHASE 3: AI Writing Evaluation, Dictation Practice & Hardening (Weeks 7–9)

## 🟠 WEEK 7: AI Writing Pipeline Architecture & Dictation Audio Model

### 🧑‍💻 Person A (Fresher)
* **🎯 Weekly Goal:** Build the decoupled AI writing submission architecture, entities, structured prompt template, and local scoring mock.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`modules/writing/entities/EssaySubmission.java` & `EssayFeedback.java`**:
     - `EssaySubmission` extends `BaseEntity`. `@Entity @Table(name = "essay_submissions")`:
       - `userId` (UUID, nullable=false),
       - `taskType` (`TASK_1` or `TASK_2`),
       - `prompt` (`@Column(columnDefinition = "TEXT", nullable = false)`),
       - `essayText` (`@Column(columnDefinition = "TEXT", nullable = false)`),
       - `wordCount` (int, nullable=false),
       - `overallBand` (Double),
       - `submittedAt` (Instant).
     - `EssayFeedback` extends `BaseEntity`. `@Entity @Table(name = "essay_feedbacks")`:
       - `submission` (`@ManyToOne @JoinColumn(name = "submission_id") EssaySubmission submission;`),
       - `criterion` (`TASK_ACHIEVEMENT`, `COHERENCE_COHESION`, `LEXICAL_RESOURCE`, `GRAMMATICAL_ACCURACY`),
       - `score` (Double),
       - `feedback` (`@Column(columnDefinition = "TEXT")`),
       - `suggestionsJson` (`@Column(columnDefinition = "TEXT")`).
  2. **`modules/writing/ports/IEssayScorer.java` & `MockEssayScorerImpl.java`**:
     - `IEssayScorer` interface: `EssayScoringResult score(String prompt, String essay, TaskType taskType);`.
     - `MockEssayScorerImpl` (`@Service @Profile("dev")`): Calculates rubric scores based on word count heuristics (e.g. >= 250 words -> base 6.5) and returns mock sentence-level suggestions without hitting external APIs.
  3. **`modules/writing/prompt/LlmPromptTemplate.java`**:
     - Encapsulates IELTS official rubric guidelines and enforces strict JSON response schema:
       ```json
       {
         "overallBand": 7.0,
         "criteria": [
           { "criterion": "TASK_ACHIEVEMENT", "score": 7.0, "feedback": "..." },
           { "criterion": "COHERENCE_COHESION", "score": 6.5, "feedback": "..." },
           { "criterion": "LEXICAL_RESOURCE", "score": 7.0, "feedback": "..." },
           { "criterion": "GRAMMATICAL_ACCURACY", "score": 7.5, "feedback": "..." }
         ],
         "inlineSuggestions": [{ "originalSentence": "...", "improvedSentence": "...", "explanation": "..." }]
       }
       ```
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/modules/writing/MockEssayScorerTest.java`:
    - Test that `MockEssayScorerImpl` returns complete evaluation for Task 1 and Task 2.
    - Test Jackson parsing of `LlmPromptTemplate` JSON schema.
  - Verification Command: `mvn test -Dtest=MockEssayScorerTest` (Must pass with 0 errors).

---

### 🧑‍💻 Person B (Intern)
* **🎯 Weekly Goal:** Model `AudioContent` reusable embeddable, create `DictationItem` data model, and write dictation repository queries.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`common/base/AudioContent.java`**:
     - `@Embeddable` class:
       - `audioUrl` (`@Column(name = "audio_url", nullable = false)`),
       - `transcript` (`@Column(name = "transcript", columnDefinition = "TEXT", nullable = false)`),
       - `durationSeconds` (`@Column(name = "duration_seconds", nullable = false)`),
       - `accent` (`@Column(length = 20)` - e.g. "BRITISH", "AMERICAN", "AUSTRALIAN").
  2. **`modules/dictation/entities/DictationItem.java` & `DictationAttempt.java`**:
     - `DictationItem` extends `BaseEntity`. `@Entity @Table(name = "dictation_items")`:
       - `title` (`@Column(nullable = false)`),
       - `cefrLevel` (`@Column(length = 10, nullable = false)`),
       - `topic` (`@Column(length = 50)`),
       - `@Embedded private AudioContent audio;`.
     - `DictationAttempt` extends `BaseEntity`. `@Entity @Table(name = "dictation_attempts")`:
       - `userId` (UUID, nullable=false),
       - `dictationItem` (`@ManyToOne @JoinColumn(name = "item_id") DictationItem dictationItem;`),
       - `userInput` (`@Column(columnDefinition = "TEXT", nullable = false)`),
       - `accuracyPercentage` (Double),
       - `correctWordsCount` (int),
       - `totalWordsCount` (int).
  3. **`modules/dictation/repository/DictationItemRepository.java`**:
     - `List<DictationItem> findByCefrLevel(String level);`
     - `Page<DictationItem> findByTopic(String topic, Pageable pageable);`
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/modules/dictation/DictationRepositoryTest.java`:
    - Test saving `DictationItem` with embedded `AudioContent` and retrieving by `cefrLevel`.
  - Verification Command: `mvn test -Dtest=DictationRepositoryTest` (Must pass with 0 errors).

---

## 🟠 WEEK 8: Gemini AI Writing Scorer & Dictation Levenshtein Diff Engine

### 🧑‍💻 Person A (Fresher)
* **🎯 Weekly Goal:** Implement production Gemini AI essay rubric scoring (`LlmEssayScorerImpl`), word count validation, and writing controllers.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`modules/writing/services/impl/LlmEssayScorerImpl.java`**:
     - `@Service @Profile("prod")` implementing `IEssayScorer`:
     - Combines prompt + user essay into `LlmPromptTemplate`.
     - Calls Person A's `ILlmClient.generate()`.
     - Parses returned JSON using `ObjectMapper` into `EssayScoringResult`. Throws `DomainException` if parsing fails.
  2. **`modules/writing/ports/IWritingService.java` & `WritingServiceImpl.java`**:
     - `EssaySubmissionResponse submitEssay(UUID userId, SubmitEssayRequest req)`:
       - Counts words (`text.trim().split("\\s+").length`).
       - Enforces word count thresholds: Task 1 minimum 150 words, Task 2 minimum 250 words (throws `BadRequestException` if below threshold).
       - Saves initial `EssaySubmission`.
       - Calls injected `IEssayScorer` (decoupled from mock vs prod).
       - Persists 4 `EssayFeedback` records and overall band score onto `EssaySubmission`.
     - `EssaySubmissionResponse getSubmission(UUID userId, UUID submissionId)`.
     - `Page<EssaySubmissionResponse> getUserSubmissions(UUID userId, Pageable pageable)`.
  3. **`modules/writing/controllers/WritingController.java`**:
     - `POST /api/writing/submissions` -> HTTP 201 with score and feedback breakdown.
     - `GET /api/writing/submissions/{id}` -> HTTP 200 with details.
     - `GET /api/writing/submissions/my` -> HTTP 200 with history.
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/modules/writing/WritingServiceTest.java`:
    - Test word count rejection (< 150 words -> HTTP 400).
    - Test saving essay and persisting 4 criteria feedback records.
  - Controller Tests in `src/test/java/com/ieltsplatform/modules/writing/WritingControllerTest.java` (MockMvc).
  - Verification Command: `mvn test -Dtest=WritingServiceTest,WritingControllerTest` (Must pass with 0 errors).

---

### 🧑‍💻 Person B (Intern)
* **🎯 Weekly Goal:** Build the word-level Levenshtein diff comparison engine and complete the dictation submission and scoring service.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`modules/dictation/dtos/WordDiffDto.java` & `DiffStatus.java`**:
     - `DiffStatus` enum: `CORRECT`, `TYPO`, `MISSING`, `EXTRA`.
     - `WordDiffDto`: `String expectedWord`, `String actualWord`, `DiffStatus status`, `int editDistance`.
     - `DictationResultResponse`: `double accuracyPercentage`, `int correctWords`, `int totalWords`, `List<WordDiffDto> diffBreakdown`.
  2. **`modules/dictation/ports/IDictationScorer.java` & `LevenshteinDictationScorerImpl.java`**:
     - Word-level scoring algorithm:
       - Strips punctuation and normalizes casing from both strings.
       - Computes token alignment between `transcriptTokens` and `userInputTokens`.
       - For matching positions:
         - If `distance == 0` -> `CORRECT`.
         - If `distance <= 2` -> `TYPO` (minor spelling error).
         - If word omitted -> `MISSING`.
         - If extraneous word entered -> `EXTRA`.
       - Computes `accuracy = (correctTokens / totalExpectedTokens) * 100`.
  3. **`modules/dictation/ports/IDictationService.java` & `DictationServiceImpl.java`**:
     - `DictationResultResponse submitDictation(UUID userId, UUID itemId, String userInput)`:
       - Loads `DictationItem`; invokes `LevenshteinDictationScorerImpl`; saves `DictationAttempt`; returns diff breakdown.
  4. **`modules/dictation/controllers/DictationController.java`**:
     - `GET /api/dictation/items?level=B2` -> HTTP 200 list of exercises.
     - `GET /api/dictation/items/{id}` -> HTTP 200 details (audio URL, duration, difficulty).
     - `POST /api/dictation/items/{id}/submit` -> HTTP 200 with accuracy and word diff list.
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `src/test/java/com/ieltsplatform/modules/dictation/LevenshteinDictationScorerTest.java`:
    - Test exact match -> 100% accuracy.
    - Test single letter typo (e.g. "enviroment" vs "environment") -> flagged as `TYPO`.
    - Test omitted word -> flagged as `MISSING`.
  - Controller Tests in `src/test/java/com/ieltsplatform/modules/dictation/DictationControllerTest.java` (MockMvc).
  - Verification Command: `mvn test -Dtest=LevenshteinDictationScorerTest,DictationControllerTest` (Must pass with 0 errors).

---

## 🟠 WEEK 9: Cross-Feature Linking, OpenAPI Docs & Backend Hardening

### 🧑‍💻 Person A (Fresher)
* **🎯 Weekly Goal:** Polish OpenAPI Swagger UI with Bearer auth, implement Redis rate limiting, and write the full backend regression suite.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`common/config/OpenApiConfig.java`**:
     - Configures Swagger 3 specification with Bearer JWT SecurityScheme (`type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT"`).
     - Documents tags: `Auth`, `Users`, `Reading Content`, `Assessment`, `Writing AI`, `Flashcards`, `Dictation`.
  2. **`common/security/RateLimitFilter.java`**:
     - Redis-backed token bucket rate limiter:
       - Restricts `POST /api/auth/login` to 10 requests / min per IP.
       - Restricts `POST /api/writing/submissions` to 5 requests / min per user.
       - Returns HTTP 429 (`Too Many Requests`) with `Retry-After` header when limit exceeded.
  3. **Full System Integration Test Suite (`IeltsBackendIntegrationTest.java`)**:
     - Full automated journey test:
       - 1. Signup user -> Login -> Receive JWT.
       - 2. Start Reading Assessment -> Save answers -> Submit exam -> Assert Band Score calculated.
       - 3. Submit Writing Essay -> Assert 4 criteria returned.
* **✅ Expected Results & Automated Test Gate:**
  - Verify Swagger UI is interactive at `http://localhost:8080/swagger-ui.html`.
  - Execute full test suite: `mvn clean test`.
  - All tests across all modules pass with 100% success rate.

---

### 🧑‍💻 Person B (Intern)
* **🎯 Weekly Goal:** Implement cross-feature word extraction into Flashcards (from Reading & Dictation) and seed realistic dictation exercises.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **Cross-Feature Word Extraction Endpoints**:
     - In `modules/content/controllers/ContentController.java`:
       - `POST /api/content/passages/{id}/flashcards` (Body: `{ "word": "sustainable" }`):
       - Calls `flashcardService.addWord(userId, word, SourceTag.READING_PASSAGE, passageId)`.
     - In `modules/dictation/controllers/DictationController.java`:
       - `POST /api/dictation/items/{id}/flashcards` (Body: `{ "word": "fluctuation" }`):
       - Calls `flashcardService.addWord(userId, word, SourceTag.DICTATION, itemId)`.
  2. **`modules/dictation/seed/DictationDataSeeder.java`**:
     - Seed 5 real IELTS audio dictation exercises across levels B1, B2, and C1.
     - Configures clean MP3 sample audio URLs and verified reference transcripts.
  3. **`src/test/java/com/ieltsplatform/modules/flashcard/FlashcardIntegrationTest.java`**:
     - Test adding word from Reading passage -> verify card appears in `GET /api/flashcards` with `sourceTag == READING_PASSAGE`.
     - Test reviewing card -> verify `nextReviewAt` is scheduled.
* **✅ Expected Results & Automated Test Gate:**
  - Automated Tests in `FlashcardIntegrationTest.java` pass cleanly.
  - End-to-end verification: Dictate audio -> extract mistaken word to flashcard -> card stored with definition.
  - Verification Command: `mvn test -Dtest=FlashcardIntegrationTest` (Must pass with 0 errors).

---

# 🔵 PHASE 4: Vertical Frontend Implementation (Weeks 10–12)

---

## 🔵 WEEK 10: Architectural Sync, Shared Foundation & Core Catalogs

### 🧑‍💻 Person A & Person B (Joint Kickoff - Days 1 to 3)
* **🎯 Joint Goal:** Establish frontend architectural rules, standardize design tokens, install shadcn/ui primitives, and set up shared layout.
* **🛠️ What Needs to Be Done (Days 1–3):**
  1. **Day 1: Architecture Sync & Component Breakdown**:
     - Agree on directory layout: `src/app/`, `src/components/ui/`, `src/components/shared/`, `src/lib/api/`, `src/hooks/`.
     - Establish shared primitives: `Button`, `Input`, `Dialog/Modal`, `TimerBadge`, `AudioPlayer`, `CardContainer`.
     - Define TypeScript types mapped 1:1 to backend DTOs.
  2. **Days 2–3: Next.js 16 Setup & Global Auth Context**:
     - Initialize Next.js 16 with TypeScript, Tailwind CSS v4, and shadcn/ui.
     - Set up Axios / TanStack Query client with JWT bearer interceptor and token refresh handling.
     - Build Root Layout, Navigation Header with auth state indicator, and Theme Provider.

---

### 🧑‍💻 Person A (Fresher - Days 4 & 5)
* **🎯 Weekly Goal:** Build Auth UI (Login Modal, Signup Form with validation, and User Profile dropdown).
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`src/components/auth/LoginModal.tsx` & `SignupModal.tsx`**:
     - Form validated with React Hook Form + Zod schema matching backend constraints.
     - Submits to `POST /api/auth/login` and `POST /api/auth/signup`.
     - Stores tokens in `localStorage` / HTTP-only cookies; updates `AuthContext`.
  2. **`src/components/layout/UserNav.tsx` & `src/app/profile/page.tsx`**:
     - User avatar dropdown with Logout trigger.
     - Profile page allowing user to update Name, Target Band Score (e.g. 7.5), and view member status.
* **✅ Expected Results:**
  - User can open browser to `http://localhost:3000`, click "Sign In", authenticate, and see their profile avatar in the navigation bar.

---

### 🧑‍💻 Person B (Intern - Days 4 & 5)
* **🎯 Weekly Goal:** Build the Reading Content Explorer page with level filtering and the responsive passage reader view.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`src/app/reading/page.tsx`**:
     - Catalog grid showing Reading Passage cards (Title, Topic badge, CEFR level badge, word count estimate).
     - Filter pill bar: `All`, `B1 (Intermediate)`, `B2 (Upper-Intermediate)`, `C1 (Advanced)`.
     - Connects via TanStack Query to `GET /api/content/passages`.
  2. **`src/app/reading/[id]/page.tsx`**:
     - Clean typography passage reader view with adjustable font size.
     - Text selection hook: highlighting any word triggers a popover menu ("Add to Flashcards").
* **✅ Expected Results:**
  - User can browse all reading passages, filter by CEFR level, click a passage to read, and highlight text with interactive tooltip.

---

## 🔵 WEEK 11: Mock Exam Interface & Spaced Repetition UI

### 🧑‍💻 Person A (Fresher)
* **🎯 Weekly Goal:** Build the authentic IELTS Reading Mock Exam arena with split-pane view, countdown timer, auto-submit, and Band Score dashboard.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`src/app/assessment/[passageId]/page.tsx`**:
     - Split-pane layout: Reading passage on the left (scrollable), Question sheet on the right (independent scroll).
     - Floating Countdown Timer bar: Displays remaining minutes/seconds; turns amber at 5 min, flashing red at 1 min.
     - Radio group component for Multiple Choice questions; 3-way toggle for True / False / Not Given.
     - Auto-save on click: Triggers `PUT /api/assessment/attempts/{id}/answers` in the background.
     - Auto-submit trigger when timer hits `00:00`.
  2. **`src/app/assessment/results/[attemptId]/page.tsx`**:
     - Band Score display badge (e.g. "Band 7.5 - Good User").
     - Summary metrics: Accuracy %, correct count / total questions, total time spent.
     - Question review accordion: Displays correct answer vs user answer and official explanation.
* **✅ Expected Results:**
  - User can start a timed IELTS exam, answer questions under exam pressure, auto-submit upon timer completion, and view their official band score report.

---

### 🧑‍💻 Person B (Intern)
* **🎯 Weekly Goal:** Build the Flashcards Deck management page and interactive 3D Flip Card review arena with SM-2 quality rating buttons.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`src/app/flashcards/page.tsx`**:
     - Overview banner: "Cards Due Today" counter, Total deck size, Mastered count.
     - Vocabulary table with search filter and SourceTag pill badges (`Reading`, `Dictation`, `Manual`).
     - "Quick Add Word" dialog modal submitting to `POST /api/flashcards`.
  2. **`src/app/flashcards/review/page.tsx`**:
     - 3D Flip Card animation:
       - Front side: Target word, phonetics, audio pronunciation button.
       - Back side (revealed on click/spacebar): Definition, part of speech, and highlighted example sentence.
     - SM-2 Rating Bar: 6 buttons (0="Blackout", 1="Wrong", 2="Struggled", 3="Passed", 4="Good", 5="Perfect").
     - Keyboard shortcuts: Keys 0 through 5 submit review score to `POST /api/flashcards/{id}/review` and load next card.
     - Review completed celebration screen when no more cards are due.
* **✅ Expected Results:**
  - User can browse their flashcards, click "Start Review", flip through due cards using keyboard or mouse, rate recall quality, and see the queue update in real time.

---

## 🔵 WEEK 12: AI Writing Feedback UI, Dictation Room & Final Polish

### 🧑‍💻 Person A (Fresher)
* **🎯 Weekly Goal:** Build the AI Writing submission editor with real-time word counter and the interactive 4-criteria IELTS feedback report card.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`src/app/writing/page.tsx`**:
     - Task 1 & Task 2 selection tabs with official IELTS prompts.
     - Distraction-free essay text editor.
     - Live Word Counter: Displays word count dynamically; turns green when meeting threshold (>= 150 words for Task 1, >= 250 words for Task 2).
     - "Analyze Essay with AI" submission button with loading spinner state.
  2. **`src/app/writing/feedback/[id]/page.tsx`**:
     - Overall Band Score circular progress card.
     - 4-Criteria Radar Chart & Breakdown:
       - Task Achievement score & commentary.
       - Coherence & Cohesion score & commentary.
       - Lexical Resource score & commentary.
       - Grammatical Range & Accuracy score & commentary.
     - Inline sentence improvements list: side-by-side comparison of user sentence vs improved native version.
* **✅ Expected Results:**
  - User can write an IELTS essay, see word counts update live, submit to Gemini AI, and inspect deep rubric feedback with actionable corrections.

---

### 🧑‍💻 Person B (Intern)
* **🎯 Weekly Goal:** Build the interactive Dictation practice player with playback speed controls, real-time typing input, and visual mistake diff highlights.
* **🛠️ What Needs to Be Done (Deep Technical Specification):**
  1. **`src/app/dictation/[id]/page.tsx`**:
     - Custom Audio Player: Play/Pause button, speed controls (0.8x, 1.0x, 1.2x), and instant 5-second rewind keyboard shortcut (Arrow Left).
     - Clean typing input area with auto-focus and auto-scroll.
     - "Check My Dictation" submit button triggering `POST /api/dictation/items/{id}/submit`.
  2. **`src/components/dictation/DiffResults.tsx`**:
     - Visual diff rendering:
       - Correct words highlighted in Green.
       - Misspelled words highlighted in Amber with hover tooltip showing expected spelling.
       - Omitted words shown with Red strike-through.
     - One-click "+" icon next to any mistake allowing instant addition to user's Flashcards deck.
* **✅ Expected Results:**
  - User can listen to dictation audio at custom speed, type what they hear, check results, see exact color-coded error highlights, and save misspelled words to flashcards in one click.

---

### 🧑‍💻 Person A & Person B (Joint Final Delivery - Week 12 Days 4 & 5)
* **🎯 Final Milestone:**
  1. **End-to-End User Journey Smoke Test:**
     - User signs up -> browses reading passages -> takes timed mock exam -> receives Band 7.0 report.
     - User reads passage -> selects unfamiliar word -> saves to flashcard -> reviews card with SM-2 flip animation.
     - User writes Task 2 essay -> submits to Gemini AI -> receives 4-criteria feedback report.
     - User completes dictation exercise -> reviews Levenshtein diff highlights -> saves misspelled word to deck.
  2. **Production Build & Verification:**
     - Frontend: `npm run build` succeeds with 0 linting or type errors.
     - Backend: `mvn clean test` passes 100% of test cases.
     - Deployable full-stack application ready for demo and release!
