# 01 — Roles and Access

---

## 1. The Five Roles

Every user account has exactly one role stored as an enum on the `User` entity.

| Role | Who Is This? |
| :--- | :--- |
| `GUEST` | Not logged in. Can view the landing page and read 3 sample essay previews. Cannot earn Sparks. |
| `FREEMIUM` | Registered user with a verified email. No active subscription. Sees ads. Has access to core learning features with limits. Earns Sparks. |
| `PREMIUM` | Active Stripe subscriber. Full unlimited access to all learning features. No ads. No Focus Bar. Still earns Sparks. |
| `CONTENT_ADMIN` | Internal staff member managing all learning content. Cannot see user data, billing, or system config. |
| `SUPER_ADMIN` | Full system access: users, billing, analytics, gamification config, system settings. |

> There is no Moderator role in MVP. There is no user-generated content (no comments, no forums) to moderate.

---

## 2. Feature Access Matrix

### 2.1 User-Facing Features

| Feature | GUEST | FREEMIUM | PREMIUM |
| :--- | :---: | :---: | :---: |
| Browse landing page | ✅ | ✅ | ✅ |
| Read 3 sample essay previews | ✅ | ✅ | ✅ |
| Register and log in | — | ✅ | ✅ |
| **Ads shown** | ✅ | ✅ | ❌ |
| **Earn Sparks** | ❌ | ✅ | ✅ |
| **Watch rewarded ads for Sparks / Focus** | ❌ | ✅ (5/day) | ❌ |
| **Ad-Free Pass (one-time purchase)** | ❌ | ✅ (removes ads, no other benefits) | N/A |
| **Focus Bar** | — | ✅ (20 units/day) | ❌ (no limit) |
| **Proficiency placement test** | ❌ | ✅ (mandatory on signup) | ✅ |
| **Full progress tracking (CEFR mastery profile)** | ❌ | ✅ | ✅ |
| **Daily challenges** | ❌ | ✅ (personalized) | ✅ (personalized) |
| **Streaks and streak freeze** | ❌ | ✅ | ✅ |
| **Achievements and badges** | ❌ | ✅ | ✅ |
| **Leaderboard** | ❌ | ✅ (league-grouped) | ✅ (league-grouped) |
| **Character customization** | ❌ | ✅ | ✅ |
| **CEFR Reference Library** (all articles) | ❌ | ✅ Full | ✅ Full |
| **Sample Essays Library** | 3 preview | 10/month | Unlimited |
| **Flashcards — total cards** | ❌ | Unlimited | Unlimited |
| **Flashcards — daily add limit** | ❌ | 20/day (extendable) | Unlimited |
| **Flashcards — AI definition** (Gemini) | ❌ | 5 Sparks/word | Free (included) |
| **Dictation — free bundles** | ❌ | ✅ (5 free bundles) | All unlocked |
| **Dictation — premium bundles** | ❌ | 150 Sparks each | All unlocked |
| **Skill Practice — free sets** | ❌ | ✅ (starter sets) | All unlocked |
| **Skill Practice — premium sets** | ❌ | 100 Sparks each | All unlocked |
| **IELTS Full Mock Tests** | ❌ | ✅ **Unlimited** | ✅ Unlimited |
| **Writing Check AI** (Gemini scoring) | ❌ | 80 Sparks/submission | 5/day free + Sparks for more |
| **Speaking Room — AI scored** (Deepgram + Gemini) | ❌ | 60 Sparks/session | 10/day free + Sparks for more |
| **Speaking Practice — unscored** (no AI) | ❌ | ✅ Free | ✅ Free |
| **Semantic search** (pgvector) | ❌ | Basic keyword only | Full semantic |
| **PDF export** | ❌ | ❌ | ✅ |
| **Minigames** (PVP, solo) | ❌ | ✅ (costs Focus + Sparks entry) | ✅ (costs Sparks entry, no Focus) |

### 2.2 Admin Features

| Capability | CONTENT_ADMIN | SUPER_ADMIN |
| :--- | :---: | :---: |
| Create / edit / delete mock tests | ✅ | ✅ |
| Create / edit / delete reading passages | ✅ | ✅ |
| Create / edit / delete sample essays | ✅ | ✅ |
| Create / edit / delete CEFR articles | ✅ | ✅ |
| Review and verify word definitions | ✅ | ✅ |
| Set content visibility (DRAFT / PUBLISHED) | ✅ | ✅ |
| Upload Listening audio to S3 | ✅ | ✅ |
| View individual user profiles and data | ❌ | ✅ |
| Search and manage users | ❌ | ✅ |
| Suspend or delete user accounts | ❌ | ✅ |
| Manually set a user's plan or league | ❌ | ✅ |
| Grant or deduct Sparks from a user | ❌ | ✅ |
| View billing, revenue, and MRR | ❌ | ✅ |
| Issue refunds via Stripe | ❌ | ✅ |
| Apply discount coupons | ❌ | ✅ |
| Configure Focus Bar limits per role | ❌ | ✅ |
| Configure Sparks earned per activity | ❌ | ✅ |
| Toggle feature flags | ❌ | ✅ |
| Post system-wide announcements | ❌ | ✅ |
| View audit log | ❌ | ✅ |
| View analytics dashboard | ❌ | ✅ |
| Manage daily challenge templates | ❌ | ✅ |
| Manage achievement definitions | ❌ | ✅ |

---

## 3. Rule: AI Features Are Never Free

Any feature that calls an external AI service is gated by Sparks or Premium. No exceptions.

| Service | Used For | Cost to Us | Gating |
| :--- | :--- | :---: | :--- |
| Gemini Flash | Writing scoring, Speaking scoring, Flashcard definitions, Embeddings | ~$0.002–$0.006/call | Sparks or Premium |
| Deepgram Nova-3 | Speaking Room STT transcription | ~$0.046/session | Bundled with Speaking Sparks cost |
| Cartesia Sonic | Dictation audio generation | ~$0.013/generation | One-time cost by Content Admin, playback is free |

> Dictation audio is generated once by Content Admin when creating exercises, then cached permanently in S3. Users replay cached audio for free. No per-user AI cost for dictation playback.

---

## 4. Freemium Flashcard Daily Limit — Expansion Options

Base limit: **20 cards per day**.

| Method | Bonus Cards | Limit |
| :--- | :---: | :--- |
| Watch a rewarded ad | +10 cards | Max 3 ads/day (+30 max) |
| Spend 80 Sparks | +50 cards | Once per day |
| Premium subscription | Unlimited | No daily limit |

---

## 5. Role Transition Events

| Event | Trigger | Result |
| :--- | :--- | :--- |
| New user registers | Email verified | Role: `FREEMIUM` |
| Stripe `checkout.session.completed` | Payment succeeded | Role: `PREMIUM`, store `stripeCustomerId`, `subscriptionId`, `currentPeriodEnd` |
| Stripe `invoice.payment_succeeded` | Renewal | Extend `currentPeriodEnd` |
| Stripe `invoice.payment_failed` | Payment failed | Send email, start 3-day grace period |
| Grace period expires | 3 days after failed payment | Role: `FREEMIUM` |
| Stripe `customer.subscription.deleted` | Cancellation | Role: `FREEMIUM` immediately |
| Admin action | Super Admin manually sets role | Instant role change |

**Downgrade behavior (Premium → Freemium):**
- All user data is preserved (flashcards, test history, essays, progress, characters, Sparks balance)
- Premium features become inaccessible
- "Your plan has expired" banner is shown
- No data is ever deleted on downgrade
