---
name: FitAI
description: Dark, high-energy fitness companion — personalized AI workout & nutrition tracking for Android and iOS (Compose Multiplatform).
colors:
  background: '#0D0D0D'
  on-background: '#E5E2E1'
  surface: '#1A1A1A'
  surface-variant: '#242424'
  on-surface: '#E0E0E0'
  on-surface-variant: '#BACAC2'
  primary: '#46F1C5'
  on-primary: '#00382B'
  primary-container: '#00A884'
  on-primary-container: '#0D0D0D'
  primary-light: '#5FFFD4'
  secondary: '#FF6B35'
  on-secondary: '#0D0D0D'
  tertiary: '#7B61FF'
  on-tertiary: '#E5E2E1'
  success: '#4CAF50'
  warning: '#FFB300'
  error: '#FF5252'
  on-error: '#E5E2E1'
  error-container: '#3D1414'
  on-error-container: '#FFDAD6'
  secondary-container: '#123D34'
  on-secondary-container: '#B9F8E6'
  surface-container-lowest: '#0A0A0A'
  surface-container-low: '#141414'
  surface-container: '#1A1A1A'
  surface-container-high: '#202020'
  surface-container-highest: '#242424'
  surface-bright: '#2E2E2E'
  outline: '#6B6B6B'
  outline-variant: '#2E2E2E'
  inverse-surface: '#E5E2E1'
  inverse-on-surface: '#1A1A1A'
typography:
  display-large:
    fontFamily: Sora
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
  display-medium:
    fontFamily: Sora
    fontSize: 34px
    fontWeight: '600'
    lineHeight: 40px
  display-small:
    fontFamily: Sora
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  headline-large:
    fontFamily: Sora
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-medium:
    fontFamily: Sora
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  headline-small:
    fontFamily: Sora
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 26px
  title-large:
    fontFamily: Lexend
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-medium:
    fontFamily: Lexend
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  title-small:
    fontFamily: Lexend
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  body-large:
    fontFamily: Lexend
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-medium:
    fontFamily: Lexend
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-small:
    fontFamily: Lexend
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-large:
    fontFamily: Lexend
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  label-medium:
    fontFamily: Lexend
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  label-small:
    fontFamily: Lexend
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
rounded:
  button: 12px
  card: 16px
  bottom-sheet: 24px
  chip: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 48px
  screen-padding: 24px
  section-gap: 16px
  card-padding: 16px
components:
  button-primary:
    backgroundColor: '{colors.primary}'
    textColor: '{colors.on-primary}'
    rounded: '{rounded.button}'
    padding: 12px 20px
  button-secondary:
    backgroundColor: '{colors.surface-variant}'
    textColor: '{colors.on-surface}'
    rounded: '{rounded.button}'
    padding: 12px 20px
  button-ghost:
    backgroundColor: transparent
    textColor: '{colors.primary-light}'
    rounded: '{rounded.button}'
  button-outlined:
    backgroundColor: transparent
    textColor: '{colors.primary}'
    rounded: '{rounded.button}'
  card-default:
    backgroundColor: '{colors.surface}'
    rounded: '{rounded.card}'
    padding: '{spacing.card-padding}'
  text-field:
    backgroundColor: transparent
    textColor: '{colors.on-background}'
    rounded: '{rounded.button}'
  navigation-bar:
    backgroundColor: '{colors.surface}'
    textColor: '{colors.on-surface}'
  trial-banner-active:
    backgroundColor: '{colors.surface}'
    textColor: '{colors.primary}'
  trial-banner-expired:
    backgroundColor: '{colors.surface}'
    textColor: '{colors.secondary}'
  macro-ring:
    backgroundColor: '{colors.surface-variant}'
    textColor: '{colors.primary}'
---

# Design System: FitAI

## Overview

FitAI is a dark, high-energy fitness companion that turns personal data into actionable AI workout and diet plans. The visual identity should feel like a premium training coach in your pocket: focused, motivating, and calm enough for daily logging — never neon-club loud or clinical gym-brochure sterile.

The atmosphere is **near-black canvases**, **mint-teal primary accents**, and **warm energy orange** for secondary motivation. Surfaces lift subtly above the void (`#1A1A1A` cards on `#0D0D0D` background). Progress and macros are glanceable; primary actions (start workout, log meal, continue trial) always read as the brightest interactive element on screen.

