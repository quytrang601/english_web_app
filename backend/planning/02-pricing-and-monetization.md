# 02 — Pricing and Monetization

---

## 1. Revenue Streams

The platform has four revenue streams:

| Stream | Who Pays | How |
| :--- | :--- | :--- |
| Premium Subscription | Freemium users who want unlimited access | Monthly or annual, via Stripe |
| Sparks Credit Packs | Freemium and Premium users who want more AI features | One-time purchases via Stripe |
| Ad-Free Pass | Freemium users who hate ads but won't subscribe | One-time purchase via Stripe |
| Ad Revenue | Freemium users (passive) | AdMob / AdSense rewarded and display ads |

---

## 2. Infrastructure Cost Analysis

Estimated monthly costs at **~500 active paying users**:

| Service | Cost/Month | Notes |
| :--- | :---: | :--- |
| Hosting (PostgreSQL 17, Redis 7, Spring Boot on cloud) | ~$80 | Single server + managed DB |
| Cartesia Sonic TTS (Startup plan) | $49 | 1.25M characters included |
| Deepgram Nova-3 STT (pay-per-use) | ~$24 | ~500 speaking sessions × $0.046 |
| Gemini API (writing scoring, embeddings) | ~$30 | ~5,000 scoring calls |
| AWS S3 (audio files, PDFs) | ~$10 | Storage + egress |
| **Total** | **~$193/month** | |

**Break-even:** ~20 Premium subscribers at $9.99/month covers all infrastructure.

---

## 3. Subscription Pricing

### 3.1 Tiers

| Tier | Price | What You Get |
| :--- | :--- | :--- |
| **Freemium** | Free | Core features with limits, earns Sparks, sees ads |
| **Premium Monthly** | See regional table | Full unlimited access, no ads, no Focus Bar |
| **Premium Annual** | ~38% off monthly | Same as monthly + 4+ months free |

Annual plan is always shown as **"Save X months"**, never as a percentage — it feels more concrete.

### 3.2 PPP-Adjusted Regional Pricing

| Country | Premium Monthly | Premium Annual | Notes |
| :--- | :---: | :---: | :--- |
| USA 🇺🇸 | $7.99 | $59.99 | Reference price |
| UK 🇬🇧 | £6.49 | £49.99 | |
| Vietnam 🇻🇳 | 99,000₫ | 699,000₫ | ~$3.87/mo — competitive with Duolingo |
| India 🇮🇳 | ₹299 | ₹2,299 | |
| Indonesia 🇮🇩 | Rp59,000 | Rp449,000 | |
| Philippines 🇵🇭 | ₱249 | ₱1,899 | |
| Thailand 🇹🇭 | ฿189 | ฿1,399 | |
| Japan 🇯🇵 | ¥799 | ¥5,999 | |
| South Korea 🇰🇷 | ₩7,900 | ₩59,900 | |
| UAE / Saudi Arabia 🇦🇪 | AED 27 | AED 199 | |
| Brazil 🇧🇷 | R$29.90 | R$219 | |
| Turkey 🇹🇷 | ₺149 | ₺1,099 | |
| Egypt 🇪🇬 | EGP 149 | EGP 1,099 | |
| Pakistan 🇵🇰 | PKR 1,199 | PKR 8,999 | |
| Bangladesh 🇧🇩 | BDT 499 | BDT 3,799 | |
| Malaysia 🇲🇾 | RM 14.90 | RM 109 | |

> Implementation: Stripe's built-in local currency support. We define base USD price + a regional multiplier table.

### 3.3 Competitor Landscape

| Competitor | Price (USD equiv.) | Notes |
| :--- | :---: | :--- |
| Duolingo Super (Vietnam) | ~$3–4/year | General English only, no IELTS-specific content |
| Magoosh IELTS | $59–$99/1–6 months | Video-only, no AI scoring, no gamification |
| E2Language | ~$50+/month | Live classes, very expensive |
| British Council IELTS Ready | Free with test booking | Limited to test buyers |
| IELTS Advantage | ~$99/year | Content only, no AI |

**Our positioning:** IELTS-specific AI scoring + gamification at ~Duolingo pricing in target markets. Every direct IELTS competitor charges 5–20x more.

---

## 4. Sparks Credit Packs

### 4.1 Base Pricing (USA)

| Pack | Sparks | Price (USD) | Cost if all on Writing AI | Margin |
| :--- | :---: | :---: | :---: | :---: |
| Starter Pack | 150 | $0.99 | ~$0.011 | ~99% |
| Value Pack | 400 | $2.49 | ~$0.030 | ~99% |
| Power Pack | 1,000 | $5.49 | ~$0.075 | ~99% |
| Mega Pack | 2,500 | $11.99 | ~$0.187 | ~98% |

> Sparks earned by watching ads or learning do not reduce these margins — they are earned Sparks, not purchased.

### 4.2 PPP-Adjusted Sparks Pack Pricing

