# 09 — Minigames

---

## 1. Design Principles

- Every game **requires actual English or IELTS knowledge** to win — not just reaction speed alone
- PVP games use a **Sparks entry pool**; winner takes 90% (platform keeps 10%)
- All games use the user's equipped **character as their avatar**
- Games are quick (2–5 minutes) — meant to be played *between* study sessions, not instead of them
- PVP matchmaking: **random opponent** (MVP simplicity; no league restriction)
- Minigame wins/losses contribute to a separate **Minigame Leaderboard**

---

## 2. Focus Bar Costs for Games

| Game Type | Focus Cost (Freemium) |
| :--- | :---: |
| PVP game | 2 Focus |
| Solo game (Tower Defense, Reading Raid) | 1 Focus |
| Premium | 0 Focus (no Focus Bar) |

---

## 3. Game 1 — Synonym Blaster

**Type:** Arcade shooter, 1v1 or solo  
**Duration:** 3 rounds, ~3 minutes total  
**Entry:** 50 Sparks → winner takes 90 Sparks

### Mechanics
- A target word appears at the top of the screen
- Words rain down from the sky — some are synonyms of the target (correct), some are unrelated (traps)
- Your character shoots upward at words using a "word cannon"
- Hit a correct word → +10 points, satisfying hit animation
- Hit a wrong word → −5 HP
- A wrong word reaches the bottom → −10 HP
- A correct word reaches the bottom (missed) → no penalty

### 1v1 Mode
- Both players see the same stream of words simultaneously
- Shooting a correct word first = your character fires a projectile at the opponent (−5 HP)
- First player to reach 0 HP loses that round
- Best of 3 rounds wins the Sparks pot

---

## 4. Game 2 — Grammar Duel

**Type:** Turn-based battle, 1v1  
**Duration:** 10 rounds, ~4 minutes  
**Entry:** 100 Sparks → winner takes 180 Sparks

### Mechanics
- Both players see the same grammar question (error correction, fill-in-blank, sentence transformation)
- First to answer correctly deals damage
- Correct answer → 20 HP damage to opponent
- Wrong answer → −5 self-damage (prevents random guessing)
- No answer in 15 seconds → round skipped, no damage

### Special Moves (triggered by consecutive correct answers)

| Trigger | Special Move | Effect |
| :--- | :--- | :--- |
| 3 correct in a row | Power Strike | 30 damage instead of 20 |
| 5 correct in a row | Critical Hit | 40 damage + heal yourself 10 HP |
| Answer in under 3 seconds | Lightning Fast | +10 bonus damage that round |
| Wrong then immediately correct (next round) | Comeback | Full 20 damage, no self-damage |

Both players start with 100 HP. First to reach 0 loses.

---

## 5. Game 3 — Dictation Sprint Duel

**Type:** Speed typing, 1v1  
**Duration:** 5 rounds, ~4 minutes  
**Entry:** 75 Sparks → winner takes 135 Sparks

### Mechanics
- A short audio clip (10–15 words) plays simultaneously for both players
- Both type what they hear as fast and accurately as possible
- Scoring: 2 points per correctly typed word
- After 5 rounds, highest total score wins
- Each correctly typed word = small attack animation on opponent's character

### Catch-Up Mechanic
- If trailing by 10+ points with 2 rounds left: trailing player receives 1 easier clip

---

## 6. Game 4 — Word Bid

**Type:** Bluffing / strategy, 3–5 players  
**Duration:** 7 rounds, ~5 minutes  
**Entry:** 80 Sparks per player → winner takes 85% of total pot (platform keeps 15%)

### Mechanics
- Each round: a word definition or synonym question appears
- Before seeing answer options, each player secretly bids (10–100 virtual game tokens from their starting pool of 500)
- All answers and bids revealed simultaneously
- Correct answer → win your bid from the pot
- Wrong answer → lose your bid to the pot
- After 7 rounds, player with most virtual tokens wins the real Sparks prize

### The Bluffing Layer
You might know the answer and bid high. Or you might not know and bid low. Other players cannot tell — this adds psychological tension on top of vocabulary knowledge. Works like a poker game where the cards are English questions.

---

## 7. Game 5 — IELTS Lightning Round

**Type:** Quick-fire quiz, 1v1 or 2v2  
**Duration:** 20 questions, ~5 minutes  
**Entry (solo 1v1):** 80 Sparks → winner takes 144 Sparks  
**Entry (2v2):** 80 Sparks per player → winning team splits 288 Sparks