Source of truth in code: `core/design-system` → `FitAIColors`, `FitAISpacing`, `FitAIShapes`, `FitAITheme`, plus shared components (`FitAIButton`, `FitAICard`, `FitAITextField`, `MacroRing`, `TrialBanner`, `WorkoutPlanCard`, `MealLogCard`).

## Colors

The palette is **dark-mode only** (Material 3 `darkColorScheme`). Roles:

| Role | Hex | Use |
|------|-----|-----|
| Background | `#0D0D0D` | App canvas, scaffold |
| Surface | `#1A1A1A` | Cards, navigation bar, sheets |
| Surface variant | `#242424` | Secondary buttons, borders, ring track |
| Primary (mint teal) | `#46F1C5` | CTAs, selected tabs, progress arcs, focus rings |
| Primary dark | `#00A884` | Primary container / pressed depth |
| Primary light | `#5FFFD4` | Ghost button text, focused labels |
| Accent / Secondary | `#FF6B35` | Energy highlights, expired trial, motivational accents |
| Accent secondary / Tertiary | `#7B61FF` | Occasional tertiary emphasis (not primary CTA) |
| On-background | `#E5E2E1` | Titles, primary body |
| On-surface | `#E0E0E0` | Card body text |
| On-surface muted | `#BACAC2` | Subtitles, placeholders, inactive tabs |
| Surface steps | `#0A0A0A` / `#141414` / `#1A1A1A` / `#202020` / `#242424` / `#2E2E2E` | Material `surfaceContainer*` roles: lowest → bright. Neutral, no violet tint |
| Selected container | `#123D34` on `#B9F8E6` | `secondaryContainer`: selected chips, nav indicator |
| Outline / outline variant | `#6B6B6B` / `#2E2E2E` | Control borders, incl. unfocused text fields (≥3:1 on background and surface) / dividers |
| Success / Warning / Error | `#4CAF50` / `#FFB300` / `#FF5252` | Status only |

**Gradients (sparingly):**
- Primary: `#00D4AA` → `#00A884` (legacy gradient stops; `Primary` itself is `#46F1C5`) (hero cards, `FitAIGradientCard`)
- Energy: `#FF6B35` → `#FFB300` (motivation moments)
- Dark: `#1A1A1A` → `#0D0D0D` (subtle depth)

Do **not** invent purple-on-white or cream/serif marketing looks. Stay on the dark teal–orange identity.

## Typography

Material 3 scale, every role defined in `FitAITheme`: **Sora** (SemiBold/Bold) for display and headline roles, **Lexend** for title, body and label roles. A role missing from the theme silently falls back to the platform sans, so all 15 roles are set. Prefer `MaterialTheme.typography` tokens — never hardcode `fontSize` in feature screens.

| Token | Size / Weight | Role |
|-------|---------------|------|
| `displayLarge` | Sora 48 / Bold | Splash / brand moments |
| `displayMedium` | Sora 34 / SemiBold | Large welcome headlines |
| `displaySmall` | Sora 28 / SemiBold | Onboarding / paywall headlines |
| `headlineLarge` | Sora 24 / SemiBold | Screen titles |
| `headlineMedium` | Sora 20 / SemiBold | Section headers, brand mark |
| `headlineSmall` | Sora 18 / SemiBold | Sub-section headers |
| `titleLarge` | Lexend 20 / SemiBold | Stat values (kcal, streak days, weekly numbers) |
| `titleMedium` | Lexend 16 / SemiBold | Card titles, plan names |
| `titleSmall` | Lexend 14 / SemiBold | List-row titles (exercise names) |
| `bodyLarge` | Lexend 16 / Regular | Form fields, primary body |
| `bodyMedium` | Lexend 14 / Regular | Subtitles, secondary copy |
| `bodySmall` | Lexend 12 / Regular | Row meta (sets × reps, rest), helper text |
| `labelLarge` | Lexend 16 / SemiBold | Buttons |
| `labelMedium` | Lexend 12 / Medium | Small actions, field labels |
| `labelSmall` | Lexend 11 / Medium | Chips, “Active” badges, meta |

Hierarchy: one clear screen title → short muted subtitle → content cards. Stats (kcal, reps) should feel denser/weightier than supporting labels.

## Layout

- **Screen padding:** `lg` (24dp) horizontal/vertical on main scrolls (Home pattern).
- **Section gaps:** `md` (16dp) between cards/blocks; `xs`/`sm` inside cards.
- **Rhythm:** 4dp base (`xs` → `xxl`).
- **Touch:** Icon buttons 48dp; primary buttons padded 12×20 with 12dp radius.
- **Main shell:** Bottom `NavigationBar` on `Surface`; selected tab uses Primary, inactive uses muted.
- **Adaptive:** Use window size class / adaptive padding — never override `LocalDensity` for “scaling”.
- **One job per screen region:** Today’s workout, macros, trial status, and CTAs should not compete in a dense dashboard mashup.

