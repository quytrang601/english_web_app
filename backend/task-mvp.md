# **IELTS Platform — Backend-Only MVP Plan (4 Weeks)**

Frontend is explicitly out of scope for these 4 weeks — both people build it together later. This plan covers backend only: architecture, full folder structure, and a file-by-file task guide.

## **Team & Split**

* **Person A:** Auth, User, Dictation + all shared infrastructure (security, LLM/TTS/STT/storage clients).  
* **Person B** *(new to SWE)*: Mock tests across all 4 skills (Reading, Listening, Writing, Speaking) + Flashcards. Mostly CRUD by design; the genuinely harder spots are flagged with 🔶 below, and Person A is free to pair on those from Week 3 onward (their own scope is done by end of Week 2).

---

## **1. Folder Structure & Base Entity Convention**

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

**Why `dtos/` is separate from `entities/`:** entities are your internal DB shape; DTOs are the external contract. This keeps internal-only fields (password hashes, etc.) from ever leaking out, and lets your schema evolve without breaking API consumers.

---

## **2. Full Backend Folder Tree**

```text
backend/
├── src/main/java/com/ieltsplatform/
│   ├── IeltsPlatformApplication.java                  # Spring Boot entrypoint; also where @EnableJpaAuditing goes
│   │
│   ├── common/                                         # [Person A] Shared kernel
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── OpenApiConfig.java
│   │   │   └── RedisConfig.java
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── ApiError.java
│   │   │   └── DomainException.java
│   │   ├── base/
│   │   │   └── BaseEntity.java                   # BASE ENTITY — ALL JPA entities extend this class for metadata (id, createdAt, updatedAt)
│   │   ├── ports/
│   │   │   ├── ILlmClient.java
│   │   │   ├── ITtsClient.java
│   │   │   ├── ISttClient.java
│   │   │   └── IStorageClient.java
│   │   └── services/impl/
│   │       ├── ClaudeLlmClientImpl.java
│   │       ├── AzureTtsClientImpl.java
│   │       ├── WhisperSttClientImpl.java
│   │       └── S3StorageClientImpl.java
│   │
│   └── modules/
│       ├── auth/                          # [A] entities/ repository/ dtos/ mapper/ ports/ services/impl/ controllers/
│       ├── user/                          # [A] same shape
│       ├── dictation/                     # [A] same shape
│       ├── content/                       # [B] same shape
│       ├── assessment/                    # [B] same shape
│       ├── listening/                     # [B] same shape
│       ├── writing/                       # [B] same shape
│       ├── speaking/                      # [B] same shape
│       └── flashcard/                     # [B] same shape
│
└── src/main/resources/
    └── db/migration/
        ├── V1__init_users.sql             # [A] Users & authentication tables
        ├── V2__init_content.sql           # [B] Reading passages content table
        ├── V3__init_dictation.sql         # [A] Dictation exercises table
        ├── V4__init_assessment.sql        # [B] Test attempts & answers tables
        ├── V5__init_listening.sql         # [B] Listening audio sections & questions
        ├── V6__init_writing.sql           # [B] Essay submissions & feedback tables
        ├── V7__init_speaking.sql          # [B] Speaking sessions & audio records
        └── V8__init_flashcards.sql        # [B] Flashcards & SM-2 review tables
```

---

## **3. The Flashcard Feature, Defined (Person B's centerpiece)**

* **Add from anywhere:** a word/phrase can be added to the deck from a reading passage, an essay, a dictation transcript, a speaking transcript, or typed manually.  
* **Duplicate detection:** on add, normalize the word to its base form (lemma) and check if it already exists for that user before inserting.  
* **Source tagging:** every card remembers where it was learned (a specific passage, "Speaking Room," "Dictation," or a free-text external tag).  
* **Auto-generated definitions:** on add, call the LLM for a definition + example sentence; cache by word so it's only generated once, not once per user.  
* **Spaced-repetition review:** SM-2 algorithm schedules the next review date based on how well the user recalled the card.  
* **Central vocabulary page (API only for now):** `GET /flashcards` lists everything, filterable by tag/mastery/date.  
* **Mastery tracking:** a card can be marked mastered to stop appearing in daily review.

