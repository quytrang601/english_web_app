# User Flow & Technical Specifications: Skill Practice & Training Drills

**Module Scope:** Requirements FR-10.1 through FR-12.5  
**Target Path:** `backend/feature-flows/10-skill-practice-drills-flow.md`

---

## 1. Executive Summary & User Journey

The Skill Practice & Training Module provides targeted, untimed micro-exercises distinct from full official IELTS mock tests. It allows students to isolate and strengthen specific skills across:
1. **Objective Reading & Listening Drills** (isolated question types like Matching Headings, True/False/Not Given, Fill-in-Blanks).
2. **Writing Drills** (sentence building, Task 1 chart overview descriptions, Task 2 thesis statements, model comparisons).
3. **Grammar & Vocabulary Quizzes** (CEFR-aligned interactive quizzes linked directly to the Reference Library).

This module strictly implements the system 7-folder module structure (`entities/`, `repository/`, `dtos/`, `mapper/`, `ports/`, `services/impl/`, `controllers/`), Dependency Inversion (`PracticeController` depends strictly on `IPracticeService`), and explicit static DTO mapping via `PracticeMapper` / `AssessmentMapper`. All domain entities (`PracticeExercise`, `PracticeAttempt`) extend `com.ieltsplatform.common.base.BaseEntity`.

---

## 2. Component Reusability Patterns Applied

- **Pattern 2 (`IAnswerScorer` Strategy Registry)**: Objective drill questions (MCQ, T/F/NG, Fill Blank) are evaluated by `PracticeServiceImpl` dispatching to `AnswerScorerRegistry` (`Map<SkillType, IAnswerScorer>`), reusing `McqScorerImpl`, `TfNgScorerImpl`, and `FillBlankScorerImpl` (with Levenshtein distance $\le 1$ typo tolerance). This guarantees **zero code duplication** between isolated practice drills and full `TestAttempt` mock exams.
- **Pattern 3 (`LlmPromptTemplate`)**: Writing paragraph drills (e.g. Task 1 overview description, Task 2 thesis statement) use `LlmPromptTemplate` to construct structured evaluation prompts. `PracticeServiceImpl` executes prompts via `ILlmClient` (Google Gemini API adapter) and parses JSON output into typed criteria feedback DTOs.
- **Pattern 4 (`IFlashcardService`)**: Vocabulary items encountered during reading drills or grammar quizzes can be saved directly to flashcard decks via `IFlashcardService.addWord(userId, word, sourceTag, exerciseId)` using `SourceTag.QUIZ`, `SourceTag.READING_PASSAGE`, or `SourceTag.SAMPLE_ESSAY`.

---

## 3. Module Structure & Component Layout

```
backend/src/main/java/com/ieltsplatform/modules/assessment/
├── controllers/
│   └── PracticeController.java
├── dtos/
│   ├── PracticeAttemptResponse.java
│   ├── PracticeExerciseResponse.java
│   ├── PracticeSubmissionRequest.java
│   ├── WritingParagraphScoreRequest.java
│   └── WritingParagraphScoreResponse.java
├── entities/
│   ├── PracticeAttempt.java
│   └── PracticeExercise.java
├── mapper/
│   └── PracticeMapper.java
├── ports/
│   ├── IAnswerScorer.java
│   ├── IFlashcardService.java
│   ├── ILlmClient.java
│   └── IPracticeService.java
├── repository/
│   ├── PracticeAttemptRepository.java
│   └── PracticeExerciseRepository.java
└── services/impl/
    ├── AnswerScorerRegistry.java
    ├── FillBlankScorerImpl.java
    ├── McqScorerImpl.java
    ├── PracticeServiceImpl.java
    └── TfNgScorerImpl.java
```

