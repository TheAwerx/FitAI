# FitAI — Main app screens (bottom navigation) for Stitch

Design a cohesive **premium dark mobile UI** (portrait phone) for **FitAI**, an AI fitness companion. These screens live inside a **bottom navigation bar** with 5 tabs. Each tab has **one main purpose**. Some tabs have **sub-screens** (push/stack). Keep visual identity consistent with FitAI DESIGN.md.

---

## Global shell (every main tab)

### Bottom navigation
Tabs (left → right):
1. **Home**
2. **Workout**
3. **Nutrition**
4. **Progress**
5. **Profile**

Rules:
- Bar background: `#1A1A1A` (Surface)
- Selected tab icon + label: mint teal `#00D4AA`
- Unselected: muted `#9E9E9E`
- No emoji-as-icons; use clean line icons (home, dumbbell, fork/plate, chart, person)
- Hide or simplify bottom bar on deep sub-screens if needed (Active Session, Meal Log, Exercise Detail) — prefer **keep bar on tab roots only**; sub-screens use a **Back** affordance in a top bar

### Visual system (strict)
- Canvas `#0D0D0D`, cards `#1A1A1A`, elevated `#242424`
- Primary CTA / progress / selection: `#00D4AA` (on-primary `#0D0D0D`)
- Energy accent `#FF6B35` (trial expired, alerts — not main CTA)
- Text `#F5F5F5` / `#E0E0E0`, muted `#9E9E9E`
- Radii: buttons 12px, cards 16px, chips pill
- Screen padding ~24px; section gaps ~16px
- Mood: personal coach — focused, energetic, calm for daily logging
- Avoid: purple AI gradients, cream/serif lifestyle, neon glow stacks, cluttered chip clouds, multi-stat hero dashboards

### UX principles
- **One job per screen**
- One primary teal CTA per screen
- Glanceable fitness data
- English UI copy OK; short, coach-like
- Touch targets ≥48px

---

## 1) HOME — “Today at a glance”

### Main purpose
Daily command center: **today’s workout + today’s macros + trial status + quick actions**. Not a full workout editor and not a full meal diary.

### Screen: Home (tab root)
**Layout (top → bottom):**
1. **Header** — Title “Home” / greeting (“Good morning, Alex”), muted subtitle “Today’s summary”
2. **Trial banner** (conditional card)
   - Active: teal text “Trial: 2 days left”
   - Expired: orange accent “Trial ended — unlock FitAI”
   - Tapping expired may go to paywall
3. **Today’s workout** section
   - Section title
   - If plan exists: card with plan title, “Wednesday · 5 exercises”, “Active” teal label
   - Compact exercise list (name + sets×reps), max ~4–5 rows; “See all” if more → Workout tab
   - Empty: muted “No workout scheduled today”
   - Primary CTA: **Start today’s workout** → Workout Active Session
4. **Daily macros** section
   - Section title
   - **Macro ring** (96dp+): calories consumed/target arc in teal; protein line beside (e.g. `P: 84 / 160 g`)
   - Optional small kcal / protein / carbs / fat mini stats under ring (keep sparse)
5. **Quick action** — full-width primary or secondary: **Log meal** → Nutrition Meal Log

**States to design:** loading skeletons; empty (no plan); active day with data; trial active; trial expired.

**Do not put on Home:** full week day picker, full meal history, auth forms, charts.

**Sub-screens:** none dedicated (navigates into Workout / Nutrition).

---

## 2) WORKOUT — “Train from your AI plan”

### Main purpose
Browse and run the AI workout plan: plan overview → exercise detail → live session logging.

### Screen A: Workout hub (tab root)
**Job:** show active plan, pick day, list exercises, start session.

**Layout:**
1. Title “Workout”
2. **Active plan card** — title, short description, “Active” badge
3. Secondary button: **Regenerate plan** (AI refresh)
4. **Day selector** — 7 chips (Mon–Sun); selected = teal
5. **Exercise list** for selected day — each row a card/row:
   - Exercise name
   - `3 sets × 8–12` · muscle group
   - Rest `90s` muted
   - Chevron → Exercise Detail
6. Sticky/primary bottom CTA: **Start workout** (enabled if day has exercises)
7. Optional **Past plans** section (collapsed list of inactive plan cards)

**Empty state:** “No workout plan yet” + primary **Generate plan**.

### Screen B: Exercise Detail (sub-screen)
**Job:** understand one exercise before/during training.

**Layout:**
1. Top bar: Back
2. Large exercise name
3. Muscle group chip/label
4. Specs card: sets, reps, rest seconds
5. Notes / form cues (muted body text)
6. Optional illustration/placeholder for movement (dark surface, not stock photo collage)
7. Secondary CTA optional: **Start workout including this day** (or omit if Start lives only on hub)

**No** set logging here — that belongs to Active Session.

### Screen C: Active Session (sub-screen)
**Job:** log the live workout — complete sets with actual reps + weight.

**Layout:**
1. Top bar: Back + title “Active workout” + optional elapsed time
2. Progress: “Exercise 2 of 5” or thin progress bar
3. Per exercise block:
   - Exercise name
   - For each set row: checkbox/complete control, “Set 1”, target reps hint, inputs for **Reps** and **Weight (kg)**
4. Rest hint between sets (optional timer UI — simple, teal)
5. Primary CTA: **Finish workout**
6. Success moment optional (brief completion state) then return to Workout hub

**Focus mode:** keep chrome minimal; large inputs; one exercise visually emphasized at a time if possible.

