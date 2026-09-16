# 📘 IELTS Platform — Engineering & Git Programming Guidelines

> **Target Audience:** Engineering Team (Person A: Fresher & Person B: Intern)  
> **Purpose:** A practical, daily handbook covering Clean Code Architecture, Dependency Rules, Git Branching, Daily Workflows, and a Quick-Lookup Cheat Sheet.

---

## 📑 Table of Contents
1. [Clean Code & Architecture Standards](#1-clean-code--architecture-standards)
2. [Git Branching & Commit Conventions](#2-git-branching--commit-conventions)
3. [Daily Engineering Workflows ("What Do I Do When...")](#3-daily-engineering-workflows)
   - [Every Day Before Writing Code](#scenario-1-every-day-before-writing-code)
   - [When Adding a New Feature](#scenario-2-when-adding-a-new-feature)
   - [When Fixing a Bug](#scenario-3-when-fixing-a-bug)
   - [When You Have Uncommitted Changes & Need to Switch Branches](#scenario-4-when-you-have-uncommitted-changes)
   - [When You Encounter Git Merge Conflicts](#scenario-5-when-encountering-merge-conflicts)
4. [Testing & Verification Standards](#4-testing--verification-standards)
5. [⚡ Quick Reference Cheat Sheet](#5--quick-reference-cheat-sheet)

---

## 1. Clean Code & Architecture Standards

We build a **Modular Monolith** using **Domain-Driven Design (DDD)** and **Clean Architecture**. Every module is an independent vertical slice.

```text
com.ieltsplatform.modules.<module_name>/
├── entities/       # Database tables (JPA @Entity) — MUST extend BaseEntity
├── repository/     # Spring Data JPA interfaces (extends JpaRepository)
├── dtos/
│   ├── request/    # Incoming client payloads with Jakarta Validation annotations
│   └── response/   # Outgoing client payloads (NEVER return entities directly!)
├── mapper/         # Static utility methods converting Entity <-> DTO
├── ports/          # Java interfaces defining service contracts (e.g. IContentService)
├── services/impl/  # Concrete Spring @Service implementing the port interface
└── controllers/    # REST @RestController exposing HTTP endpoints
```

### 💎 Golden Architectural Rules

#### Rule 1: Dependency Inversion (Controller → Port Interface, NEVER Impl)
Controllers must **never** import or inject a service implementation class directly. Always inject the interface port.
* ❌ **Bad:**
  ```java
  @Autowired
  private ContentServiceImpl contentService; // Violates Dependency Inversion!
  ```
* ✅ **Good:**
  ```java
  private final IContentService contentService; // Depends on interface contract

  public ContentController(IContentService contentService) {
      this.contentService = contentService;
  }
  ```

#### Rule 2: Never Expose JPA Entities Over HTTP
JPA entities represent database tables. Exposing them in controllers leaks internal database structure, causes lazy loading exceptions (`LazyInitializationException`), and creates security holes (e.g. returning `passwordHash`).
* ❌ **Bad:** `public ResponseEntity<User> getProfile() { ... }`
* ✅ **Good:** `public ResponseEntity<UserProfileResponse> getProfile() { ... }`
* Always use static mapper methods in `mapper/` (e.g. `UserMapper.toResponse(user)`).

#### Rule 3: All Entities Must Extend `BaseEntity`
Every entity in the system inherits UUID primary key, auditing timestamps, and optimistic locking version counters.
* ✅ **Good:**
  ```java
  @Entity
  @Table(name = "reading_passages")
  public class ReadingPassage extends BaseEntity {
      // id, createdAt, updatedAt, and version are already inherited!
      private String title;
  }
  ```

#### Rule 4: Throw Specific Domain Exceptions (Never Swallow Errors)
Never write empty catch blocks (`catch (Exception e) {}`). Always throw a meaningful `DomainException` and let `GlobalExceptionHandler` translate it to a clean HTTP response.
* ❌ **Bad:**
  ```java
  try {
      return repo.findById(id).get();
  } catch (Exception e) {
      return null; // Silent bug!
  }
  ```
* ✅ **Good:**
  ```java
  return repo.findById(id)
             .orElseThrow(() -> new ResourceNotFoundException("Passage not found: " + id));
  ```

#### Rule 5: Package Isolation (Zero Cross-Module Pollution)
* **Person A (Fresher):** Owns `modules/auth/`, `modules/user/`, `modules/assessment/`, and `modules/writing/`.
* **Person B (Intern):** Owns `modules/content/`, `modules/flashcard/`, and `modules/dictation/`.
* **Rule:** You never create or modify code inside the other person's module. If you need their data, call their public `ports/` interface or discuss in standup.

---

## 2. Git Branching & Commit Conventions

### 🌿 Branch Naming Strategy

The `main` branch is protected and always deployable. All work happens in short-lived feature branches:

| Type | Format | Example |
| :--- | :--- | :--- |
| **Feature (Person A)** | `feature/person-a/<short-name>` | `feature/person-a/auth-jwt` |
| **Feature (Person B)** | `feature/person-b/<short-name>` | `feature/person-b/reading-passages-crud` |
| **Bug Fix** | `fix/person-<a/b>/<bug-name>` | `fix/person-b/sm2-ease-factor-floor` |
| **Refactoring** | `refactor/person-<a/b>/<scope>` | `refactor/person-a/exception-handling` |

> [!IMPORTANT]
> Never push directly to `main`! Always develop on your feature branch and merge via Pull Request.

---

### 📝 Commit Message Convention (Conventional Commits)

Format: `<type>(<scope>): <short imperative description>`

#### Permitted Types:
* `feat`: A new feature (e.g., `feat(auth): implement refresh token rotation`)
* `fix`: A bug fix (e.g., `fix(assessment): prevent submission after timer expires`)
* `test`: Adding or updating automated tests (e.g., `test(flashcard): add SM-2 boundary test cases`)
* `refactor`: Code change that neither fixes a bug nor adds a feature (e.g., `refactor(content): extract word count calculation into mapper`)
* `docs`: Documentation only changes (e.g., `docs: update 12-week task plan`)
* `chore`: Build tooling, dependencies, or `.gitignore` (e.g., `chore: bump spring boot to 3.4.4`)

#### Good vs. Bad Commit Messages:
* ❌ `git commit -m "fix stuff"` *(Vague, uninformative)*
* ❌ `git commit -m "update code and fixed bug in controller"` *(Multiple changes bundled)*
* ✅ `git commit -m "feat(auth): add BCrypt password hashing on user signup"`
* ✅ `git commit -m "test(sm2): assert ease factor never drops below 1.3"`

---

## 3. Daily Engineering Workflows

### Scenario 1: Every Day Before Writing Code
Before writing a single line of code in the morning, always sync your local repository with `main` to prevent drift:

```bash
# 1. Switch to main
git checkout main

# 2. Pull the latest code merged by your teammate
git pull origin main

# 3. Verify that the fresh main branch compiles and passes tests
mvn clean test

# 4. Create your new branch for today's task
git checkout -b feature/person-a/auth-endpoints
```

---

### Scenario 2: When Adding a New Feature
Follow the strict **Red-Green-Refactor** development cycle:

```bash
# Step 1: Create your branch from updated main
git checkout main
git pull origin main
git checkout -b feature/person-b/dictation-levenshtein

# Step 2: Implement your changes in your module
# 1. Create Entity & Repository
# 2. Create DTOs & Mapper
# 3. Create Service Interface (ports/) & Implementation (services/impl/)
# 4. Write Unit Tests (Verify business logic)
# 5. Create Controller & MockMvc Tests (Verify HTTP endpoints)

# Step 3: Run the local test suite to ensure 100% pass rate
mvn clean test

# Step 4: Check what you modified
git status
git diff

# Step 5: Stage only your relevant files (don't blindly do git add . if untracked junk exists)
git add src/main/java/com/ieltsplatform/modules/dictation/
git add src/test/java/com/ieltsplatform/modules/dictation/

# Step 6: Commit with conventional commit message
git commit -m "feat(dictation): add Levenshtein diff engine and accuracy scoring"

# Step 7: Push your branch to GitHub
git push -u origin feature/person-b/dictation-levenshtein

# Step 8: Open a Pull Request (PR) on GitHub for your teammate to review
```

---

### Scenario 3: When Fixing a Bug
Always write a reproducing test **first** before touching the implementation:

1. **Reproduce:** Write a test that fails due to the bug (e.g. `testSm2Scheduler_QualityZero_ShouldResetInterval()`).
2. **Fix:** Modify the minimal implementation code necessary to make that test pass.
3. **Verify:** Run `mvn test` to ensure the bug is fixed and no other tests broke.
4. **Commit:**
   ```bash
   git commit -m "fix(flashcard): reset SM-2 interval to 1 day on recall failure"
   ```

---

### Scenario 4: When You Have Uncommitted Changes
You are halfway through coding a feature, but you need to switch branches immediately (e.g. to test your teammate's PR or inspect something on `main`):

> [!CAUTION]
> Never do `git checkout main` with dirty uncommitted work. Git will either block you or accidentally carry over uncommitted edits!

**The `git stash` solution:**
```bash
# 1. Stash your unfinished work with a clear label
git stash save "WIP: halfway through reading passage question parser"

# 2. Your working directory is now completely clean!
git status

# 3. Switch to main or another branch safely
git checkout main
git pull origin main

# 4. When you're ready to resume your work, switch back to your branch:
git checkout feature/person-b/reading-passages

# 5. Restore your stashed changes:
git stash pop
```

---

### Scenario 5: When Encountering Merge Conflicts
If your teammate merged changes into `main` while you were working on your branch, sync your branch cleanly using `rebase`:

```bash
# 1. Fetch latest changes from remote
git fetch origin

# 2. Rebase your current feature branch on top of latest origin/main
git rebase origin/main

# If Git pauses and reports conflicts in a file:
# 3. Open the conflicted file in your IDE.
# Look for <<<<<<< HEAD, =======, >>>>>>> markers and choose the correct code.

# 4. Once resolved, stage the resolved file:
git add <resolved-file-path>

# 5. Continue the rebase:
git rebase --continue

# 6. Verify everything compiles and tests pass:
mvn clean test
```

---

## 4. Testing & Verification Standards

### ⚖️ The Testing Rule: JUnit 5 + Mockito
* **JUnit 5 (`org.junit.jupiter.api.*`)**: Used for every test class. Defines test cases (`@Test`), setups (`@BeforeEach`), and assertions (`assertEquals`, `assertTrue`, `assertThrows`).
* **Mockito (`org.mockito.*`)**: Used to fake external dependencies (databases, external APIs) so unit tests stay fast and isolated.

### What Must Be Tested in Every Pull Request:
1. **Algorithms & Calculators (100% Pure Unit Tests):**
   - SM-2 Spaced Repetition (`Sm2SchedulerTest`)
   - Levenshtein Diff Scorer (`LevenshteinDictationScorerTest`)
   - IELTS Band Score Converter (`IeltsBandScoreConverterTest`)
   - *Requirement:* Test boundary conditions (0, 5, negative, null, empty strings).
2. **Business Services (Mockito Unit Tests):**
   - Mock all repository and external client calls (`@Mock private UserRepository userRepo;`).
   - Test both success cases and expected exceptions (e.g. duplicate email throws 409).
3. **REST Controllers (MockMvc Integration Tests):**
   - Verify HTTP status codes (`status().isOk()`, `status().isCreated()`, `status().isNotFound()`).
   - Verify JSON output structure with `jsonPath("$.field").value(...)`.

---

## 5. ⚡ Quick Reference Cheat Sheet

Print this or keep it open in a split pane while coding:

### 🛠️ Daily Maven & Build Commands
| Task | Command |
| :--- | :--- |
| **Compile without running tests** | `mvn clean compile` |
| **Run all automated tests** | `mvn clean test` |
| **Run a single test class** | `mvn test -Dtest=AuthServiceTest` |
| **Run a specific test method** | `mvn test -Dtest=AuthServiceTest#login_WithValidCredentials_ReturnsTokens` |
| **Package runnable JAR** | `mvn clean package` |
| **Start Spring Boot server** | `mvn spring-boot:run` |

---

### 🌿 Essential Git Commands
| Action | Command |
| :--- | :--- |
| **Check current branch & status** | `git status` |
| **View compact status** | `git status -s` |
| **View exact line changes** | `git diff` |
| **Discard local changes in a file** | `git restore <file-path>` |
| **Unstage a file from git add** | `git restore --staged <file-path>` |
| **Create & switch to new branch** | `git checkout -b <branch-name>` |
| **Switch to existing branch** | `git checkout <branch-name>` |
| **Pull latest code on current branch** | `git pull origin <branch-name>` |
| **Stage specific files** | `git add <file1> <file2>` |
| **Commit with message** | `git commit -m "feat(scope): message"` |
| **Push new branch to remote** | `git push -u origin <branch-name>` |
| **Save unfinished work to stash** | `git stash save "message"` |
| **List all stashes** | `git stash list` |
| **Restore most recent stash** | `git stash pop` |
| **View last 5 commits on one line** | `git log -n 5 --oneline` |

---

### 🐳 Docker & Infrastructure Commands
| Action | Command |
| :--- | :--- |
| **Start Postgres & Redis in background** | `docker compose up -d` |
| **Check container health status** | `docker compose ps` |
| **View real-time database logs** | `docker compose logs -f postgres` |
| **Stop containers without deleting data** | `docker compose stop` |
| **Stop and remove containers** | `docker compose down` |
| **Connect to PostgreSQL CLI** | `docker exec -it ielts-postgres psql -U postgres -d ielts_db` |
| **Connect to Redis CLI** | `docker exec -it ielts-redis redis-cli` |

---

### 🌐 Key Local URLs
* **Backend Health Check:** [http://localhost:8080/api/health](http://localhost:8080/api/health)
* **Swagger OpenAPI Documentation:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **Next.js Frontend (from Week 10):** [http://localhost:3000](http://localhost:3000)
