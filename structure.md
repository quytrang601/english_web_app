# 🚀 IELTS Platform Fullstack Monorepo Architecture (Maven)

Hệ thống Web App luyện thi IELTS chuyên nghiệp được thiết kế theo kiến trúc **Modular Monolith** kết hợp **Clean Architecture / Domain-Driven Design (DDD)**, sử dụng **Maven** làm công cụ quản lý dự án.

---

## 📐 Cấu trúc Thư mục Hệ thống Đầy đủ (100% Directory Tree)

```text
english_web_app/                                    # ROOT MONOREPO REPOSITORY
├── docs/                                           # Tài liệu dự án & Đặc tả sản phẩm
│   ├── product-specifications/                     # Đặc tả sản phẩm hợp nhất
│   │   ├── feature-flows/                          # 11 Tài liệu luồng tính năng chi tiết (01 - 11)
│   │   ├── pricing-and-gamification/               # 13 Tài liệu định giá & Gamification (00 - 12)
│   │   └── feature-list.md                         # Bảng tổng hợp tính năng & công nghệ
│   └── er-diagram.md                               # Sơ đồ quan hệ thực thể DBML/Mermaid
│
├── frontend/                                       # Project Client (React 19 / Next.js 16)
│
├── README.md                                       # Tài liệu hướng dẫn chạy & tổng quan dự án
├── structure.md                                    # Tài liệu kiến trúc chi tiết (File này)
│
└── backend/                                        # PROJECT SPRING BOOT BACKEND (MAVEN)
    ├── pom.xml                                     # Quản lý Dependencies & Plugins (Maven 3.9+)
    ├── task-mvp.md                                 # Kế hoạch phát triển MVP 12 tuần (Person A & Person B)
    ├── programming-guidelines.md                   # Hướng dẫn Clean Code, Git Workflow & Cheat Sheet
    └── src/
        ├── main/
        │   ├── java/com/ieltsplatform/
        │   │   │
        │   │   ├── IeltsPlatformApplication.java   # Spring Boot Entrypoint (@SpringBootApplication)
        │   │   │
        │   │   ├── common/                         # SHARED KERNEL (Hạ tầng kỹ thuật chung)
        │   │   │   ├── base/                       # BaseEntity (Base Entity metadata: id, createdAt, updatedAt — TẤT CẢ Entity đều kế thừa)
        │   │   │   ├── config/                     # SecurityConfig, RedisConfig, OpenApiConfig, JpaAuditConfig
        │   │   │   ├── event/                      # DomainEventPublisher (Truyền tin liên module)
        │   │   │   ├── exception/                  # GlobalExceptionHandler (Hứng & chuẩn hóa lỗi HTTP)
        │   │   │   ├── infrastructure/             # External Adapters & Local Mocks
        │   │   │   │   ├── email/mocks/            # Giả lập Email Sender
        │   │   │   │   ├── llm/mocks/              # Giả lập AI Client (Claude/OpenAI)
        │   │   │   │   ├── media/mocks/            # Giả lập S3 Storage & Text-to-Speech (TTS)
        │   │   │   │   ├── payment/mocks/          # Giả lập Cổng thanh toán Stripe
        │   │   │   │   └── stt/mocks/              # Giả lập Whisper Speech-to-Text
        │   │   │   └── security/                   # JwtAuthFilter, RateLimitFilter, AuditorAwareImpl
        │   │   │
        │   │   └── modules/                        # BOUNDED CONTEXTS (11 Module nghiệp vụ)
        │   │       │
        │   │       ├── auth/                       # [1 - Person A] Đăng ký, Đăng nhập & Session Token
        │   │       │   ├── api/dto/                # SignupRequest, LoginRequest, AuthTokenResponse
        │   │       │   ├── application/            # AuthService
        │   │       │   ├── domain/model/           # RefreshToken Entity
        │   │       │   ├── domain/repository/      # RefreshTokenRepository Port
        │   │       │   └── infrastructure/persistence/
        │   │       │
        │   │       ├── user/                       # [2 - Person A] Thông tin cá nhân, Phân quyền (FREE/PAID)
        │   │       │   ├── api/{dto/, mapper/}     # UserController, UserMapper
        │   │       │   ├── application/            # UserService, PasswordResetService
        │   │       │   ├── domain/model/           # User Entity, UserRole Enum
        │   │       │   ├── domain/repository/      # UserRepository Port
        │   │       │   └── infrastructure/persistence/
        │   │       │
        │   │       ├── content/                    # [3 - Person B Task] Quản lý bài đọc Reading
        │   │       │   ├── api/{dto/, mapper/}     # ContentController, ContentMapper
        │   │       │   ├── application/            # ContentService (CRUD bài đọc theo Level)
        │   │       │   ├── domain/model/           # ReadingPassage Entity
        │   │       │   ├── domain/repository/      # ReadingPassageRepository Port
        │   │       │   └── infrastructure/persistence/
        │   │       │
        │   │       ├── listening/                  # [4 - Person B] Quản lý bài nghe Listening & Audio Tracks
        │   │       │   ├── api/{dto/, mapper/}     # ListeningController
        │   │       │   ├── application/            # ListeningService (TTS & S3 storage)
        │   │       │   ├── domain/model/           # ListeningSection Entity
        │   │       │   ├── domain/repository/      # ListeningSectionRepository Port
        │   │       │   └── infrastructure/persistence/
        │   │       │
        │   │       ├── writing/                    # [5 - Person B] Nộp bài luận & Chấm điểm bằng AI
        │   │       │   ├── api/{dto/, mapper/}     # WritingController
        │   │       │   ├── application/            # WritingCheckService
        │   │       │   ├── domain/model/           # EssaySubmission, EssayFeedback
        │   │       │   ├── domain/repository/      # EssaySubmissionRepository Port
        │   │       │   └── infrastructure/persistence/
        │   │       │
        │   │       ├── speaking/                   # [6 - Person B] Phòng luyện nói phản xạ & Chấm phát âm AI
        │   │       │   ├── api/{dto/, mapper/}     # SpeakingController
        │   │       │   ├── application/            # SpeakingService (STT + AI Scoring)
        │   │       │   ├── domain/model/           # SpeakingSession Entity
        │   │       │   ├── domain/repository/      # SpeakingSessionRepository Port
        │   │       │   └── infrastructure/persistence/
        │   │       │
        │   │       ├── flashcard/                  # [7 - Person B] Học từ vựng lặp lại ngắt quãng (SM-2)
        │   │       │   ├── api/dto/                # FlashcardController
        │   │       │   ├── application/            # FlashcardService
        │   │       │   ├── domain/model/           # Flashcard, FlashcardReview Entities
        │   │       │   ├── domain/repository/      # FlashcardRepository Port
        │   │       │   ├── domain/service/         # SpacedRepetitionScheduler (Pure SM-2)
        │   │       │   └── infrastructure/persistence/
        │   │       │
        │   │       ├── assessment/                 # [8 - Person B] Động cơ thi thử & Chấm trắc nghiệm
        │   │       │   ├── api/dto/                # AssessmentController
        │   │       │   ├── application/            # AssessmentService (Test Timer)
        │   │       │   ├── domain/model/           # TestAttempt, TestAnswer Entities
        │   │       │   ├── domain/repository/      # TestAttemptRepository Port
        │   │       │   ├── domain/service/         # ScoringEngine (Trắc nghiệm MCQ)
        │   │       │   └── infrastructure/persistence/
        │   │       │
        │   │       ├── dictation/                  # [9 - Person A] Nghe chép chính tả từng từ
        │   │       │   ├── api/{dto/, mapper/}     # DictationController
        │   │       │   ├── application/            # DictationService
        │   │       │   ├── domain/model/           # DictationItem Entity
        │   │       │   ├── domain/repository/      # DictationItemRepository Port
        │   │       │   ├── domain/service/         # DictationScorer (Levenshtein Diff)
        │   │       │   └── infrastructure/persistence/
        │   │       │
        │   │       ├── essaybank/                  # [10] Thư viện bài luận mẫu Band 8.0+
        │   │       │   ├── api/{dto/, mapper/}     # EssayBankController
        │   │       │   ├── application/            # EssayBankService
        │   │       │   ├── domain/model/           # SampleEssay Entity
        │   │       │   ├── domain/repository/      # SampleEssayRepository Port
        │   │       │   └── infrastructure/persistence/
        │   │       │
        │   │       └── subscription/               # [11] Thanh toán gói & Giới hạn lượt dùng
        │   │           ├── api/{dto/, mapper/}     # SubscriptionController, WebhookController
        │   │           ├── application/            # SubscriptionService, UsageLimitService
        │   │           ├── domain/model/           # Subscription Entity
        │   │           ├── domain/repository/      # SubscriptionRepository Port
        │   │           └── infrastructure/persistence/
        │   │
        │   └── resources/
        │       ├── application.yml                 # Cấu hình Spring Boot
        │       └── db/migration/                   # Flyway Database Migration SQL Scripts
        │           ├── V1__init_users.sql
        │           ├── V2__init_content.sql        # [Tạo bảng reading_passages của bạn]
        │           ├── V3__init_flashcards.sql
        │           └── ...