# 07 — Leaderboard and Leagues

---

## 1. Design Principles

The leaderboard must be fair. English proficiency varies enormously — a C1 user competing against an A2 user destroys motivation for the A2 learner and removes challenge for the C1 learner.

Two fairness mechanisms:
1. **Leagues grouped by proficiency** — users only compete within their proficiency tier
2. **Earned Sparks only** — purchased Sparks do not count toward leaderboard rank

---

## 2. The Seven Leagues

| # | League | CEFR Level | Est. IELTS Band | Badge Color | Rarity |
| :---: | :--- | :---: | :---: | :---: | :---: |
| 1 | **Learner** | A1–A2 | < 4.0 | Grey | Most users start here |
| 2 | **Challenger** | A2–B1 | 4.0–5.0 | Bronze | Common |
| 3 | **Scholar** | B1–B2 | 5.0–6.0 | Silver | Common |
| 4 | **Contender** | B2 | 6.0–6.5 | Gold | Moderate |
| 5 | **Vanguard** | C1 | 6.5–7.5 | Platinum | Rare |
| 6 | **Luminary** | High C1 | 7.5–8.5 | Sapphire | Very rare (<5% of users) |
| 7 | **Legend** | C2 | 8.5+ | Obsidian (black + gold animated aura) | Extremely rare (<1%) |

**Design intent for Luminary and Legend:**
- These leagues must feel genuinely exclusive
- The Obsidian badge for Legend has a unique animated gold aura — unmistakably elite
- Reaching Legend should feel like a real life accomplishment, not just an app milestone
- Very few users will ever reach Luminary or Legend — this is intentional

---

## 3. Proficiency Placement Test (Onboarding)

Every new user takes a placement test **immediately after email verification**, before accessing any features. This determines their starting league.

### 3.1 Test Structure

| Section | Questions | Format | Time |
| :--- | :---: | :--- | :---: |
| Grammar | 10 MCQ | Choose the grammatically correct sentence | ~3 min |
| Vocabulary | 10 MCQ | Choose the correct word for the definition | ~3 min |
| Reading Comprehension | 1 short passage + 5 questions | True/False/Not Given | ~5 min |
| Listening | 1 short audio clip + 5 questions | Fill in the blank | ~4 min |

Total: 30 questions, ~15 minutes. Auto-scored. Results are instant.

### 3.2 Placement Results → Starting League

| Score | Starting League | CEFR Estimate |
| :--- | :--- | :---: |
| 0–7 correct | **Learner** | A1–A2 |
| 8–14 correct | **Challenger** | A2–B1 |
| 15–20 correct | **Scholar** | B1–B2 |
| 21–25 correct | **Contender** | B2 |
| 26–28 correct | **Vanguard** | C1 |
| 29–30 correct | **Luminary** | High C1 |
| Perfect + 29–30 within time limit | **Legend** eligibility — confirm with a 10-question extension | C2 |

### 3.3 Placement Test Rules
- Mandatory — users cannot skip it. Access to features is blocked until it is completed.
- Time limit per question is soft (no auto-advance) but total time is shown
- No retakes of the placement test (only league qualification tests are retakeable)

---

## 4. League Movement Rules

### 4.1 Moving Down (Always Free)

Users can freely drop to any league at or below their placement test result at any time.

**Why allow this?**
- A user might want to compete in a lower league to rank higher on the leaderboard
- This is permitted — it only affects competition, not their CEFR mastery profile
- Example: a Contender user drops to Scholar to win the weekly leaderboard. Their mastery data still shows B2 progress.

**How:** A "Change League" button is available in the profile. Requires selecting the target league and confirming.

### 4.2 Moving Up (Requires Passing a Qualification Test)

To move up one league, the user must pass a **League Qualification Test**.

