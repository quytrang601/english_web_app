# 04 — Focus Bar (Daily Energy System)

---

## 1. Concept and Philosophy

The Focus Bar is the platform's daily energy system. It represents cognitive bandwidth — the idea that consistent daily practice (20–30 minutes) is more effective than weekend binges.

**User-facing framing:**  
*"Your Focus recharges every day. Learning a little every day beats cramming once a week — and science agrees."*

This is not presented as a restriction. It is presented as a feature grounded in learning science.

**The mechanic in one sentence:** Freemium users have 20 Focus units per day. Most intensive activities cost 1–2 units. The bar refills hourly. Premium removes the bar entirely.

---

## 2. Specifications

| Property | Value |
| :--- | :--- |
| Maximum Focus (Freemium) | 20 units |
| Starting Focus after daily login | Full (20 units) |
| Hourly regeneration rate | +1 unit per hour when below max |
| Full passive recharge time | 20 hours offline |
| Premium users | **No Focus Bar** — unlimited activity |
| GUEST users | No Focus Bar (they have no access to activities anyway) |

---

## 3. Activity Cost Table

### What Costs Focus (Freemium only)

| Activity | Focus Cost |
| :--- | :---: |
| Dictation exercise (1 exercise) | 1 |
| Practice drill set (1 set) | 1 |
| PVP minigame (Grammar Duel, Synonym Blaster, etc.) | 2 |
| Solo minigame (Tower Defense, Reading Raid) | 1 |
| Flashcard bulk-add session (adding 20 cards) | 1 |

### What NEVER Costs Focus

| Activity | Cost | Reason |
| :--- | :---: | :--- |
| **Full IELTS Mock Test** | **0** | Core product — must always be accessible |
| CEFR Library reading | 0 | Top-of-funnel, always free |
| Flashcard review (SM-2 sessions) | 0 | Reviewing your own cards is always free |
| **Daily challenge tasks** | **0** | Streak must never be blocked by Focus |
| Proficiency placement test | 0 | Onboarding — must complete without barriers |
| League qualification test | 0 | Progression — must complete without barriers |
| AI-scored features (Writing, Speaking) | 0 Focus — gated by Sparks | Separate gating system |

> **Key design decision:** Daily challenge activities cost 0 Focus even if the underlying activity normally costs Focus. If the challenge says "Complete 2 dictation exercises," those 2 exercises are Focus-free. This ensures the streak system is never blocked by the Focus Bar.

---

## 4. Refilling Focus

| Method | Focus Gained | Notes |
| :--- | :---: | :--- |
| Wait (natural hourly regen) | +1/hour | Passive, no user action needed |
| Watch a rewarded ad | +3 Focus | Capped at 5 ads/day overall |
| Spend 30 Sparks | +5 Focus | Available anytime in the Focus popup |
| Complete all 3 daily challenges | +3 Focus bonus | Reward for consistency |
| Streak milestone (7-day, 30-day, etc.) | +Full refill (one-time) | Celebrates the streak |

---

## 5. UI Design

### Header Display
- Focus Bar is displayed in the home screen header, next to the Sparks balance counter
- Icon: an animated lightning bolt or flame (matches Sparks branding)
- Segmented bar with 20 visible segments
- Number shown: "15 / 20 Focus"

### Color States

| Focus Level | Color | Meaning |
| :---: | :---: | :--- |
| 15–20 | 🟢 Green | Ready to learn |
| 10–14 | 🟡 Yellow | Getting used up |
| 5–9 | 🟠 Orange | Running low |
| 1–4 | 🔴 Red | Almost empty |
| 0 | ⚫ Grey | Depleted — must recharge |

### Empty State Popup

When a user tries an activity with 0 Focus, a friendly popup appears:

> **"Your Focus is empty! 🧠"**  
> Your brain deserves a rest. Come back when you're recharged!  
> — Next unit refills in **43 minutes**  
>
> **[Watch an ad +3 Focus]** &nbsp; **[Spend 30 Sparks +5 Focus]** &nbsp; **[Go Premium — No Limits]**

The popup does not say "you've run out of lives" or any punishing language. It is friendly, science-backed, and always offers a path forward.

### Character Idle Animation

| Focus Level | Character State |
| :--- | :--- |
| Full (20) | Energetic idle, small sparkle effect around character |
| Medium (10–19) | Normal idle |
| Low (1–9) | Slightly slouched, yawning animation |
| Empty (0) | Character sits down, looks tired |
| Just refilled to full | Character jumps up, mini-celebration |

---

## 6. Interaction With Other Systems

| System | Interaction |
| :--- | :--- |
| **Streaks** | No conflict. Daily challenge tasks are always Focus-free, so streaks are never at risk |
| **Sparks** | No conflict. Sparks gate AI features. Focus gates activity volume. Two separate systems. |
| **Mock Tests** | No conflict. Mock tests cost 0 Focus. Always available. |
| **Leaderboard** | Focus indirectly limits daily Sparks earned from activities. A Freemium user at 0 Focus earns fewer activity-based Sparks that day. Premium users with no Focus limit can earn more. |
| **Games** | PVP games cost 2 Focus + Sparks entry fee. Solo games cost 1 Focus. Keeps games feeling special. |

---

## 7. Focus Achievements

| Achievement | Condition | Sparks |
| :--- | :--- | :---: |
| Full Focus | Use all 20 Focus in a single day | 30 |
| Iron Mind | Use 20 Focus every day for 7 consecutive days | 100 |
| Never Empty | End 14 consecutive days with at least 5 Focus remaining | 50 |
| Focus Saver | Reach maximum Focus (20) after a full passive recharge | 10 |

---

## 8. Admin Configuration

Super Admin can adjust without a code deploy (stored in a config table):

- Maximum Focus cap per role
- Focus cost per activity type
- Hourly regeneration rate
- Sparks cost to refill Focus
- Focus bonus per daily challenge completion
