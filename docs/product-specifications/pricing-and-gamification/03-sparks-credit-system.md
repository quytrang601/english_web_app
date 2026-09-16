# 03 — Sparks Credit System

---

## 1. What Are Sparks?

Sparks are the platform's virtual currency. They are:
- **Earned** through learning activity (free, no purchase needed)
- **Purchased** in credit packs via Stripe
- **Spent** on AI-powered features and cosmetic items

**Tagline:** *"Every lesson earns you Sparks. Every Spark fuels your next breakthrough."*

Sparks have **no expiry date** once earned or purchased. They persist even if a user downgrades from Premium to Freemium.

---

## 2. Real Cost Analysis — What Each AI Call Costs Us

### Gemini Flash 3.6
- Input: $1.50 per 1M tokens
- Output: $7.50 per 1M tokens

| Use Case | Avg Input Tokens | Avg Output Tokens | Cost Per Call |
| :--- | :---: | :---: | :---: |
| Writing Check (essay ~600 words + rubric prompt) | ~1,200 | ~600 | **~$0.006** |
| Speaking scoring (transcript ~300 words + rubric) | ~600 | ~400 | **~$0.004** |
| Flashcard AI definition (1 word + context) | ~150 | ~200 | **~$0.002** |
| Semantic embedding (per passage, one-time) | ~500 | — | **~$0.00008** |

### Deepgram Nova-3 STT
- $0.0077/minute (streaming real-time)
- Average IELTS speaking session: ~6 minutes (Parts 1 + 2 + 3)
- Cost per session: **~$0.046**

### Cartesia Sonic TTS
- ~$0.05 per 1,000 characters (Startup plan effective rate)
- Average dictation passage: ~250 characters
- Cost per generation: **~$0.013** — generated once by Content Admin, cached permanently in S3. User playback is free.

### Total Cost Per Feature Call

| Feature | Our Cost | User Pays (Sparks) | Effective Markup |
| :--- | :---: | :---: | :---: |
| Writing Check AI | ~$0.006 | 80 Sparks (~$0.53) | ~88x |
| Speaking Room (STT + Scoring) | ~$0.050 | 60 Sparks (~$0.40) | ~8x |
| Flashcard AI definition | ~$0.002 | 5 Sparks (~$0.033) | ~17x |

> The Speaking Room has a lower markup because Deepgram STT costs 8–10x more than Gemini scoring alone. Both are still highly profitable.

---

## 3. Sparks Pack Pricing

### 3.1 USA Reference Prices

| Pack | Sparks | Price | Cost to us (worst case) | Margin |
| :--- | :---: | :---: | :---: | :---: |
| Starter Pack | 150 | $0.99 | ~$0.011 | ~99% |
| Value Pack | 400 | $2.49 | ~$0.030 | ~99% |
| Power Pack | 1,000 | $5.49 | ~$0.075 | ~99% |
| Mega Pack | 2,500 | $11.99 | ~$0.187 | ~98% |

**Bonus on all purchases:** +10% free Sparks. A user buying 150 receives 165.

### 3.2 PPP-Adjusted Prices

See [02-pricing-and-monetization.md](./02-pricing-and-monetization.md) Section 4.2 for the full regional table.

---

## 4. Earning Sparks (Without Buying)

### 4.1 Daily Activity Earnings

| Activity | Sparks Earned |
| :--- | :---: |
| Daily login (once per calendar day) | 5 |
| Complete a dictation exercise | 3 |
| Perfect score on dictation | +5 bonus |
| Complete a practice drill set | 5 |
| Add 10 flashcards in a day | 8 |
| Review due flashcards (SM-2 session) | 4 |
| Complete a daily challenge | 20 |
| Read a CEFR library article (max 5/day) | 2 |
| Mark a CEFR article as completed | 3 |
| Watch a rewarded ad | 10–20 (5 ads/day max) |

### 4.2 Milestone Bonuses

| Milestone | Sparks |
| :--- | :---: |
| Welcome bonus (first login ever) | 100 |
| First dictation completed | 20 |
| First flashcard deck (10+ cards) | 15 |
| First mock test completed | 50 |
| First writing submission | 25 |
| First speaking session | 25 |
| 50 flashcards mastered | 30 |
| 100 flashcards mastered | 60 |
| 500 flashcards mastered | 200 |
| 1,000 flashcards mastered | 500 |
| Complete all exercises in a free bundle | 25 |
| Complete a full CEFR level article set | 80 |
| Refer a friend (they verify email) | 150 |

### 4.3 Streak Bonuses

| Streak | Sparks |
| :---: | :---: |
| 3 days | 15 |
| 7 days | 50 |
| 14 days | 100 |
| 30 days | 250 |
| 60 days | 500 |
| 100 days | 1,000 |
| 180 days | 2,000 |
| 365 days | 5,000 |

### 4.4 Weekly Leaderboard Bonuses

| Rank (within your league) | Sparks |
| :--- | :---: |
| #1 | 300 |
| #2–3 | 150 |
| #4–10 | 75 |
| Top 25% | 30 |

> Leaderboard is ranked by **earned Sparks only** — purchased Sparks do not count. See [07-leaderboard-and-leagues.md](./07-leaderboard-and-leagues.md).

### 4.5 Minigame Earnings

| Game | Sparks Earned |
| :--- | :--- |
| Win any PVP game | 90% of entry pool (platform takes 10%) |
| Tower Defense — per wave cleared | 5 |
| Tower Defense — complete all 10 waves | +50 |
| Tower Defense — complete with 0 damage | +100 |
| Reading Raid — per correct answer | 5 |
| Reading Raid — speed multiplier bonus | Up to 2x (max ~100 total) |

---

## 5. Spending Sparks

| Item | Cost |
| :--- | :---: |
| Writing Check AI (1 essay submission) | 80 Sparks |
| Speaking Room AI scoring (1 session) | 60 Sparks |
| Flashcard AI definition (1 word) | 5 Sparks |
| Unlock a premium dictation bundle (permanent) | 150 Sparks |
| Unlock a premium drill set (permanent) | 100 Sparks |
| Extra flashcard slots for today (+50 slots) | 80 Sparks |
| Streak Freeze (from shop, max 3 in stock) | 50 Sparks |
| Focus Bar refill (+5 units) | 30 Sparks |
| Common cosmetic item (character shop) | 50–100 Sparks |
| Uncommon cosmetic item | 150–300 Sparks |
| Rare cosmetic item | 400–700 Sparks |
| Epic cosmetic item | 800–1,500 Sparks |
| Legendary cosmetic item | 2,000–4,000 Sparks |
| Character skin | 500–1,500 Sparks |
| PVP game entry fee | 50–100 Sparks (varies by game) |

---

## 6. Typical User Sparks Flow

### Freemium User — Active Learner (30 min/day)

| Activity | Sparks/Day |
| :--- | :---: |
| Daily login | 5 |
| 3 dictation exercises | 9 |
| 2 drill sets | 10 |
| Flashcard review | 4 |
| Daily challenge completion bonus | 50 |
| **Total (typical active day)** | **~78 Sparks** |

- At 78 Sparks/day: earns enough for 1 Writing Check every ~1 day
- At 78 Sparks/day: earns enough for 1 Speaking session every ~0.8 days
- Can accumulate 2,500+ Sparks in ~32 days without buying anything

### The Design Intent

An engaged Freemium user who does their daily activities for 5 days can afford 1 Writing AI check without paying anything. This:
1. Creates a "taste" of the AI feature
2. Rewards consistent learning with access to premium tools
3. Motivates users who want more to either grind harder or buy Sparks / subscribe
