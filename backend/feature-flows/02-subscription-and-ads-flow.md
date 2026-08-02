# User Flow & Technical Specifications: Subscriptions & Ad Monetization

**Module Scope:** Requirements FR-2.1 through FR-2.5  
**Target Path:** `backend/feature-flows/02-subscription-and-ads-flow.md`

---

## 1. Executive Summary & Architecture Alignment

The Subscription & Ads module enforces the dual monetization model across the IELTS platform:
1. **Free Tier**: Non-paying users encounter AdSense ad placements on non-test UI routes and have daily attempt limits enforced via atomic Redis counters intercepted by `RateLimitFilter`.
2. **Paid Tier**: Paid subscribers experience zero ads and receive unlimited AI evaluations and test attempts. Subscriptions are created via Stripe Checkout and kept in sync via transactional Stripe webhooks with Redis idempotency checks.

This module strictly adheres to the 7-folder layout (`entities/`, `repository/`, `dtos/`, `mapper/`, `ports/`, `services/impl/`, `controllers/`), Dependency Inversion (`SubscriptionController` depends strictly on interface port `ISubscriptionService`), and explicit static DTO mapping via `SubscriptionMapper`. `Subscription` entity extends `com.ieltsplatform.common.base.BaseEntity`.

---

## 2. Module Structure & Component Layout

### Subscription Module (`com.ieltsplatform.modules.subscription`)
- `entities/`: `Subscription.java` (extends `BaseEntity`)
- `repository/`: `SubscriptionRepository.java`
- `dtos/`: `CheckoutSessionRequest.java`, `CheckoutSessionResponse.java`, `SubscriptionResponse.java`
- `mapper/`: `SubscriptionMapper.java` (static entity to `SubscriptionResponse` DTO mapping)
- `ports/`: `ISubscriptionService.java`
- `services/impl/`: `SubscriptionServiceImpl.java` (Stripe API calls, Redis `event_id` idempotency check, webhook signature validation, updating `User.role` to `PAID`/`FREE`, invalidating Redis usage keys)
- `controllers/`: `SubscriptionController.java` (depends strictly on `ISubscriptionService`), `StripeWebhookController.java`

### Shared Kernel Interceptor
- `RateLimitFilter` (`common/services/impl/RateLimitFilter.java`): Intercepts requests for `FREE` tier users using atomic Redis `INCR` or Lua script on key `usage:{userId}:{date}` to prevent race conditions during concurrent requests.

---

## 3. Step-by-Step User Flows

### Flow A: Upgrade to Paid Tier & Stripe Webhook Lifecycle Handling
1. **User Action:** Clicks "Upgrade to Pro" on pricing page (`/pricing`).
2. **API Request:** `POST /api/subscriptions/checkout-session` with `CheckoutSessionRequest` DTO.
3. **Backend Processing:**
   - `SubscriptionController` receives request and delegates to `ISubscriptionService.createCheckoutSession(userId, request)`.
   - `SubscriptionServiceImpl` calls Stripe API (`Stripe.checkout.Session.create()`).
   - Sets success URL (`/dashboard?subscription=success`) and cancel URL (`/pricing`).
   - Returns `CheckoutSessionResponse` containing Checkout Session ID and Stripe hosted URL mapped via `SubscriptionMapper`.
4. **User Action:** Redirects to Stripe hosted page, completes payment.
5. **Stripe Webhook Processing & Idempotency Check:**
   - Stripe sends event payload to `POST /webhooks/stripe`.
   - `StripeWebhookController` delegates payload and signature header to `ISubscriptionService.handleWebhook(payload, sigHeader)`.
   - **Redis `event_id` Idempotency Check:** Backend checks Redis key `stripe:event:{event_id}` via `SETNX` (with 24-hour TTL). If the event has already been processed, returns `200 OK` immediately to prevent duplicate webhook processing.
   - Backend verifies Stripe signature (`Webhook.constructEvent()`).
   - **Event-Specific Handlers:**
     - `customer.subscription.created` / `customer.subscription.updated` (active status): `SubscriptionServiceImpl` updates `User.role = PAID`, inserts/updates `Subscription` entity extending `BaseEntity` (`stripeCustomerId`, `stripeSubscriptionId`, `status = ACTIVE`, `currentPeriodEnd`), and invalidates Redis daily usage key `usage:{userId}:{date}`.
     - `customer.subscription.deleted`: `SubscriptionServiceImpl` updates `Subscription.status = CANCELED` and reverts `User.role = FREE`.
     - `invoice.payment_failed`: `SubscriptionServiceImpl` updates `Subscription.status = PAST_DUE` and reverts `User.role = FREE`.

### Flow B: Daily Usage Cap Enforcement for Free Tier (Atomic Execution)
1. **User Action:** User attempts to launch a new mock test or AI essay evaluation.
2. **Backend Processing:**
   - `RateLimitFilter` intercepts incoming request before controller execution.
   - Inspects user role from Security Context. If `UserRole.PAID`, bypasses counter check.
   - If `UserRole.FREE`, executes atomic Redis `INCR` or Lua script against key `usage:{userId}:{date}`:
     - Atomically increments key value.
     - On initial key creation (increment value == 1), sets key TTL to expire at midnight (or 24 hours).
     - If returned increment value > 3 (daily cap), throws `DomainException("DAILY_LIMIT_REACHED")` mapped by `GlobalExceptionHandler` to `403 Forbidden`.
     - If returned increment value <= 3, allows request execution to proceed.

