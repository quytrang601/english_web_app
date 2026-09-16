# 12 — Admin System

---

## 1. Overview

There are two admin roles with completely separate capabilities:

| Role | Access Level | Target User |
| :--- | :--- | :--- |
| `CONTENT_ADMIN` | Learning content only | Non-technical content writers, curriculum designers |
| `SUPER_ADMIN` | Full system | Founders, senior technical staff |

Neither role appears on leaderboards or in Freemium/Premium feature flows.

---

## 2. Content Admin Capabilities

Content Admins have access only to the **content management** section of the admin panel. They cannot see user data, billing, or system configuration.

### 2.1 Mock Tests
- Create new full IELTS mock tests (all 4 sections: Reading, Listening, Writing, Speaking prompts)
- Edit or delete existing mock tests
- Set test visibility: `DRAFT` (not visible to users) or `PUBLISHED`
- Add metadata: difficulty level, CEFR tag, topic tags, IELTS Academic vs General Training flag
- Preview a test exactly as a user would see it

### 2.2 Reading Passages
- Upload or paste reading passages (Academic and General Training)
- Tag passages by topic (15 predefined topics), CEFR level, question types
- Attach questions to passages (all 14 IELTS reading question types supported)
- Set answer keys and mark schemes
- Set passage as standalone practice or link to a mock test

### 2.3 Listening Audio and Questions
- Upload audio files to S3 (`.mp3` or `.wav`)
- Attach audio to listening sections (Sections 1–4)
- Create question sets for the audio (all 7 listening question types)
- Set time codes for when questions appear (optional, for interactive mode)
- Tag by section type, difficulty, topic

### 2.4 Dictation Exercises
- Create individual dictation exercises (text + generate or upload audio)
- Assign to bundles: by CEFR level, topic, type, accent
- Trigger Cartesia TTS generation for the exercise text (auto-generates and caches to S3)
- Set exercise difficulty, CEFR level, accent type
- Preview the exercise audio before publishing

### 2.5 Sample Essays
- Upload or write sample essays (Task 1 and Task 2)
- Attach IELTS band score estimate, topic tags, essay type tags
- Mark essays as model answers or student examples
- Set visibility: public, Premium only

### 2.6 CEFR Reference Library
- Create articles (Grammar, Vocabulary, Skills strategy)
- Assign CEFR level (A1–C2), topic, skill category
- Organize articles into level sets (e.g., "All B2 Grammar articles")
- Mark articles as completed by Content Admin (triggers a review check)

### 2.7 Flashcard Definitions
- Review AI-generated word definitions flagged for manual verification
- Edit or approve/reject AI definitions
- Add curated example sentences to word definitions

### 2.8 Skill Practice Drills
- Create practice drill sets (all types: Reading, Writing, Grammar, Vocabulary)
- Set as free (included in Freemium) or premium (100 Sparks or Premium)
- Add questions, answer keys, explanations
- Organize by CEFR level and topic

---

## 3. Super Admin Capabilities

Super Admin has all Content Admin capabilities plus:

### 3.1 User Management
- Search users by email, username, ID
- View full user profile: plan, Sparks balance, league, streak, achievements, purchase history
- Suspend or permanently delete a user account
- Manually set a user's plan (Freemium → Premium or vice versa)
- Manually set a user's league (override placement test result)
- Manually grant or deduct Sparks from a user's account (support cases)
- View a user's login history and activity log

### 3.2 Billing and Revenue
- View Stripe subscription dashboard (MRR, churn, ARR)
- View Sparks pack purchase history
- Issue refunds for Sparks packs (Stripe refund via API)
- Apply coupon codes to a specific user's account
- View Ad-Free Pass purchase history
- Estimated ad revenue dashboard (linked to AdMob/AdSense)

### 3.3 Gamification Configuration (no code deploy required)

All gamification values are stored in a **config table** in the database, editable from the Super Admin panel:

| Configurable | Example |
| :--- | :--- |
| Sparks earned per activity type | "Dictation exercise = 3 Sparks" |
| Focus Bar max units | "20 units" |
| Focus Bar regen rate | "1 unit/hour" |
| Focus cost per activity | "Dictation = 1 unit" |
| Daily ad cap per user | "5 ads/day" |
| Sparks per rewarded ad | "15 Sparks" |
| Daily challenge bonus Sparks | "50 Sparks for all 3 complete" |
| Streak milestone bonus Sparks | "30-day streak = 250 Sparks" |
| Streak Freeze shop price | "50 Sparks" |

### 3.4 Achievement and Challenge Management

- View full achievement catalogue
- Enable or disable specific achievements
- Edit achievement Sparks reward value
- Add new achievement definitions (name, description, condition trigger, Sparks)
- View daily challenge template pool
- Add new daily challenge templates
- Enable/disable specific challenge types
- View challenge completion rates and skip rates

### 3.5 Feature Flags

- Toggle features on/off without a code deploy:
  - Ads enabled / disabled globally
  - Focus Bar enabled / disabled globally
  - Minigames enabled / disabled globally
  - Specific minigame enabled / disabled
  - Season event items visible / hidden
  - League qualification tests enabled / disabled

### 3.6 Analytics Dashboard

| Metric | Details |
| :--- | :--- |
| User growth | DAU, MAU, WAU, new registrations per day |
| Conversion rate | Freemium → Premium, per region |
| Revenue | MRR, ARR, breakdown by revenue stream |
| Sparks economy | Total Sparks earned this week, total spent, total purchased |
| Most used features | Which features get the most daily sessions |
| AI cost tracker | Total Gemini + Deepgram spend this month vs budget |
| Leaderboard standings | Top 10 per league, per region |
| Retention | D1, D7, D30 retention rates |
| Achievement completion rates | Which achievements are most and least earned |
| Challenge skip rates | Which daily challenges are most skipped |

### 3.7 Audit Log

All Super Admin actions are recorded in an **append-only audit log**:
- Who performed the action (Super Admin ID)
- What action (e.g., "Manually granted 500 Sparks to user #12345")
- Timestamp
- Reason (optional free-text field)

The audit log cannot be edited or deleted. It is viewable only by other Super Admins.

### 3.8 Announcements

- Post system-wide banners visible to all logged-in users
- Schedule banners with a start and end date
- Target banners to specific roles (Freemium only, Premium only, all users)
- Examples: maintenance notices, discount promotions, new feature launches

### 3.9 Credit Grants for Support Cases

When a user reports that an AI feature failed (Writing Check returned an error, Speaking Room crashed mid-session), Super Admin can:
1. Verify the error in the API logs
2. Manually refund the Sparks to the user's account
3. Log the refund in the audit log with the reason

This is the only form of Sparks "refund" — it is manual and case-by-case.
