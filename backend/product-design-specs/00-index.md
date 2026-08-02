# Planning Documentation — Index

> **Status:** Pre-implementation planning. All decisions here must be approved before code is written.  
> **Last updated:** August 2026  
> **Scope:** Full product design for the IELTS / English Learning Platform

---

## Document Map

| # | File | What It Covers |
| :---: | :--- | :--- |
| 01 | [Roles and Access](./01-roles-and-access.md) | 5 user roles, permission matrix, what each role can and cannot do |
| 02 | [Pricing and Monetization](./02-pricing-and-monetization.md) | Subscription tiers, PPP-adjusted regional pricing, Ad-Free Pass, discount calendar, cost analysis |
| 03 | [Sparks Credit System](./03-sparks-credit-system.md) | What Sparks are, cost analysis per AI call, pack pricing, earning without buying, spending |
| 04 | [Focus Bar](./04-focus-bar.md) | Daily energy system, what it gates, recharge mechanics, UI design, interaction with other systems |
| 05 | [Gamification — Streaks and Daily Challenges](./05-gamification-streaks-challenges.md) | Streak mechanics, streak freeze, daily challenge pool (100+ templates), personalization |
| 06 | [Achievements and Titles](./06-achievements-and-titles.md) | 150+ achievements in 11 categories, title system, milestone rewards |
| 07 | [Leaderboard and Leagues](./07-leaderboard-and-leagues.md) | 7 league tiers, proficiency placement test, promotion via test, regional grouping, fairness design |
| 08 | [Character System](./08-character-system.md) | Character types, customization slots, rarity tiers, earning vs buying items, integration across the platform |
| 09 | [Minigames](./09-minigames.md) | 8 games with full mechanics, PVP credit betting, Sparks economy per game |
| 10 | [Skill Practice Module](./10-skill-practice-module.md) | Exhaustive IELTS skill practice: Reading (14 question types), Listening (4 sections, 7 types), Writing (Task 1 + Task 2), Speaking (3 parts), Grammar (25 topics), Vocabulary, Smart Practice |
| 11 | [Content Bundles](./11-content-bundles.md) | All dictation bundles (by CEFR level, topic, type, accent) and practice drill sets (free vs premium split) |
| 12 | [Admin System](./12-admin-system.md) | Content Admin capabilities, Super Admin capabilities, gamification controls |

---

## Core Design Principles

These principles must be respected by every feature built.

### 1. AI = Never Free
Any feature that calls an external AI model (Gemini, Deepgram, Cartesia) requires either Sparks or a Premium subscription. There are no exceptions. See [Sparks Credit System](./03-sparks-credit-system.md) and [Roles and Access](./01-roles-and-access.md).

### 2. Mock Tests Are Always Available to Freemium
Full IELTS mock tests are unlocked for all registered users (Freemium and Premium). They are the core product. Blocking them kills conversion. The monetization hook is AI scoring of Writing and Speaking sections, not access to the test itself.

### 3. Daily Challenges Are Never Blocked
Daily challenge activities always cost 0 Focus. The streak system must never be put at risk by the Focus Bar. A user should always be able to maintain their streak regardless of their Focus level.

### 4. Sparks Earned Through Learning Are Separated From Purchased Sparks on the Leaderboard
The leaderboard ranks only earned Sparks (from learning activity). Purchased Sparks do not count. This ensures money cannot buy leaderboard position.

### 5. League Tiers Are Permanent Until Changed by the User or a Passed Test
No automatic league demotion. Users choose to drop down. They must pass a qualification test to go up. League status is earned and should feel permanent and prestigious.

### 6. The Focus Bar Is Framed Positively
Focus is not a punishment. It is a daily learning pacing mechanic. Messaging must always emphasize: "Daily learning beats binge learning — and science agrees." Premium removes the bar entirely.

---

## Technology Decisions (Relevant to Planning)

| Tool | Purpose | Notes |
| :--- | :--- | :--- |
| Gemini Flash 3.6 | Writing scoring, Speaking scoring, Flashcard definitions, Embeddings | ~$0.006/writing call, ~$0.004/speaking scoring call |
| Deepgram Nova-3 | Speaking Room STT transcription | ~$0.046/session (6 min avg) |
| Cartesia Sonic | Dictation TTS audio generation | ~$0.013/generation, cached permanently in S3 |
| Stripe | Subscription billing, Sparks pack purchases, Ad-Free Pass | Webhooks handle all lifecycle events |
| AdMob / AdSense | Rewarded ads for Sparks (Freemium only) | 5 ads/day cap, +3 Focus or +10–20 Sparks per ad |

---

## Key Numbers to Remember

| Metric | Value |
| :--- | :--- |
| Sparks for Writing Check AI | 80 Sparks |
| Sparks for Speaking Room (AI) | 60 Sparks |
| Sparks for Flashcard AI definition | 5 Sparks per word |
| Focus Bar max (Freemium) | 20 units |
| Focus regen rate | +1 per hour |
| Freemium flashcard daily add limit | 20 cards/day |
| Extra cards via ad | +10 per ad (max 3 ads/day) |
| Extra cards via Sparks | 80 Sparks = +50 slots for the day |
| Starter Sparks Pack (USA) | $0.99 → 150 Sparks |
| Vietnam Premium Monthly | 99,000₫/month |
| Number of leagues | 7 (Learner → Legend) |
| Number of achievements | 150+ |
| Number of minigames | 8 |