**Data model (All entities extend `BaseEntity`):**

* `Flashcard` (Extends `BaseEntity`): `userId, word, lemma, definition, exampleSentence, sourceTag, masteryLevel` *(metadata: `id, createdAt, updatedAt` inherited)*  
* `FlashcardReview` (Extends `BaseEntity`, **one-to-one** with `Flashcard`): `flashcard (FK), nextReviewAt, easeFactor, intervalDays, lastReviewedAt` *(metadata: `id, createdAt, updatedAt` inherited)*

---

## **4. Week-by-Week File-Level Task Guide**

### **Week 1 — Foundation**

#### **Person A**

| File | What to do |
| ----- | ----- |
| `common/config/SecurityConfig.java` | `SecurityFilterChain` bean: permit `/auth/**` and Swagger paths, require auth on everything else; register the JWT filter; `@EnableMethodSecurity` for later `@PreAuthorize` use; configure CORS allowed origins. |
| `common/config/OpenApiConfig.java` | `OpenAPI` bean with title/version + a Bearer-token security scheme so Swagger UI has an "Authorize" button. |
| `common/config/RedisConfig.java` | `RedisConnectionFactory` (Lettuce) + `RedisTemplate<String,String>` beans — used for rate limiting now, speaking session state in Week 3. |
| `common/exception/ApiError.java` | Fields: `timestamp, status, error, message, path`. Plain data class. |
| `common/exception/DomainException.java` | Abstract class extending `RuntimeException`, with an `errorCode` field every module's own exceptions will set. |
| `common/exception/GlobalExceptionHandler.java` | `@ControllerAdvice`; `@ExceptionHandler` methods for `DomainException` → 400, `MethodArgumentNotValidException` (bean validation failures) → 400 with field errors, generic `Exception` → 500. Always return `ApiError`. |
| `common/base/BaseEntity.java` | `@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`: Base entity for metadata — `id` (`@Id @GeneratedValue UUID`), `createdAt` (`@CreatedDate`), `updatedAt` (`@LastModifiedDate`). **All module JPA entities MUST extend this class.** |
| `common/ports/ILlmClient.java` | One method: `String generate(String prompt)`. |
| `common/ports/ITtsClient.java` | One method: `AudioResult synthesize(String script, String voiceId)` — returns audio bytes + duration. |
| `common/ports/ISttClient.java` | One method: `String transcribe(byte[] audioBytes)`. |
| `common/ports/IStorageClient.java` | Methods: `String upload(String key, byte[] content, String contentType)`, `String presignedUrl(String key)`, `void delete(String key)`. |
| `modules/auth/entities/RefreshToken.java` | Extends `BaseEntity`. Fields: `userId (UUID), tokenHash (String), expiresAt (Instant), revoked (boolean)`. |
| `modules/auth/repository/RefreshTokenRepository.java` | `extends JpaRepository<RefreshToken, UUID>` + `Optional<RefreshToken> findByTokenHash(String hash)`. |
| `modules/auth/dtos/request/SignupRequest.java` | `email` (`@Email @NotBlank`), `password` (`@NotBlank @Size(min=8)`), `name`. |
| `modules/auth/dtos/request/LoginRequest.java` | `email`, `password`. |
| `modules/auth/dtos/response/AuthTokenResponse.java` | `accessToken, refreshToken, expiresIn`. |
| `modules/auth/ports/IAuthService.java` | `AuthTokenResponse signup(SignupRequest)`, `login(LoginRequest)`, `refresh(String refreshToken)`, `void logout(String refreshToken)`. |
| `modules/auth/services/impl/AuthServiceImpl.java` | Implements `IAuthService`. `signup()`: check email uniqueness via `UserRepository`, hash password (`PasswordEncoder`), save `User`, issue tokens. `login()`: look up by email, verify password, issue tokens. `refresh()`: validate the stored `RefreshToken` (not expired/revoked), rotate it, issue a new access token. `logout()`: mark the token revoked. |
| `modules/auth/controllers/AuthController.java` | `POST /api/auth/signup`, `/login`, `/refresh`, `/logout` — each just calls `IAuthService` and wraps the result in `ResponseEntity`. |
| `modules/user/entities/User.java` | Extends `BaseEntity`. Fields: `email (unique), passwordHash, name, role (UserRole), locale, avatarUrl`. |
| `modules/user/entities/UserRole.java` | Enum: `FREE, PAID, ADMIN`. |
| `modules/user/repository/UserRepository.java` | `extends JpaRepository<User, UUID>` + `Optional<User> findByEmail(String email)`. |
| `resources/db/migration/V1__init_users.sql` | `CREATE TABLE users (...)`, `CREATE TABLE refresh_tokens (...)` with FK to `users`. |

