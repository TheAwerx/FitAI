---
name: FitAI
description: Dark, high-energy fitness companion — personalized AI workout & nutrition tracking for Android and iOS (Compose Multiplatform).
colors:
  background: '#0D0D0D'
  on-background: '#F5F5F5'
  surface: '#1A1A1A'
  surface-variant: '#242424'
  on-surface: '#E0E0E0'
  on-surface-variant: '#9E9E9E'
  primary: '#00D4AA'
  on-primary: '#0D0D0D'
  primary-container: '#00A884'
  on-primary-container: '#F5F5F5'
  primary-light: '#5FFFD4'
  secondary: '#FF6B35'
  on-secondary: '#0D0D0D'
  tertiary: '#7B61FF'
  on-tertiary: '#F5F5F5'
  success: '#4CAF50'
  warning: '#FFB300'
  error: '#FF5252'
  on-error: '#F5F5F5'
  outline: '#242424'
  outline-variant: '#242424'
  inverse-surface: '#F5F5F5'
  inverse-on-surface: '#0D0D0D'
typography:
  display-large:
    fontFamily: Sans Serif
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
  display-medium:
    fontFamily: Sans Serif
    fontSize: 34px
    fontWeight: '600'
    lineHeight: 40px
  headline-large:
    fontFamily: Sans Serif
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-medium:
    fontFamily: Sans Serif
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-medium:
    fontFamily: Sans Serif
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-large:
    fontFamily: Sans Serif
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-medium:
    fontFamily: Sans Serif
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-small:
    fontFamily: Sans Serif
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
| Primary (mint teal) | `#00D4AA` | CTAs, selected tabs, progress arcs, focus rings |
| Primary dark | `#00A884` | Primary container / pressed depth |
| Primary light | `#5FFFD4` | Ghost button text, focused labels |
| Accent / Secondary | `#FF6B35` | Energy highlights, expired trial, motivational accents |
| Accent secondary / Tertiary | `#7B61FF` | Occasional tertiary emphasis (not primary CTA) |
| On-background | `#F5F5F5` | Titles, primary body |
| On-surface | `#E0E0E0` | Card body text |
| On-surface muted | `#9E9E9E` | Subtitles, placeholders, inactive tabs |
| Success / Warning / Error | `#4CAF50` / `#FFB300` / `#FF5252` | Status only |

**Gradients (sparingly):**
- Primary: `#00D4AA` → `#00A884` (hero cards, `FitAIGradientCard`)
- Energy: `#FF6B35` → `#FFB300` (motivation moments)
- Dark: `#1A1A1A` → `#0D0D0D` (subtle depth)

Do **not** invent purple-on-white or cream/serif marketing looks. Stay on the dark teal–orange identity.

## Typography

Sans-serif Material 3 scale. Prefer `MaterialTheme.typography` tokens — never hardcode `fontSize` in feature screens.

| Token | Size / Weight | Role |
|-------|---------------|------|
| `displayLarge` | 48 / Bold | Splash / brand moments |
| `displayMedium` | 34 / SemiBold | Large welcome headlines |
| `headlineLarge` | 24 / SemiBold | Screen titles |
| `headlineMedium` | ~20 / SemiBold | Section headers |
| `titleMedium` | ~16 / SemiBold | Card titles, plan names |
| `bodyLarge` | 16 / Regular | Form fields, primary body |
| `bodyMedium` | 14 / Regular | Subtitles, secondary copy |
| `labelSmall` | 11 / Medium | Chips, “Active” badges, meta |

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

- **Primary:** Teal fill `#00D4AA`, text on dark `#0D0D0D` — main actions (Start workout, Continue, Save).
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

Outlined field, button radius (12dp). Focused border/label → primary / primary-light; unfocused → surface-variant / muted. Error → `#FF5252`. Full width by default.

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