---

## 3) NUTRITION — “Hit today’s macros”

### Main purpose
See diet targets, today’s meals, and log food (manual or photo AI).

### Screen A: Nutrition hub (tab root)
**Job:** daily nutrition dashboard.

**Layout:**
1. Title “Nutrition”
2. **Daily targets** — Macro ring (calories vs target) + protein; card with `kcal · P/C/F targets`
3. Actions row/stack:
   - Primary: **Log meal** → Meal Log
   - Secondary: **Photo meal** → camera/gallery → analyzing → Meal Log prefilled
   - Ghost/secondary: **Regenerate diet plan**
4. Section **Today’s meals** — list of meal cards:
   - Meal name
   - Calories
   - `P · C · F` line
5. Empty meals: muted “No meals logged yet” + CTA Log meal
6. Analyzing state: “Analyzing your meal…” with subtle progress when photo AI runs

### Screen B: Meal Log (sub-screen)
**Job:** add/edit one meal entry.

**Layout:**
1. Top bar: Back · title “Log meal”
2. Fields (one column):
   - Meal name
   - Calories
   - Protein (g)
   - Carbs (g)
   - Fat (g)
3. If opened from photo AI: show small photo thumbnail + “AI estimate — edit if needed”
4. Primary CTA: **Save meal**
5. Validation errors in red `#FF5252` under fields

### Screen C (optional but recommended): Photo capture / analyze
Simple dark full-screen: camera preview or “Choose photo”, teal shutter/confirm, then analyzing overlay. Can be a system sheet — if designing in Stitch, show a polished in-app variant.

---

## 4) PROGRESS — “See how you’re changing”

### Main purpose
Longitudinal progress (weight, measurements, consistency) — motivate with clear trends. (Currently placeholder in app; design the real destination.)

### Screen A: Progress hub (tab root)
**Job:** overview of progress at a glance.

**Layout:**
1. Title “Progress”
2. **Hero metric card** — current weight + delta (“−1.2 kg this month”), teal positive / orange caution as needed
3. **Time range chips:** 1W · 1M · 3M · All (selected teal)
4. **Weight trend chart** — simple line/area on dark card; teal stroke; minimal axes
5. **Consistency strip** — workouts completed this week (e.g. 3/5) + meals logged days
6. **Measurements summary** — waist / chest / etc. as compact rows (optional)
7. Primary CTA: **Log weight** → Log Weight sub-screen
8. Secondary: **Add photo check-in** (optional future) or **Add measurement**

**Empty state:** “Log your first weigh-in to start tracking” + CTA.

### Screen B: Log Weight (sub-screen)
**Job:** enter today’s weight.

**Layout:** Back · title · large numeric weight (kg) · date · Save (teal).

### Screen C: Progress detail / history (sub-screen, optional)
List of past weigh-ins or measurement entries; swipe/delete optional; keep sparse.

**Do not** turn Progress into Home — no today’s workout list or meal logger here.

---

## 5) PROFILE — “You & your account”

### Main purpose
View profile used for AI plans, manage auth/sync, sign out. Settings, not daily logging.

### Screen A: Profile hub (tab root)
**Layout:**
1. Title “Profile” (center top bar OK)
2. **Identity card** — avatar placeholder, name, email (if any), goal + activity summary
3. **Body metrics row** — age · height · weight (editable later)
4. **Preferences card** — equipment, diet restrictions (chips or text)
5. **Trial / subscription** status chip or banner
6. Section **Account**
   - Sign in (email)
   - Create account
   - Continue with Google
   - Continue with Apple
   - If signed in: show account email/uid in teal muted + **Sign out** (ghost)
7. Hint text: cloud sync / backup when signed in
8. Optional rows: Edit profile → Edit Profile; Language; Notifications (placeholders OK)

### Screen B: Edit Profile (sub-screen, recommended)
Edit the same fields AI uses: name, age, height, weight, goal, activity, equipment, diet restrictions. Save (teal). Note: saving may imply plan regeneration later — show calm helper text.

### Screen C: Auth (can be modal / stack from Profile)
Sign in & Register forms (email/password) — dark FitAI fields, primary Continue. Keep aligned with existing Auth screen patterns. Not a tab root.

---

## Navigation map (for Stitch consistency)

```
Bottom Nav
├── Home
│     → (Start workout) Workout / Active Session
│     → (Log meal) Nutrition / Meal Log
│     → (Trial expired) Paywall
├── Workout
│     ├── Exercise Detail
│     └── Active Session
├── Nutrition
│     ├── Meal Log (manual or AI-prefilled)
│     └── Photo capture / analyze (optional)
├── Progress
│     ├── Log Weight
│     └── History / detail (optional)
└── Profile
      ├── Edit Profile
      └── Auth (Sign in / Register)
```

---

## Deliverable for Stitch

Generate **high-fidelity** designs for:

**Must-have**
1. Home
2. Workout hub
3. Exercise Detail
4. Active Session
5. Nutrition hub
6. Meal Log
7. Progress hub
8. Profile hub

**Strongly recommended**
9. Log Weight
10. Edit Profile
11. Nutrition photo analyzing state
12. Empty + loading variants for Home / Workout / Nutrition / Progress

Show the **bottom nav** on all tab roots with the correct tab selected. Sub-screens: Back + no competing primary nav chrome.

Make the set feel like **one product**: same dark teal–orange FitAI language, one main purpose per screen, premium coach aesthetic.