#### **Person B**

| File | What to do |
| ----- | ----- |
| `docs/er-diagram.md` | Draw out `content`, `assessment`, `listening`, `writing`, `speaking`, `flashcard` entities and their relationships before writing any code. Review with Person A once — cheapest time to fix mistakes. |
| `modules/content/entities/ReadingPassage.java` | Extends `BaseEntity`. Fields: `title, body (Text), level (String, e.g., "B1"), topic, wordCount`. |
| `modules/content/repository/ReadingPassageRepository.java` | `extends JpaRepository<ReadingPassage, UUID>` + `List<ReadingPassage> findByLevel(String level)`. |
| `modules/content/dtos/response/ReadingPassageResponse.java` | `id, title, body, level, topic`. |
| `modules/content/mapper/ContentMapper.java` | Static method `toResponse(ReadingPassage entity)`. |
| `modules/content/ports/IContentService.java` | `List<ReadingPassageResponse> getPassages(String level)`, `ReadingPassageResponse getPassageById(UUID id)`. |
| `modules/content/services/impl/ContentServiceImpl.java` | Implements the above using the repository + mapper. Throw a `DomainException` (404-mapped) if `id` not found. |
| `modules/content/controllers/ContentController.java` | `GET /api/content/passages?level=`, `GET /api/content/passages/{id}`. |
| `resources/db/migration/V2__init_content.sql` | `CREATE TABLE reading_passages (...)`. |
| `(seed data)` | A `data.sql` or a small `CommandLineRunner` bean that inserts ~10 real reading passages across a few levels — this is your test data for Week 2. |

**Deliverable:** Signup/login/refresh/logout work via Swagger; reading passages are seeded and browsable by level.

---

### **Week 2 — Dictation (A) / Assessment Engine + Listening (B)**

#### **Person A**