### Shared Kernel Contracts
- `com.ieltsplatform.common.base.BaseEntity`: Provides `id` (UUID), `createdAt` (Instant), `updatedAt` (Instant), and `version` (Long).
- `com.ieltsplatform.common.ports.IAnswerScorer`: Shared strategy interface for question scoring (`score(userAnswer, correctAnswer)`).
- `com.ieltsplatform.common.ports.ILlmClient`: Gemini API adapter port.
- `com.ieltsplatform.modules.flashcard.ports.IFlashcardService`: Universal flashcard extraction service port.

---

## 4. Domain Models & Specifications

### `PracticeExercise.java`
- **Extends**: `BaseEntity`
- **Fields**:
  - `moduleType`: `ModuleType` enum (`WRITING`, `READING`, `LISTENING`, `GRAMMAR`, `VOCABULARY`)
  - `skill`: `SkillType` enum (`MATCHING_HEADINGS`, `TRUE_FALSE_NOT_GIVEN`, `TASK1_OVERVIEW`, `TASK2_THESIS`, `FILL_BLANKS`)
  - `cefrLevel`: `CEFRLevel` enum (`A1`, `A2`, `B1`, `B2`, `C1`, `C2`)
  - `title`: `String`
  - `contentJson`: `String` (Question text, passage snippet, options, or prompts)
  - `rubricJson`: `String` (Correct answers, explanation text, or AI grading prompt criteria)

### `PracticeAttempt.java`
- **Extends**: `BaseEntity`
- **Fields**:
  - `userId`: `UUID`
  - `exerciseId`: `UUID`
  - `userAnswer`: `String` (JSON or raw string answer submission)
  - `score`: `Double` (Percentage or raw band score $0.0 - 100.0$)
  - `feedbackJson`: `String` (Detailed breakdown, text-span evidence highlights, or AI paragraph evaluation)

### `PracticeMapper.java` / `AssessmentMapper.java`
- Static utility conversion methods:
  - `toExerciseResponse(PracticeExercise entity)` -> `PracticeExerciseResponse`
  - `toAttemptResponse(PracticeAttempt entity)` -> `PracticeAttemptResponse`

### `WritingParagraphScoreRequest.java`
- **Fields**:
  - `exerciseId`: `UUID`
  - `draftText`: `String`

### `WritingParagraphScoreResponse.java`
- **Fields**:
  - `score`: `Double`
  - `grammarFixes`: `List<String>`
  - `suggestion`: `String`

### `PracticeSubmissionRequest.java`
- **Fields**:
  - `exerciseId`: `UUID`
  - `userAnswer`: `String`

### `PracticeAttemptResponse.java`
- **Fields**:
  - `attemptId`: `UUID`
  - `score`: `Double`
  - `feedbackJson`: `String`

---

## 5. Step-by-Step User Flows

### Flow A: Isolated Objective Drill with Zero-Duplication Scoring Strategy (Pattern 2)
1. **User Action:** Selects *"Matching Headings Only"* drill at `/practice/reading/drills`.
2. **API Request:** `GET /api/practice/exercises?skill=MATCHING_HEADINGS&level=B2`.
3. **Exercise Rendering:** Renders 5 isolated matching heading questions.
4. **Submission:** User selects answers and clicks **"Submit Practice"**.
5. **API Request:** `POST /api/practice/attempts` with `PracticeSubmissionRequest`.
6. **Backend Processing (`PracticeServiceImpl`):**
   - Fetches `PracticeExercise` entity from `PracticeExerciseRepository`.
   - Resolves `IAnswerScorer` strategy from `AnswerScorerRegistry` by `SkillType.MATCHING_HEADINGS` (`McqScorerImpl` / `TfNgScorerImpl` / `FillBlankScorerImpl`).
   - Evaluates each answer without code duplication, generating score and evidence span highlights.
   - Saves `PracticeAttempt` entity (`userId`, `exerciseId`, `userAnswer`, `score`, `feedbackJson`).
7. **Response:** Renders `PracticeAttemptResponse` with percentage score and evidence highlights.