| Pack | Sparks | USA | Vietnam | India | Indonesia | Philippines | Brazil |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| Starter | 150 | $0.99 | 19,000₫ | ₹79 | Rp14,000 | ₱49 | R$4.99 |
| Value | 400 | $2.49 | 49,000₫ | ₹199 | Rp35,000 | ₱129 | R$12.90 |
| Power | 1,000 | $5.49 | 109,000₫ | ₹449 | Rp79,000 | ₱279 | R$27.90 |
| Mega | 2,500 | $11.99 | 239,000₫ | ₹999 | Rp175,000 | ₱629 | R$59.90 |

**Bonus on all pack purchases:** +10% bonus Sparks (e.g., buy 150 → receive 165). Makes every purchase feel like a deal.

---

## 5. Ad-Free Pass

A one-time purchase to permanently remove all ads for Freemium users. Does not unlock any Premium features.

| Country | Price |
| :--- | :---: |
| USA 🇺🇸 | $1.99 |
| Vietnam 🇻🇳 | 39,000₫ |
| India 🇮🇳 | ₹99 |
| Indonesia 🇮🇩 | Rp29,000 |
| Philippines 🇵🇭 | ₱89 |
| Thailand 🇹🇭 | ฿59 |
| Japan 🇯🇵 | ¥249 |
| Brazil 🇧🇷 | R$9.90 |
| Turkey 🇹🇷 | ₺49 |

**Surfacing rule:** Only shown to users who have already watched at least 1 rewarded ad. Message: *"Love earning Sparks but hate the ads? Remove them forever."*

---

## 6. Ad Revenue (Freemium Only)

| Ad Type | Placement | Revenue Estimate |
| :--- | :--- | :--- |
| Rewarded Video Ad | User explicitly watches to earn Sparks or Focus | $0.01–$0.05 per view (eCPM varies by country) |
| Display / Banner Ad | Shown between exercises on result screens | $0.001–$0.005 per impression |

**Daily ad cap:** 5 rewarded ads per Freemium user per day.  
**GDPR / consent:** Ad SDK requires a consent dialog on first use. User can decline — they simply won't see rewarded ads (and cannot earn Sparks from ads).  
**Premium users:** No ads. Never. Ad SDK is not loaded for Premium accounts.

---

## 7. Stripe Payment Flow

```
User clicks "Upgrade" or "Buy Sparks"
  → Redirected to Stripe Checkout (hosted page)
  → No card data ever touches our servers
  → Payment processed by Stripe
  → Stripe sends webhook to our backend
  → We update user role / Sparks balance / subscription metadata
  → User redirected back to app with confirmation
```

**Webhook events handled:**

| Event | Action |
| :--- | :--- |
| `checkout.session.completed` | Grant Sparks / activate Premium / activate Ad-Free Pass |
| `invoice.payment_succeeded` | Renew Premium, extend `currentPeriodEnd` |
| `invoice.payment_failed` | Send warning email, start 3-day grace period |
| `customer.subscription.deleted` | Downgrade to Freemium immediately |

**Credits (Sparks) are non-refundable** once purchased, unless there is a documented technical error.

---

## 8. Discount Calendar

Discounts are frequent enough to feel rewarding, but not so frequent that users wait for them before buying.

| Event | Timing | Discount | Duration |
| :--- | :--- | :---: | :--- |
| Launch Promo | First 60 days post-launch | 50% off first month | 60 days |
| IELTS Test Season | Feb–Mar, Jul–Aug | 30% off any plan | 3 weeks |
| New Year | Jan 1–7 | 40% off annual | 7 days |
| Mid-Year Sale | Late June | 25% off any plan | 5 days |
| Regional Events | Tết, Diwali, Eid | 35% off any plan | 3 days |
| Flash Sale (random) | ~3–4 times/year | 40–50% off, 24 hours | 24 hours |
| 30-Day Streak Reward | User-specific trigger | 1 week Premium free | 48-hour window |
| Re-engagement | After 14+ days inactive | 50% off first month back | 7-day window |
| Birthday | User's birthday | 20% off any plan | 24 hours |
| Study Buddy | 2 friends sign up together | 25% off first 3 months each | 7-day window |

**Rules:**
- Never stack two discounts simultaneously
- Never discount during the same week as a previous discount
- Flash Sales are unannounced — scarcity is the mechanic

---

## 9. Key Discount Hooks ("Feels Like a Steal")

| Hook | Why It Works |
| :--- | :--- |
| Annual = "4+ months free" | Months-framing beats percentage-framing |
| First month at 50% off | Removes payment barrier for first-time buyers |
| Annual plan + 400 free Sparks | Tangible immediate reward on top of the saving |
| Student discount (20% off, verified .edu email) | Targets the core demographic |
| 30-day streak → 1 week free Premium popup | Converts the most engaged Freemium users at peak motivation |
| Refer a friend → both get 200 Sparks | Viral loop without direct money off |