| File | What to do |
| ----- | ----- |
| `common/services/impl/ClaudeLlmClientImpl.java` | Implements `ILlmClient` — HTTP call to the Claude API, `generate()` sends the prompt, returns the text response. |
| `common/services/impl/AzureTtsClientImpl.java` | Implements `ITtsClient` — calls Azure AI Speech (or your chosen provider), returns audio bytes + duration. |
| `common/services/impl/WhisperSttClientImpl.java` | Implements `ISttClient` — calls Whisper (self-hosted or cloud), returns the transcript string. |
| `common/services/impl/S3StorageClientImpl.java` | Implements `IStorageClient` — AWS SDK (or MinIO client) calls for upload/presign/delete. |
| `modules/user/dtos/request/UpdateProfileRequest.java`, `modules/user/dtos/response/UserResponse.java` | `UpdateProfileRequest`: `name, locale`. `UserResponse`: everything except `passwordHash`. |
| `modules/user/mapper/UserMapper.java` | `toResponse(User)`. |
| `modules/user/ports/IUserService.java`, `modules/user/ports/IPasswordResetService.java` | `IUserService`: `getProfile(userId)`, `updateProfile(userId, request)`, `uploadAvatar(userId, bytes)`. `IPasswordResetService`: `requestReset(email)`, `confirmReset(token, newPassword)`. |
| `modules/user/services/impl/UserServiceImpl.java` | Implements `IUserService`; `uploadAvatar()` calls `IStorageClient.upload()` and saves the returned URL onto the `User` entity. |
| `modules/user/services/impl/PasswordResetServiceImpl.java` | Implements `IPasswordResetService`; generates a signed/expiring token, stores or encodes it, sends via a simple email call (can stub actual sending this week). |
| `modules/user/controllers/UserController.java` | `GET /api/users/me`, `PATCH /api/users/me`, `POST /api/users/me/avatar`. |
| `modules/dictation/entities/DictationItem.java` | Extends `BaseEntity`. Fields: `audioUrl, transcript, level`. |
| `modules/dictation/repository/DictationItemRepository.java` | `extends JpaRepository<DictationItem, UUID>` + `List<DictationItem> findByLevel(String level)`. |
| `modules/dictation/dtos/request/SubmitDictationRequest.java`, `modules/dictation/dtos/response/DictationResultResponse.java` | Request: `userInput (String)`. Response: `correctWordCount, totalWordCount, diffHighlights (List)`. |
| `modules/dictation/mapper/DictationMapper.java` | `toItemResponse`, `toResultResponse`. |
| `modules/dictation/ports/IDictationService.java`, `modules/dictation/ports/IDictationScorer.java` | `IDictationService`: `getItem(id)`, `submit(id, request)`. `IDictationScorer`: `ScoreResult score(String transcript, String userInput)`. |
| `modules/dictation/services/impl/DictationScorerImpl.java` | Implements `IDictationScorer` — word-level diff (Levenshtein distance or a diff library) between `transcript` and `userInput`, tolerant of case/punctuation. |
| `modules/dictation/services/impl/DictationServiceImpl.java` | Implements `IDictationService`; `submit()` fetches the item, calls `IDictationScorer`, returns the result (no persistence of attempts needed for MVP unless you want history — keep it simple this week). |
| `modules/dictation/controllers/DictationController.java` | `GET /api/dictation/{id}`, `POST /api/dictation/{id}/submit`. |
| `resources/db/migration/V3__init_dictation.sql` | `CREATE TABLE dictation_items (...)`. |

#### **Person B**

