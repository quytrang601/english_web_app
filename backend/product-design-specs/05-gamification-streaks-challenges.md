# 05 — Gamification: Streaks and Daily Challenges

---

## 1. Design Philosophy

Gamification must serve learning, not distract from it. Every mechanic is designed to encourage consistent daily practice — the single most important habit for IELTS improvement.

**Inspirations:**
- Duolingo: streaks, hearts, daily challenges, league leaderboards
- Minecraft: large achievement catalogue, discovery-driven progress
- Duolingo twist we add: gamification is anchored to real CEFR mastery levels, not abstract XP. Users can see their actual English proficiency improving.

---

## 2. Streak System

### 2.1 What Is a Streak?

A streak is earned by completing at least **1 learning activity** every calendar day (user's local timezone). Activities that count toward a streak:

- Any dictation exercise
- Any practice drill set
- Flashcard review session (SM-2)
- Reading a CEFR library article
- Starting a mock test section
- Completing a daily challenge
- Playing any minigame
- Submitting a writing or speaking session

### 2.2 Streak Tiers and Rewards

| Days | Visual | Sparks Bonus |
| :---: | :--- | :---: |
| 1–6 | Small flame | — |
| 7 | Medium flame + "Week Warrior" glow | +50 |
| 14 | Bright flame | +100 |
| 30 | Blazing flame + bronze streak badge | +250 |
| 60 | Gold flame + gold streak badge | +500 |
| 100 | Legendary flame + particle effects | +1,000 |
| 180 | Eternal flame badge | +2,000 |
| 365 | Hall of Fame status + special title + exclusive "Year of English" cosmetic | +5,000 |

### 2.3 Streak Freeze

The Streak Freeze prevents a streak from breaking when a user misses a day. It is a safety net, not a cheat. The design makes it feel like protection — a shield the user earned through consistency.

**Earning Streak Freezes:**
- 1 Streak Freeze is earned automatically after every **7 consecutive days** of activity
- Maximum stockpile: **3 Streak Freezes** at any time
- Users can also **buy** a Streak Freeze from the Sparks shop: **50 Sparks each**, max 3 in stock

**How it activates:**
- When a user misses a day, a Freeze auto-applies (if available)
- Streak is preserved exactly as if they had logged in
- The next login shows a warm notification: *"Your streak was protected by a Freeze! 🛡️ You have X Freezes left."*
- Character animation: shield briefly glows around the character

**Design notes:**
- Freezes are shown prominently as collectible items in the profile (3 empty shield slots, filling as freezes are earned)
- The feeling of having 3 full freezes is comforting — users feel protected
- Losing a freeze feels significant but not punishing — like using a safety net
- Achievement: "Untouchable" — reach 30-day streak without ever using a Freeze (+100 Sparks)

---

## 3. Daily Challenges

### 3.1 How They Work

- 3 challenges are presented to the user each day
- Challenges refresh at midnight (user's local timezone)
- Each challenge is **personalized** based on the user's history
- Challenge activities always cost **0 Focus** (see [04-focus-bar.md](./04-focus-bar.md))
- **Completing all 3 = 50 bonus Sparks + +3 Focus refill + progress toward "Perfect Day" achievement**

### 3.2 Personalization Logic

Challenge selection uses these signals:
1. **Weak areas** (CEFR mastery scores per skill — nudge toward the weakest)
2. **Unexplored features** (push users to try things they have never done)
3. **Streak status** (if streak at risk or newly started, include an easy engagement challenge)
4. **Recent activity** (don't repeat yesterday's same challenge type)
5. **User level** (Learner league gets easier challenges than Vanguard league)

### 3.3 Challenge Pool (100+ Templates)

#### Dictation Challenges
- Complete X dictation exercises today (X = 1, 2, or 3 depending on level)
- Achieve 90%+ accuracy on a dictation exercise
- Complete a dictation in a topic you have never tried
- Complete a fast-speed dictation exercise
- Complete 3 consecutive dictation exercises without a single mistake
- Complete a dictation exercise in a new accent bundle

#### Flashcard Challenges
- Add X new words to your deck (X = 5, 10, 15)
- Review all of your currently due flashcards
- Review flashcards for X minutes (X = 5, 10)
- Master (3 correct reviews in a row) 10 flashcards
- Add 5 words from an essay you read today
- Create a new flashcard deck on a topic you have never studied

#### Reading Challenges
- Complete a reading drill set
- Score 80%+ on a reading drill set
- Read 2 CEFR library articles
- Complete a True/False/Not Given drill set
- Complete a Matching Headings drill set
- Finish a full 40-question reading section of a mock test

#### Writing Challenges
- Submit a Task 2 essay for AI scoring (80 Sparks — personalizes to users with a Sparks balance)
- Write 3 thesis statements for Task 2 prompts (unscored drill)
- Paraphrase 3 sentences from a sample essay
- Complete a writing drill set (e.g., Task 1 bar chart description practice)
- Write a 100-word Task 1 response to a graph prompt (self-check, no AI required)

#### Speaking Challenges
- Complete an AI-scored speaking session (60 Sparks)
- Record a 2-minute Part 2 cue card response (unscored, self-review)
- Answer 5 Part 1 questions (unscored practice, no AI)
- Complete a full unscored speaking simulation (all 3 parts, no AI)

#### Grammar Challenges
- Complete a grammar drill set
- Score 100% on any grammar drill set
- Complete 2 grammar drill sets from different topic areas
- Complete an error correction drill set
- Complete a sentence transformation drill set

#### Mock Test Challenges
- Start a full mock test today
- Complete the Reading section of a mock test
- Complete the Listening section of a mock test
- Complete a full mock test without pausing at any point
- Complete 2 different mock test sections today

#### Streak and Engagement Challenges
- Log in and complete any activity
- Maintain your streak today
- Complete 3 activities of any type
- Earn 50 Sparks through learning today
- Spend 20 minutes in the app today
- Play a minigame today

#### Exploration Challenges (for new users, first 14 days)
- Try dictation for the first time
- Create your first flashcard deck
- Complete your first practice drill set
- Read your first CEFR library article
- Take your first mock test
- Play a minigame for the first time
- Visit the character customization shop

#### Mastery Challenges (for Vanguard and Luminary leagues)
- Score Band 7+ on an AI-scored mock writing task
- Achieve 95%+ accuracy on a C1-level dictation exercise
- Master 50 flashcard words in a week (cumulative challenge)
- Complete a C1 reading drill set with 80%+ accuracy
- Complete a full mock test Reading section in under 40 minutes

#### Leaderboard Challenges
- Earn enough Sparks today to enter the top 25% of your league's weekly leaderboard
- Move up at least 5 positions on the leaderboard this week (multi-day challenge)

### 3.4 Challenge Difficulty Scaling

| League | Challenge Difficulty |
| :--- | :--- |
| Learner, Challenger | 1–2 activities per challenge, easy threshold |
| Scholar, Contender | 2–3 activities per challenge, moderate threshold |
| Vanguard, Luminary | 3 activities, strict accuracy thresholds |
| Legend | Same as Vanguard/Luminary + occasional week-long mastery challenges |
