# Sippy Design Tokens v1 (source of truth)

Single source consumed by: website (Tailwind 4 `@theme` + `tokens.css`) and app (Compose `Color.kt`/`Dimens.kt`/`Motion.kt` in Phase 4a).
Theme model: **dark is the hero**, light fully finished. One accent hue (Sippy Blue) + neutrals. User-customizable accent (Phase 4b) swaps the `--accent*` set only.

## Colour

### Dark theme (hero)

| Token | Hex / value | Use |
|---|---|---|
| `--bg-0` | `#05070D` | page base (near-black, blue-shifted) |
| `--bg-1` | `#0A101C` | raised base |
| `--bg-2` | `#111A2E` | elevated base |
| `--text-1` | `#F4F7FF` | primary text (≈19:1 on bg-0) |
| `--text-2` | `#A9B4CE` | secondary (≈8:1 on bg-1) |
| `--text-3` | `#74819E` | muted / captions |
| `--accent` | `#2E7CF6` | Sippy Blue (UI fills) |
| `--accent-bright` | `#5B9BFF` | accents on dark, focus ring (≈6.5:1 on bg-0) |
| `--accent-deep` | `#1D5FE0` | accent text on light (AA) |
| `--accent-glow` | `rgba(46,124,246,0.45)` | glows, shadows tint |
| `--ok` | `#3ECF8E` | success |
| `--warn` | `#F5B84D` | warnings (sugary drink, etc.) |
| `--danger` | `#F26D6D` | errors |

Glass (dark):
- `--glass-tint`: `rgba(255,255,255,0.08)` (brief: 6–10% white ✓)
- `--glass-rim-start`: `rgba(255,255,255,0.40)` · `--glass-rim-mid`: `rgba(255,255,255,0.05)` · `--glass-rim-end`: `rgba(255,255,255,0.18)` — 1px gradient rim, brighter top-left
- `--glass-inner`: `rgba(255,255,255,0.10)` (inner top highlight)
- `--glass-shadow`: `0 1px 2px rgba(0,0,0,.28), 0 8px 24px rgba(0,0,0,.38), 0 28px 72px rgba(0,0,0,.42)`

### Light theme

| Token | Value |
|---|---|
| `--bg-0` | `#EDF2FB` |
| `--bg-1` | `#FFFFFF` |
| `--bg-2` | `#E1E9F8` |
| `--text-1` | `#0C1424` (≈17:1 on bg-1) |
| `--text-2` | `#43506B` |
| `--text-3` | `#6C7A97` |
| `--accent` | `#2E7CF6` (UI only — 4.0:1 on white, large/UI elements) |
| `--accent-bright` | `#1D5FE0` (inverted role: text-grade accent on light) |
| `--accent-deep` | `#1749B8` |
| `--accent-glow` | `rgba(29,95,224,0.25)` |

Glass (light):
- `--glass-tint`: `rgba(255,255,255,0.52)` (brief: 40–55% white ✓)
- `--glass-rim-start`: `rgba(255,255,255,0.95)` · `--glass-rim-mid`: `rgba(18,42,84,0.10)` · `--glass-rim-end`: `rgba(255,255,255,0.55)`
- `--glass-inner`: `rgba(255,255,255,0.60)`
- `--glass-shadow`: `0 1px 2px rgba(16,32,64,.10), 0 8px 24px rgba(16,32,64,.14), 0 28px 72px rgba(16,32,64,.16)`

**Rule:** text on glass must keep WCAG AA; when the backdrop behind a glass surface is busy, add a scrim (`rgba(0,0,0,.35)` dark / `rgba(255,255,255,.5)` light) behind the text block.

## Shape

| Token | Value | Use |
|---|---|---|
| `--r-pill` | `999px` | pills, buttons, chips, tab bars, inputs |
| `--r-card` | `28px` | cards, sheets, nav |
| `--r-inner` | `20px` | inner elements (inputs in cards, wells) |

No hard corners anywhere. Continuous corners on the app where `ContinuousRoundedCornerShape`/kyant shapes support them.

## Glass physics

| Token | Value |
|---|---|
| `--blur-sm` / `--blur-md` / `--blur-lg` | `24px` / `32px` / `40px` |
| `--glass-saturation` | `165%` |
| `--grain-opacity` | `0.03` (SVG feTurbulence, baseFrequency 0.9, mix-blend overlay) |
| Refraction | web: SVG displacement inside `backdrop-filter`, Chromium-only progressive enhancement, blur-only fallback elsewhere · app: `lens()` T1, none T2/T3 |
| Max simultaneous backdrop-filter surfaces per viewport | **6** |

## Motion

Springs (motion/react `SpringOptions`; Compose `spring(stiffness, damping, mass)` equivalents):

| Preset | stiffness | damping | mass | Use |
|---|---|---|---|---|
| `spring-base` | 300 | 30 | 1 | default presses, toggles |
| `spring-gentle` | 220 | 24 | 1 | sheets, big panels |
| `spring-snappy` | 500 | 38 | 1 | tab lens, small chips |
| `spring-float` | 120 | 16 | 1.2 | idle float, mascot breathing |

Durations (non-spring): quick 180ms · base 300ms · slow 450ms · intro max 4500ms (intro + ambient only).
Rules: springs only (no plain ease); nothing >600ms except intro/ambient; animate transform+opacity only; press = scale 0.97 + refraction bulge, spring back; `prefers-reduced-motion` → opacity-only or static; `prefers-reduced-transparency` → opaque fallback.

## Type (Inter variable, bundled)

| Token | Value | Use |
|---|---|---|
| `--font-body` | Inter Variable 400–600 | body 16px (mobile 17px), UI 15px |
| `--font-display` | Inter Tight 600–700, `letter-spacing: -0.025em` | headlines |
| `--fs-display` | `clamp(3rem, 7.5vw, 7.5rem)` (48–120px) | hero |
| `--fs-h1` | 3rem | section heads |
| `--fs-h2` | 2.25rem | card heads |
| `--fs-h3` | 1.375rem | subheads |
| `--fs-caption` | 0.8125rem | captions, meta |

Big numerals: Inter Tight **300–400** (light weights, per brief).

## Space / Z / Misc

- Spacing scale: 4 · 8 · 12 · 16 · 24 · 32 · 48 · 64 · 96 px
- Z: backdrop 0 · content 10 · floating nav 40 · sheets 60 · toasts 80
- Focus ring: `2px solid var(--accent-bright)`, offset 2px, on every interactive
- Touch targets (app): min 48dp
- Icon set: single rounded-stroke set, 1.75px stroke (app: 2dp), 24dp grid

## App quality tiers (Phase 4)

| Tier | When | Rendering |
|---|---|---|
| T1 | API 33+ (and not low-RAM) | Kyant0 `drawBackdrop`: `vibrancy() + blur + lens` + Highlight + Shadow |
| T2 | API 31–32 | Haze `hazeBlur`/`hazeGlass` (blur glass, no custom refraction) |
| T3 | API < 31, low-RAM, or Settings→"reduce effects" | static frosted: tint + gradient + noise (no runtime backdrop) |