| File | What to do |
| ----- | ----- |
| `modules/assessment/entities/TestAttempt.java` | Extends `BaseEntity`. Fields: `userId, skill (String: READING/LISTENING), startedAt, submittedAt, overallScore`. |
| `modules/assessment/entities/TestAnswer.java` | Extends `BaseEntity`. Fields: `attemptId (FK, ManyToOne to TestAttempt), questionId, userAnswer, isCorrect, score`. This is your first real `@ManyToOne`/`@OneToMany` relationship — one attempt has many answers. |
| `modules/assessment/repository/TestAttemptRepository.java`, `modules/assessment/repository/TestAnswerRepository.java` | Standard `JpaRepository`; `TestAnswerRepository` needs `List<TestAnswer> findByAttemptId(UUID attemptId)`. |
| `modules/assessment/dtos/request/SubmitAnswerRequest.java` | `questionId, userAnswer`. |
| `modules/assessment/dtos/response/TestAttemptResponse.java`, `modules/assessment/dtos/response/ScoreResponse.java` | Attempt state; final score breakdown. |
| `modules/assessment/mapper/AssessmentMapper.java` | Entity <-> DTO conversions. |
| `modules/assessment/ports/IAnswerScorer.java` | One method: `ScoreResult score(Question question, String userAnswer)`. |
| `modules/assessment/services/impl/McqScorerImpl.java` | Implements `IAnswerScorer` — exact match against the correct option. |
| `modules/assessment/services/impl/TfNgScorerImpl.java` | Implements `IAnswerScorer` — exact match against True/False/Not-Given. |
| `modules/assessment/services/impl/FillBlankScorerImpl.java` | Implements `IAnswerScorer` — case-insensitive, minor-typo-tolerant match (e.g., Levenshtein distance <= 1). |
| `modules/assessment/ports/IAssessmentService.java` | `startAttempt(userId, skill)`, `submitAnswer(attemptId, request)`, `submitAttempt(attemptId)`. |
| `modules/assessment/services/impl/AssessmentServiceImpl.java` | 🔶 Implements `IAssessmentService`. `startAttempt()`: create a `TestAttempt` with `startedAt = now()`. `submitAnswer()`: look up the question, pick the right `IAnswerScorer` by question type, save a `TestAnswer`. `submitAttempt()`: set `submittedAt`, **validate server-side that the elapsed time doesn't exceed the allowed limit** (don't trust any client-sent duration), sum up scores into `overallScore`. This time-validation piece is the trickiest logic this week — flag it for a quick review with Person A if unsure. |
| `modules/assessment/controllers/AssessmentController.java` | `POST /api/assessment/attempts`, `POST /api/assessment/attempts/{id}/answers`, `POST /api/assessment/attempts/{id}/submit`. |
| `modules/listening/entities/ListeningSection.java` | Extends `BaseEntity`. Fields: `audioUrl, transcript, level, questions (store as JSON column or a related entity — JSON column is fine for MVP)`. |
| `modules/listening/repository/ListeningSectionRepository.java` | Standard `JpaRepository` + `findByLevel`. |
| `modules/listening/dtos/response/ListeningSectionResponse.java` | `id, audioUrl, level, questions`. |
| `modules/listening/mapper/ListeningMapper.java` | `toResponse`. |
| `modules/listening/ports/IListeningService.java` | `getSection(id)`, `generateSection(script, level)` (admin/seed-time use). |
| `modules/listening/services/impl/ListeningServiceImpl.java` | 🔶 `generateSection()` calls `common/ports/ITtsClient.synthesize()` then `IStorageClient.upload()`, saves the returned URL + transcript. `getSection()` is plain CRUD. |
| `modules/listening/controllers/ListeningController.java` | `GET /api/listening/{id}`. |
| `resources/db/migration/V4__init_assessment.sql`, `resources/db/migration/V5__init_listening.sql` | Tables for `test_attempts`, `test_answers` (with FK to `test_attempts`), `listening_sections`. |
| `(seed data)` | One full Reading test's worth of questions against Week 1's passages, and one full Listening section (placeholder audio is fine if `ITtsClient` isn't ready yet — swap in real audio once it is). |

**Deliverable:** Dictation fully working via Swagger. A full Reading test and a full Listening section can be started, answered, and scored via Swagger.

---

### **Week 3 — Writing + Speaking**

#### **Person B**

| File | What to do |
| ----- | ----- |
| `modules/writing/entities/EssaySubmission.java` | Extends `BaseEntity`. Fields: `userId, promptId, essayText (Text), submittedAt`. |
| `modules/writing/entities/EssayFeedback.java` | Extends `BaseEntity`. Fields: `submissionId (FK, ManyToOne), criterion (String), score (Double), comments (Text/JSON)`. One submission -> many feedback rows (one per IELTS criterion). |
| `modules/writing/repository/EssaySubmissionRepository.java`, `modules/writing/repository/EssayFeedbackRepository.java` | Standard, plus `findBySubmissionId` on the feedback repo. |
| `modules/writing/dtos/request/SubmitEssayRequest.java` | `promptId, essayText`. |
| `modules/writing/dtos/response/EssayFeedbackResponse.java` | `submissionId, criteriaScores (List), overallBand`. |
| `modules/writing/mapper/WritingMapper.java` | Entity <-> DTO. |
| `modules/writing/ports/IEssayScorer.java` | `List<EssayFeedback> score(String essayText, String promptText)`. |
| `modules/writing/services/impl/EssayScorerImpl.java` | 🔶 Implements `IEssayScorer`. Build a prompt asking the LLM (via `common/ports/ILlmClient`) to return **structured JSON**: a score + comment per criterion (Task Response, Coherence, Lexical Resource, Grammar). Parse that JSON into `EssayFeedback` objects. Handle malformed/missing JSON gracefully (retry once, then throw a `DomainException`). |
| `modules/writing/ports/IWritingCheckService.java` | `EssayFeedbackResponse submitEssay(userId, SubmitEssayRequest)`. |
| `modules/writing/services/impl/WritingCheckServiceImpl.java` | Implements the above: save the `EssaySubmission`, call `IEssayScorer`, save the returned `EssayFeedback` rows, return the response. |
| `modules/writing/controllers/WritingController.java` | `POST /api/writing/submissions`, `GET /api/writing/submissions/{id}`. |
| `modules/speaking/entities/SpeakingSession.java` | Extends `BaseEntity`. Fields: `userId, questionId, audioUrl, transcript, feedback (JSON)`. |
| `modules/speaking/repository/SpeakingSessionRepository.java` | Standard `JpaRepository`. |
| `modules/speaking/dtos/response/SpeakingResultResponse.java` | `transcript, criteriaScores, overallBand`. |
| `modules/speaking/mapper/SpeakingMapper.java` | Entity <-> DTO. |
| `modules/speaking/ports/ISpeakingScorer.java` | `List<CriterionScore> score(String transcript)`. |
| `modules/speaking/services/impl/SpeakingScorerImpl.java` | Implements `ISpeakingScorer` — same LLM-structured-JSON pattern as `EssayScorerImpl`, scoring the 4 speaking criteria instead of the 4 writing criteria. |
| `modules/speaking/ports/ISpeakingService.java` | `uploadAndScore(userId, questionId, audioBytes)`. |
| `modules/speaking/services/impl/SpeakingServiceImpl.java` | 🔶 Uploads audio via `IStorageClient`, transcribes via `ISttClient`, saves the transcript, calls `ISpeakingScorer`, saves the result. Keep the Part-3 dynamic-follow-up-question logic as a stretch goal this week if time is short — the core record -> transcribe -> score loop is the priority. |
| `modules/speaking/controllers/SpeakingController.java` | `POST /api/speaking/sessions` (upload + score), `GET /api/speaking/sessions/{id}`. |
| `resources/db/migration/V6__init_writing.sql`, `resources/db/migration/V7__init_speaking.sql` | Tables for `essay_submissions`, `essay_feedback` (FK to submissions), `speaking_sessions`. |
| `(seed data)` | A handful of writing prompts and speaking questions across Part 1/2/3 categories. |

#### **Person A**

* Pairs with Person B on `EssayScorerImpl` and `SpeakingServiceImpl` (both 🔶) — A already knows `ILlmClient`/`ISttClient` inside out from building them.  
* Security hardening on Week 1–2 endpoints: rate limiting (`common/services/impl/RateLimitFilter.java` if not already done, using Redis from `RedisConfig`) on `/auth/**` and any AI-scoring endpoints once they exist.  
* Test coverage: write unit tests for `AuthServiceImpl`, `UserServiceImpl`, `DictationScorerImpl`.

**Both:** Manually QA the Writing and Speaking scoring output against ~10 real sample essays/answers across band levels before calling either "done."

**Deliverable:** Writing submissions get real AI feedback. Speaking sessions can be recorded, transcribed, and scored.

---

### **Week 4 — Flashcards + Backend Hardening**

#### **Person B**

| File | What to do |
| ----- | ----- |
| `modules/flashcard/entities/Flashcard.java` | Extends `BaseEntity`. Fields: `userId, word, lemma, definition, exampleSentence, sourceTag, masteryLevel (enum or int)`. |
| `modules/flashcard/entities/FlashcardReview.java` | Extends `BaseEntity`. Fields: `flashcard (FK, OneToOne), nextReviewAt, easeFactor (Double), intervalDays (Int), lastReviewedAt`. |
| `modules/flashcard/entities/SourceTag.java` | Enum: `READING_PASSAGE, LISTENING, WRITING, SPEAKING, DICTATION, EXTERNAL, MANUAL` (+ a free-text field on `Flashcard` for the `EXTERNAL` case). |
| `modules/flashcard/repository/FlashcardRepository.java`, `modules/flashcard/repository/FlashcardReviewRepository.java` | `FlashcardRepository` needs `Optional<Flashcard> findByUserIdAndLemma(userId, lemma)` for dedup. `FlashcardReviewRepository` needs `List<FlashcardReview> findByFlashcard_UserIdAndNextReviewAtBefore(userId, now)` for the "due" query. |
| `modules/flashcard/dtos/request/AddFlashcardRequest.java` | `word, sourceTag, sourceDetail (optional free text)`. |
| `modules/flashcard/dtos/response/FlashcardResponse.java` | `id, word, definition, exampleSentence, sourceTag, masteryLevel, nextReviewAt`. |
| `modules/flashcard/mapper/FlashcardMapper.java` | Entity (+review) <-> DTO. |
| `modules/flashcard/ports/ISpacedRepetitionScheduler.java` | `ReviewSchedule computeNext(FlashcardReview current, int recallQuality)` (quality typically 0-5, per SM-2). |
| `modules/flashcard/services/impl/Sm2SchedulerImpl.java` | 🔶 Implements the SM-2 algorithm: adjusts `easeFactor` based on `recallQuality`, computes the next `intervalDays`, sets `nextReviewAt`. This is a good one to write a unit test for first since it's pure logic with no DB/HTTP dependency — feed it known inputs and check the outputs match the documented SM-2 formula. |
| `modules/flashcard/ports/IFlashcardService.java` | `addWord(userId, request)`, `listFlashcards(userId, filters)`, `getDueCards(userId)`, `review(flashcardId, recallQuality)`. |
| `modules/flashcard/services/impl/FlashcardServiceImpl.java` | `addWord()`: normalize `word` to `lemma`, check `findByUserIdAndLemma` — if found, return the existing card (don't duplicate); if not, call `common/ports/ILlmClient` for a definition + example sentence, save the new `Flashcard` + an initial `FlashcardReview`. `review()`: fetch the review row, call `ISpacedRepetitionScheduler.computeNext()`, save the updated schedule. |
| `modules/flashcard/controllers/FlashcardController.java` | `POST /api/flashcards`, `GET /api/flashcards`, `GET /api/flashcards/due`, `POST /api/flashcards/{id}/review`. |
| `resources/db/migration/V8__init_flashcards.sql` | `CREATE TABLE flashcards (...)`, `CREATE TABLE flashcard_reviews (...)` with a unique FK to `flashcards` (one-to-one). |
| `(regression testing)` | Full regression pass across `content`, `assessment`, `listening`, `writing`, `speaking`, `flashcard` — re-test every endpoint via Swagger. |

#### **Person A**

* Backend security/hardening pass across the whole app: CORS finalized, dependency CVE scan (OWASP Dependency-Check or Dependabot), confirm no secrets are hardcoded anywhere, rate limits verified on all AI-scoring and auth endpoints.  
* Finalize `resources/application-prod.yml` — all values sourced from environment variables.  
* Set up backend production deployment target (container host + managed Postgres + managed Redis) — deploy, but no frontend to point at it yet, so this is really "prove it boots and connects correctly in a prod-like environment."  
* Pairs with Person B if `Sm2SchedulerImpl` or the dedup logic in `FlashcardServiceImpl` needs a second pair of eyes.

**Both:**

* Full backend integration test end-to-end via Swagger: signup -> take a Reading test -> take a Listening test -> submit a Writing essay and get feedback -> record and score a Speaking response -> add a flashcard from a reading passage and review it -> do a Dictation exercise.  
* Fix bugs found; finalize OpenAPI docs; write a short backend runbook (how to run migrations, how to deploy, where logs live).

**Deliverable:** Fully working, fully tested backend for all 6 modules — reachable and demoable entirely through Swagger. No frontend yet; that's next month's work for both of you together.

---

## **5. Notes**

* Speaking remains the single feature most worth watching for schedule risk (STT + LLM + the most moving parts). If Week 3 runs long, it's the easiest to trim to "records and transcribes, scoring deferred" without blocking anything else.  
* Every 🔶 item above shares the same shape: call an interface Person A already built (`ILlmClient`, `ISttClient`, `ITtsClient`), and handle the response. None of them require building a new integration from scratch.