### Flow B: Task 1 Paragraph Writing Drill with Structured AI Feedback (Pattern 3)
1. **User Action:** Selects Task 1 Overview Description Drill.
2. **Exercise Rendering:** Displays a sample bar chart image and prompt asking for a 3-sentence overview paragraph.
3. **User Action:** Types overview paragraph and clicks **"Check Paragraph with AI"**.
4. **API Request:** `POST /api/practice/writing/score` with `WritingParagraphScoreRequest` (`exerciseId`, `draftText`).
5. **Backend Processing (`PracticeServiceImpl`):**
   - Builds prompt using `LlmPromptTemplate` injected with `draftText` and exercise model answer rubric.
   - Invokes `ILlmClient.generate(prompt)` (Google Gemini API) to evaluate Task Overview clarity, cohesive devices, and grammatical accuracy.
   - Parses structured JSON response into `WritingParagraphScoreResponse` (score, grammar corrections, Band 9 model comparison).
   - Persists submission as `PracticeAttempt` entity extending `BaseEntity`.
6. **Response:** Displays instant paragraph feedback, highlighted grammar fixes, and side-by-side comparison with a Band 9 model overview.

### Flow C: Drill Vocabulary Extraction to Flashcard Deck (Pattern 4)
1. **User Action:** While attempting a reading or vocabulary drill, user highlights an unfamiliar term (e.g. *"fluctuated dramatically"*).
2. **UI Action:** Clicks popover button **"Add to Flashcards"**.
3. **API Request:** `POST /api/flashcards` with payload `{ "word": "fluctuated dramatically", "sourceTag": "QUIZ", "sourceEntityId": "exercise-uuid" }`.
4. **Backend Processing:** Delegates to `IFlashcardService.addWord(userId, "fluctuated dramatically", SourceTag.QUIZ, exerciseId)`.

---

## 6. Visual Architecture & Sequence Diagrams

### Diagram 10.1: Interactive Training Drills Execution (Flowchart)

```mermaid
graph TD
    classDef client fill:#2B6CB0,stroke:#1A365D,color:#FFFFFF
    classDef backend fill:#2F855A,stroke:#1C4532,color:#FFFFFF
    classDef storage fill:#6B46C1,stroke:#3C366B,color:#FFFFFF

    Start(["User Opens Skill Practice Module"]) --> DrillChoice{Choose Drill Category}
    
    DrillChoice -- "Writing Drills" --> WriteDrill["Task 1 Overview / Sentence Building"]
    DrillChoice -- "Reading Drills" --> ReadDrill["Isolated Question Type / Speed WPM"]
    DrillChoice -- "Grammar Quizzes" --> GramDrill["CEFR Interactive Quiz A1-C2"]

    WriteDrill --> SubmitWrite["Draft Paragraph & Submit"]
    SubmitWrite --> ReqAiScore["POST /api/practice/writing/score"]
    ReqAiScore --> RenderAiFeedback["Display Instant Paragraph Score & Model Compare (Pattern 3)"]

    ReadDrill --> SubmitRead["Complete 5 Isolated Questions"]
    SubmitRead --> ReqReadScore["POST /api/practice/attempts"]
    ReqReadScore --> DispatchScorer["AnswerScorerRegistry Strategy Dispatch (Pattern 2)"]
    DispatchScorer --> RenderEvidence["Render Text Span Evidence Highlights"]

    GramDrill --> SubmitGram["Submit Quiz Answers"]
    SubmitGram --> CheckGram["Check Rule Explanations & Save Accuracy %"]
    CheckGram --> LinkKb["Link Incorrect Questions to Reference Library Guide"]

    RenderEvidence --> HighlightVocab["Highlight Text -> Click Add to Flashcards"]
    HighlightVocab --> AddFlashcard["POST /api/flashcards (SourceTag.QUIZ)"]

    class Start,DrillChoice,WriteDrill,ReadDrill,GramDrill,SubmitWrite,RenderAiFeedback,SubmitRead,RenderEvidence,SubmitGram,LinkKb,HighlightVocab client;
    class ReqAiScore,ReqReadScore,DispatchScorer,CheckGram,AddFlashcard backend;
```