### Mechanics
- 20 IELTS-style questions, mixed: MCQ Reading, Grammar, Vocabulary, T/F/NG, Sentence Completion
- 15 seconds per question
- Correct → +1 point, character flexes
- Wrong → no penalty (keeps mood positive)
- Taunting: if you lead, your character taunts; if trailing, character looks determined

### 2v2 Team Mode
- Partners alternate questions (Player A answers odd, Player B answers even)
- Quick emoji reactions only for communication (no text — keeps pace)
- Team score = combined correct answers

### Catch-Up Mechanic
- If trailing by 5+ points with 5 questions left: trailing player/team receives 2 double-point questions

---

## 8. Game 6 — Fill the Blank Race

**Type:** Racing game, 1v1  
**Duration:** 10 checkpoints, ~3 minutes  
**Entry:** 60 Sparks → winner takes 108 Sparks

### Mechanics
- Visual: two characters racing along a track
- Each checkpoint = one fill-in-the-blank IELTS sentence
- Correct answer → character sprints to next checkpoint
- Wrong answer → 1-second stumble (brief slowdown, no damage)
- First character to cross the finish line wins

### Sprint Power-Up
Every 3rd question is a "sprint question." Correct within 5 seconds = speed boost that skips ~half a checkpoint ahead.

---

## 9. Game 7 — Vocabulary Tower Defense

**Type:** Strategy / tower defense, solo or 2-player co-op  
**Duration:** 10 waves, ~5 minutes  
**Entry:** Free → earns Sparks based on performance

### Mechanics
- Your character stands in front of a "Word Tower"
- Waves of enemy words approach — each has a word printed on it
- Challenge displayed at top: e.g., "DESTROY all synonyms of 'significant'"
- Type or tap correct enemies → they are destroyed (character attack animation)
- Let a correct enemy through → tower takes damage
- Let a wrong enemy through → no damage (they pass harmlessly)
- Boss enemy every 5 waves: takes 3 correct answers to defeat, worth bonus Sparks

### Sparks Earned
- +5 Sparks per wave cleared
- +50 Sparks for completing all 10 waves
- +100 Sparks for completing with 0 tower damage
- Daily leaderboard: all players who played that day ranked by score

### Co-op Mode
- 2 players defend the same tower
- Player 1 handles left-side enemies, Player 2 handles right-side enemies
- Both players must communicate: if an enemy type crosses sides, either can attack it

---

## 10. Game 8 — Reading Raid (Daily PvE Dungeon)

**Type:** Solo dungeon crawler with daily competitive leaderboard  
**Duration:** ~8 minutes  
**Entry:** Free → earns Sparks based on performance

### Mechanics
- Each day, a new "dungeon" is generated — a unique reading passage with 10 questions
- Your character walks through a dungeon environment, encountering questions as "monsters"
- Answer correctly → defeat the monster, character moves deeper
- Answer wrong → take damage (3 lives total)
- Speed bonus: faster completion multiplies Sparks earned (up to 2x multiplier)
- End score = (correct answers × accuracy bonus) × speed multiplier

### Sparks Earned
- Base: 5 Sparks per correct answer (max 50 for a perfect run)
- Speed multiplier: up to 2x (max ~100 Sparks for a perfect speed run)
- Daily leaderboard: top 10% of all players that day earn +30 bonus Sparks

---

## 11. Minigame Economy Summary

| Game | Entry | Winner Payout | Platform Take | Type |
| :--- | :---: | :---: | :---: | :--- |
| Synonym Blaster | 50 Sparks | 90 Sparks | 10% | PVP |
| Grammar Duel | 100 Sparks | 180 Sparks | 10% | PVP |
| Dictation Sprint Duel | 75 Sparks | 135 Sparks | 10% | PVP |
| Word Bid | 80/player | 85% of pot | 15% | Multiplayer |
| IELTS Lightning Round | 80 Sparks | 144 Sparks | 10% | PVP / 2v2 |
| Fill the Blank Race | 60 Sparks | 108 Sparks | 10% | PVP |
| Tower Defense | Free | 5–150 earned | — | Co-op PvE |
| Reading Raid | Free | 5–100 earned | — | Solo PvE |

---

## 12. Minigame Achievements

See [06-achievements-and-titles.md](./06-achievements-and-titles.md) — Category 11.