| Property | Value |
| :--- | :--- |
| Duration | 20 questions, ~15 minutes |
| Content | Reading, Listening, Grammar, Vocabulary — calibrated to the target league's level |
| Pass threshold | 70% correct (14/20) |
| Fail cooldown | 7 days before retaking |
| Cost | 0 Sparks, 0 Focus |
| Result | Instant promotion + celebration animation + Sparks reward |

**Promotion Sparks Rewards:**

| Promotion Path | Sparks | Extra Reward |
| :--- | :---: | :--- |
| Learner → Challenger | 100 | Bronze badge cosmetic |
| Challenger → Scholar | 200 | Silver badge cosmetic |
| Scholar → Contender | 350 | Gold badge cosmetic |
| Contender → Vanguard | 600 | Platinum badge cosmetic |
| Vanguard → Luminary | 1,000 | Sapphire badge + exclusive "Luminary" aura (cannot be bought) |
| Luminary → Legend | 2,500 | Obsidian badge + exclusive "Legend" animated aura (cannot be bought) |

### 4.3 No Automatic Demotion

The system **never** automatically demotes a user based on inactivity or performance. League status is permanent until the user chooses to drop down or passes a test to go up.

**Rationale:** Automatic demotion feels punishing and discourages users from taking risks or experimenting. The competitive element resets weekly via the leaderboard, not via league demotion.

---

## 5. Weekly Leaderboard

### 5.1 What It Measures

The weekly leaderboard ranks users by **Sparks earned through learning activity** during the current calendar week (Monday–Sunday).

**Counted:** Sparks from dictation, drills, flashcards, mock tests, daily challenges, streak bonuses, article reading, minigame wins.  
**NOT counted:** Sparks from pack purchases or ad watching.

This ensures money cannot buy leaderboard position.

### 5.2 Leaderboard Views

Users have two views available:

| View | Description |
| :--- | :--- |
| **Your Region** | Compete only with users in your country |
| **Global** | Compete with all users in your league worldwide |

Default view is "Your Region" — this is more motivating for most users since the competition pool is smaller and more relatable.

### 5.3 Weekly Reset and Prizes

Leaderboard resets every Monday at 00:00 UTC.

**Prizes (in addition to standard streak and activity Sparks):**

| Rank (within your league, your region) | Bonus Sparks |
| :--- | :---: |
| #1 | 300 |
| #2–3 | 150 |
| #4–10 | 75 |
| Top 25% | 30 |

### 5.4 Target Regions

The platform targets non-English-speaking countries where IELTS preparation demand is highest.

| Region | Countries |
| :--- | :--- |
| Southeast Asia | Vietnam, Indonesia, Philippines, Thailand, Malaysia, Myanmar, Cambodia |
| South Asia | India, Pakistan, Bangladesh, Nepal, Sri Lanka |
| East Asia | Japan, South Korea, Taiwan, China |
| Middle East | UAE, Saudi Arabia, Egypt, Jordan, Iraq, Kuwait |
| Central Asia | Kazakhstan, Uzbekistan |
| Latin America | Brazil, Colombia, Mexico, Argentina |
| Eastern Europe | Ukraine, Poland, Romania, Turkey |

---

## 6. CEFR Mastery Profile (The Platform's Unique Twist)

Unlike Duolingo's abstract XP system, each user has a **CEFR Mastery Profile** — a per-skill breakdown of their estimated proficiency level, updated after every activity.

| Skill | What Updates It |
| :--- | :--- |
| Reading Mastery | Practice drill accuracy, mock test reading scores |
| Listening Mastery | Dictation accuracy, mock test listening scores |
| Writing Mastery | AI essay scoring results (where available) |
| Vocabulary Mastery | Flashcard SM-2 retention rate |
| Grammar Mastery | Grammar drill accuracy across all topics |

This profile is visible on the user's profile page as a radar/spider chart showing their B-level estimate per skill. It updates in real time and is the primary reason users keep doing activities — they can watch their real English ability improve.

The CEFR Mastery Profile also drives the **personalized daily challenge** selection (see [05-gamification-streaks-challenges.md](./05-gamification-streaks-challenges.md)).