---

### Diagram 10.2: Practice Drill Attempt & Feedback Pipeline (Sequence Diagram)

```mermaid
sequenceDiagram
    autonumber
    participant User as "User (Browser)"
    participant Client as "Next.js Drill Component"
    participant Controller as "PracticeController"
    participant Service as "IPracticeService (PracticeServiceImpl)"
    participant Registry as "AnswerScorerRegistry"
    participant LlmClient as "ILlmClient (Gemini API)"
    participant DB as "PostgreSQL DB"

    alt Flow A: "Objective Drill (Pattern 2)"
        User->>Client: "Submit Answers for 5 Matching Heading Questions"
        Client->>Controller: "POST /api/practice/attempts (PracticeSubmissionRequest)"
        Controller->>Service: "submitAttempt(userId, request)"
        Service->>Registry: "getScorer(SkillType.MATCHING_HEADINGS)"
        Registry-->>Service: "Return IAnswerScorer (McqScorerImpl / TfNgScorerImpl)"
        Service->>Service: "Evaluate Answers with Zero Code Duplication"
        Service->>DB: "Save PracticeAttempt Entity (extends BaseEntity)"
        Service-->>Controller: "Return PracticeAttemptResponse DTO"
        Controller-->>Client: "200 OK (PracticeAttemptResponse + Evidence Spans)"
        Client->>User: "Render Score & Explanatory Evidence Spans"
    else Flow B: "Writing Paragraph Drill (Pattern 3)"
        User->>Client: "Submit Task 1 Overview Paragraph"
        Client->>Controller: "POST /api/practice/writing/score (WritingParagraphScoreRequest)"
        Controller->>Service: "scoreWritingParagraph(userId, request)"
        Service->>DB: "Fetch PracticeExercise (Model Answer & Rubric)"
        Service->>LlmClient: "generate(LlmPromptTemplate formatted prompt)"
        LlmClient-->>Service: "Return JSON { score, grammarFixes, suggestion }"
        Service->>DB: "Save PracticeAttempt Entity (extends BaseEntity)"
        Service-->>Controller: "Return WritingParagraphScoreResponse DTO"
        Controller-->>Client: "200 OK (WritingParagraphScoreResponse)"
        Client->>User: "Render Score, Fixes & Model Answer Comparison"
    end
```

---

### Diagram 10.3: Practice & Drill Scoring Domain Model (UML Class Diagram)

```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        +UUID id
        +Instant createdAt
        +Instant updatedAt
        +Long version
    }

    class IAnswerScorer {
        <<interface>>
        +score(userAnswer, correctAnswer) Double
    }

    class AnswerScorerRegistry {
        -Map~SkillType,IAnswerScorer~ scorers
        +getScorer(SkillType type) IAnswerScorer
    }

    class McqScorerImpl {
        +score(userAnswer, correctAnswer) Double
    }

    class TfNgScorerImpl {
        +score(userAnswer, correctAnswer) Double
    }

    class FillBlankScorerImpl {
        +score(userAnswer, correctAnswer) Double
    }

    class PracticeExercise {
        +ModuleType moduleType
        +SkillType skill
        +CEFRLevel cefrLevel
        +String title
        +String contentJson
        +String rubricJson
    }

    class PracticeAttempt {
        +UUID userId
        +UUID exerciseId
        +String userAnswer
        +Double score
        +String feedbackJson
    }

    BaseEntity <|-- PracticeExercise
    BaseEntity <|-- PracticeAttempt
    IAnswerScorer <|.. McqScorerImpl
    IAnswerScorer <|.. TfNgScorerImpl
    IAnswerScorer <|.. FillBlankScorerImpl
    AnswerScorerRegistry o-- IAnswerScorer
```
