# SIPPY — Technical Specification & Invariants

Companion to `PLAN.md` (roadmap & decisions), `PROGRESS.md` (round logs & root causes), and `shared/tokens.md` (design tokens).

## 1. Backdrop & Surface Architecture
- **Single fixed backdrop:** One fixed full-viewport `.backdrop` layer (mounted as the first child of `#root`, outside `.app-shell`) owns the base tone (`--backdrop-base`), feathered hero caustics, procedural aurora canvas, ambient droplets, and top hero scrim. Content sections are transparent and never clip or paint independent section backgrounds.
- **Backdrop-filter budget:** Keep simultaneous `backdrop-filter` surfaces at **≤ 6 per viewport**. Interactive sub-controls inside glass cards sit on the card's single `GlassSurface` without nested `backdrop-filter` layers.
- **Capabilities & flags:** `?fx=blur|full` overrides refraction capability detection; `?intro=1|0` forces or skips the intro; `?debug=outline|perf|both` activates layout overflow inspection and frame-time logging.

## 2. Water Orb Motion Rule (`HeroWaterRing` / `.hr*`)
- **Transform and opacity only:** Every animation inside the hero water orb (`HeroWaterRing` and all `.hr*` elements/pseudo-elements) **must animate `transform` and `opacity` only**. Never animate layout properties (`top`, `left`, `width`, `height`, `margin`), paint properties (`background`, `background-position`, `border-color`), `box-shadow`, or `filter`.
- **Single progress variable:** Fill saturation and inner glow intensity are driven by one CSS custom property (`--hr-progress`, `0` to `1`) mapped purely onto layer `opacity` and `transform: scale(...)`.
- **Surface, bubbles, cycle & accessibility:**
  - Two tileable SVG wave layers drift horizontally via `transform: translateX(...)` at distinct speeds and amplitudes.
  - At most **8** rising bubble elements exist inside the water volume, animated via `transform` and `opacity` only.
  - Cycle: ~4.5 s rise (`0 → 100%`, decelerating near the top) → 1.5 s hold at `100%` (`"Goal reached"` crossfade + ~600 ms surface splash ring + gentle damped `rotate` slosh) → smooth drain and restart.
  - Pauses when off-screen (`IntersectionObserver`).
  - Under `prefers-reduced-motion: reduce` (or `[data-emulate-motion]`), renders a static fill at the current percentage with no waves, bubbles, or splash and no `requestAnimationFrame` loop.

## 3. Surface-Break Intro Invariants
- **No pure black (`#000`):** Neither the overlay, CSS keyframes, nor the WebGL canvas may paint `#000000` or clear to transparent black `(0, 0, 0, 0)`. The darkest frame is the page's darkest backdrop tone (`--backdrop-base`), and the WebGL context clears to that tone at `alpha = 1`.
- **Seamless exit:** The page `<Backdrop />` renders underneath from frame 0. The reveal hole opens from the mark's exact centre; the final 800 ms crossfade brings overlay `opacity` to `0` while hero words rise blur-to-sharp, and the overlay unmounts only after its `opacity` reaches `0`. Total timeline ≤ `4500 ms`.
- **Reduced motion:** Plays a 500 ms crossfade from the mark to the page with no shader, no WebGL context, no ring, and no black frame.

## 4. Accessibility & Contrast
- Every text label (CTAs, body copy, captions, footnotes, and interactive card readouts) maintains **≥ 4.5:1** WCAG 2.x contrast against its real composited background in both dark and light themes. When a CTA over glass falls short, darken the glass tint behind the label rather than darkening white CTA text.
- Every interactive control is keyboard-reachable with a visible `2px solid var(--accent-bright)` `:focus-visible` ring.
