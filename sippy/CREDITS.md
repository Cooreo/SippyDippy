# Sippy — Credits & Licences

## Code libraries
| Library | Licence | Source | Version used |
|---|---|---|---|
| Backdrop (Liquid Glass for Compose) | Apache-2.0 | github.com/Kyant0/AndroidLiquidGlass | 2.0.1 |
| kyant shapes | Apache-2.0 | github.com/Kyant0 (shapes) | 1.2.1 |
| Haze (backdrop blur/glass) | Apache-2.0 | github.com/chrisbanes/haze | 2.1.0 |
| Glin UI (React components, ShimmerButton) | MIT | glinui.com / github.com/GLINCKER/glinui | 0.1.1 |
| fluidkit (liquid UI animations) | MIT | runvendo.github.io/fluidkit | 0.5.0 |

Notes:
- Apache-2.0 libraries: copyright notices retained in `third-party/` sources and in the shipped APK (Gradle bill-of-materials) — no vendoring of Backdrop/Haze source needed (both are on Maven Central).
- MIT libraries: attribution above satisfies the MIT notice requirement.

## Fonts
- **Inter** & **Inter Tight** (variable) — SIL Open Font Licence 1.1 — bundled via `@fontsource-variable/*` (site) and `com.google.android.fonts`/res (app, Phase 4). No CDN.

## Imagery
- `shared/backdrops/landscape-dusk-emerald.jpg` — AI-generated 2026-11-26. No third-party rights.
- `shared/backdrops/blobs-dark.jpg` — AI-generated 2026-11-26. No third-party rights.
- **Sippy asset pack (Phase 3 richness pass, AI-generated 2026-11-27)** — `site/public/assets/*.webp`
  (hero caustics backdrop; Dippy set: happy/dancing/sleepy/waving; friends:
  plant sprout & bloom, pet cub & adult; droplet sprite; ripple ring;
  section backdrops features/showcase/download). Style: glossy translucent
  water-droplet character, soft studio lighting, subtle rim light, Sippy
  Blue #2E7CF6 with violet accents, clean 3D render style, no text, no
  watermark. Generated with Arena's image generation; no third-party rights.
  Source PNGs are gitignored (`.assets-src/`); production WebP + alpha
  keying via `site/tooling/build-assets.py`. Review: `/#/assets`.
- Logo, favicon, app icon — original vector artwork for Sippy (this repo).
- All other website backdrops are procedural (canvas gradients) — no stock photos, no watermarks, no hotlinks.