---

## 4. Visual Architecture & Sequence Diagrams

### Diagram 2.1: Free vs. Paid Feature Gating & Ad Placement (Flowchart)

```mermaid
graph TD
    classDef client fill:#2B6CB0,stroke:#1A365D,color:#FFFFFF
    classDef backend fill:#2F855A,stroke:#1C4532,color:#FFFFFF
    classDef external fill:#C05621,stroke:#7B341E,color:#FFFFFF
    classDef redis fill:#6B46C1,stroke:#3C366B,color:#FFFFFF

    Start([User Navigates Page]) --> FetchUser["Fetch User Profile & Subscription Role"]
    FetchUser --> CheckRole{"User Role == PAID?"}

    CheckRole -- "Yes (PAID)" --> HideAds["Render Clean Layout Without Ads"]
    HideAds --> AllowTest["Bypass Daily Usage Limits"]

    CheckRole -- "No (FREE)" --> ShowAds["Embed Google AdSense Placements"]
    ShowAds --> ActionAttempt["User Initiates Test / AI Check"]

    ActionAttempt --> CheckRedis{"Atomic Redis Lua Check > Cap?"}
    CheckRedis -- "Exceeded" --> ShowPaywall["Show Upgrade Modal / Paywall"]
    CheckRedis -- "Allowed" --> IncrRedis["Atomic INCR Usage Counter for Date"]
    IncrRedis --> RunFeature["Proceed to Test / AI Evaluation"]

    class Start,HideAds,AllowTest,ShowAds,ActionAttempt,ShowPaywall client;
    class FetchUser,CheckRole,RunFeature backend;
    class CheckRedis,IncrRedis redis;
```

---

### Diagram 2.2: Stripe Checkout & Webhook Synchronization (Sequence Diagram)

```mermaid
sequenceDiagram
    autonumber
    participant User as "User (Browser)"
    participant Client as "Next.js App"
    participant RateLimitFilter as "RateLimitFilter"
    participant Controller as "SubscriptionController"
    participant WebhookController as "StripeWebhookController"
    participant SubService as "ISubscriptionService (SubscriptionServiceImpl)"
    participant Redis as "Redis Cache"
    participant Stripe as "Stripe API"
    participant DB as "PostgreSQL DB"

    User->>Client: "Click Upgrade to Pro"
    Client->>Controller: "POST /api/subscriptions/checkout-session"
    Controller->>SubService: "createCheckoutSession(userId, req)"
    SubService->>Stripe: "Session.create(customer_email, line_items, urls)"
    Stripe-->>SubService: "Checkout Session URL"
    SubService-->>Controller: "CheckoutSessionResponse DTO"
    Controller-->>Client: "Return Checkout URL JSON"
    Client->>Stripe: "Redirect User to Stripe Checkout Page"
    User->>Stripe: "Enter Card Details & Confirm Payment"

    par Asynchronous Webhook
        Stripe->>WebhookController: "POST /webhooks/stripe (event_id, customer.subscription.created / deleted / invoice.payment_failed)"
        WebhookController->>SubService: "handleWebhook(payload, signatureHeader)"
        SubService->>Redis: "Check stripe:event:{event_id} (SETNX 24h TTL)"
        alt Event Already Processed
            SubService-->>WebhookController: "200 OK (Duplicate Event Ignored)"
        else Event New
            SubService->>SubService: "Verify Signature via Webhook.constructEvent()"
            alt customer.subscription.created / updated
                SubService->>DB: "Update User (role = PAID)"
                SubService->>DB: "Save Subscription status=ACTIVE (extends BaseEntity)"
                SubService->>Redis: "DEL usage:{userId}:{date}"
            else customer.subscription.deleted / invoice.payment_failed
                SubService->>DB: "Update User (role = FREE)"
                SubService->>DB: "Update Subscription status=CANCELED / PAST_DUE"
            end
        end
    and User Redirect
        Stripe-->>User: "Redirect to Success URL (/dashboard?subscription=success)"
        User->>Client: "Load Dashboard"
        Client->>RateLimitFilter: "GET /api/users/me"
        RateLimitFilter-->>Client: "User Profile (role = PAID)"
        Client->>User: "Display Pro Badge & Hide Ads"
    end
```

---

### Diagram 2.3: Subscription & Usage Model (UML Class Diagram)

```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        +UUID id
        +Instant createdAt
        +Instant updatedAt
        +Long version
    }

    class Subscription {
        +UUID userId
        +String stripeCustomerId
        +String stripeSubscriptionId
        +String status
        +Instant currentPeriodEnd
        +boolean cancelAtPeriodEnd
    }

    class User {
        +UUID id
        +UserRole role
        +String subscriptionTier
    }

    BaseEntity <|-- Subscription
    User "1" -- "0..1" Subscription : maintains
```