## Elevation & Depth

Elevation is **subtle**, not skeuomorphic:

1. Background void (`#0D0D0D`)
2. Raised cards (`#1A1A1A`, ~2dp elevation or flat with surface contrast)
3. Bordered variant: 1dp `SurfaceVariant` outline (`FitAIBorderedCard`)
4. Gradient cards for rare hero emphasis (`FitAIGradientCard`)

Avoid multi-layer glam shadows, neon glow stacks, or glassmorphism frosted panels. Depth comes from **surface steps + teal accents**.

## Shapes

| Token | Radius | Applied to |
|-------|--------|------------|
| Button / text field | 12dp | `FitAIButton`, `FitAITextField` |
| Card | 16dp | `FitAICard`, plan/meal cards |
| Bottom sheet | 24dp top corners | Sheets / large overlays |
| Chip | Full pill | Status chips, filters |

Keep corners consistent — do not mix sharp rectangles with heavy pills on the same screen.

## Components

### Buttons (`FitAIButton`)

- **Primary:** Mint fill `#46F1C5`, text `#00382B` — main actions (Start workout, Continue, Save).
- **Secondary:** Surface-variant fill, on-surface text — alternate actions.
- **Ghost:** Transparent, primary-light text — tertiary / text links.
- **Outlined:** 1dp primary border, primary text (`FitAIOutlinedButton`).
- Disabled: surface-variant + muted text.
- Icon buttons: 48dp, primary tint.

### Cards

- **FitAICard:** Surface fill, 16dp radius, 16dp padding — default content container.
- **FitAIGradientCard:** Primary gradient — use sparingly for hero/plan highlights.
- **FitAIBorderedCard:** Surface + variant border — quieter grouping.
- Domain cards: `WorkoutPlanCard`, `MealLogCard` — title + muted subtitle + optional Active label in primary.

### Inputs (`FitAITextField`)

Outlined field, button radius (12dp). Focused border/label → primary / primary-light; unfocused → outline `#6B6B6B` / muted; disabled → surface-variant. Labels are single-line; when a field is too narrow for its label, use a column header above the fields (active-session set rows). Error → `#FF5252`. Full width by default.

### Fitness-specific

- **MacroRing:** 96dp arc, 10dp stroke; track = surface-variant, progress = primary; kcal + protein labels beside ring.
- **TrialBanner:** Card with primary text when active, accent orange when expired.
- **SkeletonLoader:** Loading placeholders matching card rhythm (no spinner-only empty voids when content structure is known).

### Navigation

Bottom bar on Surface. Five tabs: Home, Workout, Nutrition, Progress, Profile. Selected = primary; disabled/placeholder (e.g. Progress) = muted at reduced opacity.

## Do's and Don'ts

**Do**
- Match Stitch / this file for colors, padding, and type exactly when implementing screens.
- Reuse `core/design-system` components and `FitAI*` tokens.
- Localize all UI strings (`LocalStrings` / composeResources) — no hardcoded user-facing text.
- Keep primary CTA singular and teal-forward per screen.
- Prefer glanceable fitness data (macros, today’s plan, session status).

**Don't**
- Invent new hex colors or one-off radii in feature modules.
- Use light/cream backgrounds or purple-indigo default AI themes.
- Pass ViewModels into child composables — hoist state and callbacks.
- Hardcode `fontSize` / ignore spacing tokens.
- Clutter the first viewport with stats strips, chip clouds, and competing banners.
- Put API keys or AI secrets in client UI layers.

## Product surfaces (for generation)

When generating screens, stay within FitAI product scope:

| Surface | Intent |
|---------|--------|
| Splash / Welcome / Onboarding | Brand-first, collect profile, low friction |
| Auth / Paywall / Trial | Clear path: guest trial → auth → premium |
| Home | Today workout + macros + trial + quick actions |
| Workout | Plan list → exercise detail → active session |
| Nutrition | Diet plan, meal log, photo check-in |
| Progress | Visual / measurement progress (placeholder-aware) |
| Profile | User settings and account |

Tone of copy in designs: encouraging, concise, action-oriented (TR/EN ready) — coach, not corporate.
