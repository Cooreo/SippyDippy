# PROGRESS — Sippy

Running log: done / verified / broken / next. Last updated: 2026-11-26.

## Phase 0 (plan) — ✅ approved with changes
Old repo: empty (README only). Toolchain: Node 22 only — no Java/SDK/browser/KVM. Network: npm + GitHub API + codeload only (all Maven/Google hosts blocked) → APK built by user (Option A, agreed). See PLAN.md v2.

## Phase 1 (foundations) — ✅ approved ("preview looks good")
Library research (real sources), tokens v1 (`shared/tokens.md`), logo, adaptive icon XML, 2 backdrops, aurora spec, Inter variable fonts bundled, `shared/docs/api-notes.md`, design-language preview page. All served & 200-verified.

## Phase 2 (site design system & shell) — ✅ approved (2026-11-26, Phase 3 brief received)

### Done
- **Verified real type definitions before coding** (agreement #5): unpacked `fluidkit@0.5.0` (`dist/index.d.ts`: LiquidTabs/LiquidSwitch/LiquidSlider/LiquidProgress/LiquidToastProvider/toast/MeshGradient/SurfaceStyleProps/FluidTheme; note LiquidTabs & LiquidSwitch omit `refraction`; `supportsRefraction`/`supportsBackdropFilter` helpers), `@glinui/ui@0.1.1` (ShimmerButton: `variant="glass"`, `shimmerColor`, `shimmerDuration`; Button; GlassCard; Chip; Input; GlassNavbar with built-in scroll elevation), `@glinui/tokens` theme.css (all variable names + light/dark values → I override them with Sippy values).
- **Scaffold** (installed & resolved): Vite 7.3.8, React 19.3.1, TS 5.8.3, Tailwind 4.3.5 (@tailwindcss/vite), motion 12.43.0, lenis 1.3.29, fluidkit 0.5.0, @glinui/ui 0.1.1, @glinui/tokens 0.1.1, eslint 9.39.5, vitest 3.2.7, @testing-library/react 16.
- **Styles**: `src/styles/index.css` — Sippy tokens (mirror of tokens.md), Tailwind 4 `@theme inline` mapping, Glin token-value overrides (their components wear Sippy palette/blur/glass), full glass recipe (.glass + rim + grain + fallbacks), component CSS (nav pill, capsule field, icon buttons, lyric glow, matrix, gallery), reduced-motion + reduced-transparency rules (real media queries AND `[data-emulate-*]` mirrors for the gallery switch).
- **Components**: `GlassSurface` (blur 24/32/40, radii, cursor specular, refraction gated by fx), `GlassCard`, `GlassButton` (Glin ShimmerButton glass, per-theme shimmer), `GlassIconButton` (48px), `GlassChip` (Glin Chip glass), `GlassToggle` (fluidkit LiquidSwitch), `GlassTextField` (capsule), `GlassToast` (fluidkit LiquidToastProvider + toast), `GlassTabBar` (fluidkit LiquidTabs, flow stretch/slide, scene light), `GlassNav` (reference 3: floating pill, sun/moon liquid switch, white pill CTA), `AuroraBackdrop` (canvas per spec), `RefractionDefs` (SVG displacement filter), 15-icon rounded-stroke set (1.75px).
- **Libs**: `tokens.ts` (JS mirror, test-guarded), `fx.ts` (`?fx=blur|full` + runtime override + feature detection + React store), `router.ts` (`/` + `/gallery` via path OR `#/gallery` hash, zero deps), `theme.tsx` (dark hero, system-aware, persisted), `effects.tsx` ("reduce effects" = emulates reduced-motion + reduced-transparency + forces blur fx).
- **Pages**: `HomePage` (Phase 2 shell — honest status, no fake marketing copy) + `GalleryPage` (lazy-loaded): backdrop selector (aurora/meadow/blobs/flat-trap/busy), theme + reduce-effects switches, live fx pill, fixed 2×4 matrix (light & dark × 4 backdrops), every component over the chosen backdrop, reference-3 photo section with live nav, reference-5 dark music-style card (artwork, pill transport, glowing lyric line, glass tab bar).
- **Perf**: vendor chunk splitting (react/motion/fluid/glin) + gallery lazy-loaded → home critical JS 246 kB raw / **77 kB gzip**; gallery +4.8 kB gzip.

### Verified (real commands, real output)
- `npx tsc --noEmit` → **0 errors** (exit 0, clean).
- `npm run lint` → **0 errors, 3 warnings** (react-refresh on context-provider files — intentional pattern; lint exit 0).
- `npm test` → **19/19 passed** (fx query/override/detection, router, tokens mirror, 5 component smoke tests incl. LiquidTabs onChange + LiquidSwitch flip via role="switch").
- `npm run build` → **success in 4.66 s**, 2303 modules; dist CSS contains Sippy accent + glass tokens; dist JS contains `url(#sippyRefraction)`.
- Dev server `npm run dev` (Vite 7.3.8, port 5173, 0.0.0.0) — host-allowlist fixed (`server.allowedHosts`), restarted clean: `/`, `/gallery` (SPA fallback), `/src/main.tsx`, both backdrops, favicon → **all HTTP 200**.
- Phase 1 static preview server stopped (content superseded by the gallery); restart anytime with `cd sippy/shared && python3 -m http.server 8123 --bind 0.0.0.0`.

### Unverified (needs your eyes — no headless browser exists in this sandbox)
- Visual quality of everything: Glin components wearing the Sippy token overrides, fluidkit liquid animation (tabs/switch/toasts/progress), refraction bend, aurora in both themes, light theme finish.
- Lighthouse scores (command in `site/README.md`).

### Broken / compromised (honest)
- `eslint@9.39.5` is **deprecated** (v10 is current as of this date) — works fine, 0 errors; happy to migrate to ESLint 10 in Phase 3 if you want it current.
- Rollup quirk: the named `react`/`motion` manual chunks came out as 0 kB stubs (content still in index/glin chunks — totals correct, verified in build output). Cosmetic.
- Home is a deliberate Phase 2 shell — full page content is Phase 3 (no fake copy, per brief).

### Next
- Phase 3 approved & executed below.

## Phase 3 (full page + GitHub Pages readiness) — DONE, awaiting approval

### Done
- **Cinematic skippable intro** (`components/Intro.tsx`, `lib/intro.ts`): 5 beats ~3.6 s (glow breathes → logo stroke-draws & fills → glass droplet lens grows with backdrop blur/saturate → overlay lifts & hero words rise → nav pill springs in). Skippable by click/tap/Esc; once per session (`sessionStorage sippy-intro-played`); forced by `?intro=1` / `?intro=0` (beats everything, incl. reduced motion); reduced-motion → plain 0.5 s fade, no Intro rendered. Zero CLS (fixed overlay, content always laid out), `aria-hidden`, never blocks keyboard/AT. 7 unit tests.
- **Hero** (`sections/Hero.tsx`): always-dark, words "Drink / consistently. / Joyfully." rise blur-to-sharp staggered (delayed to land as the intro lifts when playing), shimmer CTA (Glin ShimmerButton glass) + glass secondary, `PhoneMock variant="home"` with mouse parallax (springs 300/30/1) + idle float. Mockup built from REAL glass components (LiquidProgress, GlassTabBar, GlassChip, Dippy) — no images, per brief.
- **`PhoneMock`** (`components/PhoneMock.tsx`): 3 variants (home / add / friends) from real Sippy glass components, forced-dark tokens.
- **Features bento** (`sections/Features.tsx`, `components/BentoCard.tsx`): 12-col grid; 3 core biggest (every drink you actually drink / friends who care / streaks & reminders) + goal, look, developer options; pointer tilt + cursor specular via GlassSurface; streak milestone pills (3/7/14/30/100, 3–14 done state).
- **Scroll-pinned showcase** (`sections/Showcase.tsx`): 300 vh pinned stage, 3 phone screens translate/scale/opacity across scroll progress (spring-smoothed), reference-5 style; reduced-motion OR narrow → static row.
- **How it works** (`sections/HowItWorks.tsx`): 3 steps, blur-to-sharp reveal, accent line draws with scroll.
- **Download card** (`sections/Download.tsx`, `lib/download.ts`): **honest gating** — button DISABLED with a visible note until `APP_DOWNLOAD` has real `url` + `sizeBytes` + `sha256` (set in Phase 6 from the real build); 4 install steps; "Android 8+ (best on 13+)"; no store badges. 3 unit tests (incl. `formatBytes`).
- **FAQ** (`sections/Faq.tsx`): 5 real Q&As, native `<details>` (keyboard/SR safe).
- **Footer** (`sections/Footer.tsx`): brand, tagline, hash-safe links (incl. base-prefixed `#/gallery` dev link).
- **GlassNav rewrite** (`components/glass/GlassNav.tsx`): IntersectionObserver scroll-spy, sliding liquid lens (`layoutId "nav-lens"`, spring 500/38), scroll compression (`.gnav-scrolled`, ≤600 ms), theme LiquidSwitch + CTA.
- **OG image** (`public/og-image.png`, 1200×630 AI droplet card) + full OG/Twitter meta in `index.html` with **`%VITE_BASE%`-substituted** `og:url`/`og:image`.
- **GitHub Pages readiness**:
  - `vite.config.ts` — `base` from `VITE_BASE` (`.env` default `/`, shell env overrides; `loadEnv` verified to merge process env from installed Vite 7.3.8 source).
  - `lib/router.ts` — `navigate()` always hash form `${BASE_URL}#/gallery`; new `asset()` helper for runtime public/ refs (Vite does NOT rewrite JS string literals) — applied in GalleryPage (5 refs).
  - `.github/workflows/deploy.yml` (repo root, the one allowed exception) — base from `${{ github.event.repository.name }}`, actions pinned to **commit SHAs** verified via GitHub API: checkout 11d5960 (v4), setup-node 49933ea (v4), configure-pages 983d773 (v5), upload-pages-artifact 56afc60 (v3), deploy-pages d6db901 (v4). Triggers: push to main + manual.
- **CSS**: `src/styles/index.css` — full Phase 3 layer (page frame, dark hero, phone mock, bento, showcase, how, download, FAQ, footer, intro keyframes, nav lens/scrolled, responsive ≤1000/≤640).
- `sippy-demo/` subpath test build output is gitignored + eslint-ignored.

### Verified (real commands, real output)
- `npx tsc --noEmit` → **0 errors** (exit 0).
- `npm run lint` → **0 errors, 3 warnings** (react-refresh on context files — intentional; exit 0).
- `npm test` → **31/31 passed** (6 files: fx 6, router 5, tokens 5, intro 7, download 3, glass smoke 5).
- `npm run build` → **success in 4.78 s**, 2317 modules. Bundles: index 264.6 kB raw / **82.5 kB gzip**, CSS 45.9 kB / 10.2 kB gzip, Gallery 14.7 kB / 4.7 kB gzip, fluid 197.8 / 66.8, glin 412.9 / 110.5.
- **Subpath test** — `VITE_BASE=/sippy-demo/ npx vite build --outDir sippy-demo --emptyOutDir` → success 4.72 s. Served via `python3 -m http.server 4180 --bind 0.0.0.0 --directory sippy/site`; curl on `http://127.0.0.1:4180/sippy-demo/` + 7 assets (JS×3, CSS, og-image, favicon, both backdrops) → **all HTTP 200**. Built `index.html` refs all `/sippy-demo/assets/…`; og:url `/sippy-demo/`; og:image `/sippy-demo/og-image.png`; favicon rewritten by Vite.
- **Absolute-path audit**: built JS (both bases) contains NO absolute `/…` asset URLs (only route name `"/gallery"` and the base-prefixed `"/sippy-demo/#/gallery"`); built CSS font `url()`s all base-prefixed in the subpath build; `src/` audit: only route-type comparisons remain. One real bug found & fixed: GalleryPage had 5 runtime `url(/backdrops/…)` literals → now `asset("backdrops/…")`.
- Dev server (Vite, 5173): `/`, `/gallery`, `/og-image.png`, backdrops, favicon, 3 new section modules → **all 200**.
- Production preview (`npm run preview`, 4173, serving `dist/`): `/`, `/#/gallery`, og-image, backdrops → **200**; content markers verified in dist bundles (hero-word, nav-lens, intro-veil, sippy-intro-played, bento-milestone, showcase-stage, download disabled state, gnav-scrolled in JS; intro-lens, hero-title, bento-grid, pm-screen-inner, faq-item, download-disabled in CSS).
- Fixed during verification: `formatBytes` "1.0 KB"→"1 KB" (2 test failures), unused import, `MotionValue`/`useMediaQuery` (motion 12.43 has no useMediaQuery export — local matchMedia hook), `process.cwd()` removed from vite config (no @types/node needed).

### Unverified (needs your eyes / your machine)
- Visual quality of the full page: intro beats & timing, hero parallax, bento tilt/specular, pinned showcase feel, nav lens glide, dark hero over both themes, reduced-motion/reduced-transparency behavior, 360→2560 responsive.
- Lighthouse (no lighthouse binary in sandbox) — run against the **production preview** (command below); target 90+ perf / 95+ a11y, LCP < 2.5 s.
- GitHub Pages workflow itself (runs only in your repo after Phase 6 push).

### Lighthouse — exact command (run on your machine, against the production build preview)
```bash
npx lighthouse https://4173-<your-sandbox>.e2b.app/ \
  --only-categories=performance,accessibility,best-practices,seo \
  --form-factor=desktop --output=json --output-path=./lighthouse-report.json --quiet
npx lighthouse https://4173-<your-sandbox>.e2b.app/ \
  --only-categories=performance,accessibility --form-factor=mobile \
  --output=json --output-path=./lighthouse-report-mobile.json --quiet
```
(Desktop first — the 90+/95+ targets are desktop-class for a glass-effect site; mobile for reference. The preview serves the exact production build on 0.0.0.0:4173.)

### Broken / compromised (honest)
- None blocking. Rollup 0 kB react/motion stub chunks remain (cosmetic, totals correct — carried over from Phase 2).
- `eslint@9.39.5` still deprecated-but-working (v10 exists); migration optional.
- 5 reference images never arrived in the session — hero/showcase/nav built from the written descriptions (ref 3 floating pill, ref 4 liquid capsule tabs, ref 5 dark glass music UI); gallery keeps all three.
- Intro duration 3.6 s + 0.22 s exit ≈ 3.8 s total — inside the 3.5–4.5 s band.

### Next
- ⏸ **STOP — AWAITING APPROVAL.** Your turn: view the dev server (full page + `?intro=1`/`?intro=0` + `#/gallery`) and the production preview, run Lighthouse, then approve Phase 4a (debug-APK compile checkpoint: minimal Compose slice + `tooling/build-debug.{sh,ps1}` + exact install steps).

## Phase 3 fix round — root causes (written BEFORE code changes, per instruction)

### A. Hard seams / black line — root cause
The "backdrop" is **three stacked systems, not one layer**:
1. `body { background: var(--bg-0) }` (styles/index.css, base section) is the only actual base colour.
2. `AuroraBackdrop` is a **transparent** `position:fixed` canvas that `clearRect`s and draws 4 blobs with `globalCompositeOperation: "screen"` — it never paints a base colour, so the colour behind the glass comes from `body`, and wherever the canvas doesn't cover (fixed-position breakage) the seam shows instantly.
3. **Per-section solid/gradient backgrounds on top**: the hero has `linear-gradient(180deg, rgba(4,6,11,0.99) 0%, rgba(5,8,15,0.96) 58%, rgba(5,8,15,0) 100%)` — a near-opaque band that holds 96% black until **58% of the hero's height**, then fades. The visible "black region" bottom sits at ~58–75% of `min(94dvh, 920px)` — a viewport-height-dependent, effectively hard colour boundary between hero-band and raw aurora.
4. **Full-width step at the content boundary**: `.page { padding-top: 104px }` pushes the hero section down 104px; the 0–104px strip shows raw backdrop while everything below sits under the hero's near-black scrim → the ~90–100px hard edge, at every scroll position, at every width.
5. Latent fixed-position hazards (the brief's list, confirmed present in code): `.hero-visual { perspective: 1200px }` + `transform-style: preserve-3d`, `will-change: transform, filter` on `.hero-word` / `.showcase-phone`, `min-height: 100dvh` (ok) — plus the footer's hard 1px `border-top` = the second reported edge (FAQ→footer).
**Structural fix:** ONE fixed full-viewport `.backdrop` layer (base colour + aurora canvas + hero caustics art + soft hero scrim) mounted as the FIRST child of `#root`, outside `.app-shell`; `html`/`body` background = darkest backdrop base; hero/sections fully transparent; the hero section starts at y=0 (padding moved inside it) so the 0–104px strip cannot exist; all section blends are single smooth ramps with ≥200px fade (no mid-plateau stops); footer border removed.

### B. Hero / PhoneMock — root cause
1. **"Get the APK" renders as bare text**: Glin `ShimmerButton`'s `glass` variant is implemented with **Tailwind utility classes** (`bg-white/10`, `border border-white/20`, `backdrop-blur-md`, `rounded-xl`, `px-6 py-2.5` — verified in `node_modules/@glinui/ui/dist/components/shimmer-button.js`). Tailwind 4's automatic content detection **does not scan `node_modules`**, so none of those utilities are generated → the button loses background/border/padding → text only. Latent for every @glinui/ui component. Fix: `@source` the Glin dist for Tailwind + explicit fallback styling on `.glass-btn`.
2. **Progress bar runs past its card**: `LiquidProgress width={196}` — hard-coded px inside a card whose content width varies (`width: min(240px, 62vw)` frame); 196px exceeds the card's inner width at most sizes.
3. **"Friends" clipped in the tab bar**: `LiquidTabs` (flow=stretch) lays out 3 content-sized tabs in ~210px; the longest label overflows the capsule and is cut by `.phone-screen { overflow: hidden }`.
4. **Lower half of the screen empty**: `.pm-screen-inner { min-height: 468px }` (fixed px) + the tab bar is in normal flow after the cards — nothing pins it to the bottom, nothing fills below.
5. **No fluid sizing**: frame is fixed 240×~468 (not 9:19.5-derived), inner sizes are px — any width change (62vw, showcase scale) breaks the fit. Fix: `aspect-ratio: 9/19.5` + `container-type: size` + all inner sizes in cqw + flex layout with `margin-top:auto` pinning + measured (ResizeObserver) width for the fixed-px LiquidProgress.

### E. Intro — root cause
1. **The droplet never falls because no falling droplet exists**: the current "droplet" is `.intro-lens` — a droplet-shaped div that animates `transform: scale(0) → scale(2.7)` **in place** at viewport centre (keyframe `intro-lens`). Zero translateY, no motion from above the viewport.
2. **"Ripple misaligned"**: there is no ripple element at all; the lens is box-centred (`top:50%; left:50%`) but its droplet shape (`border-radius: 50% 50% 50% 50% / 62% 62% 38% 38%`, point at top) has its optical centre **above** the box centre, while the logo (grid-centred symmetric teardrop) centres on the layout centre → the two apparent centres differ.
3. **All positions are hard-coded percentages** — no `getBoundingClientRect`-based impact point, no shared origin between "droplet", "ripple" and "reveal". Fix: compute the impact point ONCE from the logo target's rect, expose it as CSS custom properties, and drive droplet + all ripples + reveal from the same variables; recompute on resize; pure-function unit test across 4 viewport sizes.

## Addition: publish to GitHub Pages — DONE, awaiting approval

### Done
- `.github/workflows/deploy.yml` (repo root): triggers **push to main** + **workflow_dispatch**; permissions **contents:read, pages:write, id-token:write**; build job = checkout → setup-node (Node 22, npm cache) → configure-pages → `npm ci` → `VITE_BASE="/${GITHUB_REPOSITORY##*/}/" npm run build` → upload-pages-artifact (`sippy/site/dist`); deploy job = deploy-pages. Every action pinned to the **commit SHA of its version tag** (stronger than tag pinning; tags annotated in comments). No secrets/tokens/keystores anywhere in the workflow or repo (keystore path gitignored).
- Base path: CI derives from `GITHUB_REPOSITORY` (user's spec); local default `/` via `sippy/site/.env`. Both verified (below).
- `sippy/site/README.md`: **Live site** section (https://Cooreo.github.io/SippyDippy/) + **Troubleshooting** (failed deploy; blank page = wrong base; JS-string-literal asset note).

### Verified (real commands, real output)
- Workflow YAML parses; triggers `['push','workflow_dispatch']`, jobs `['build','deploy']`, permissions `{contents:read, pages:write, id-token:write}`; build step `VITE_BASE="/${GITHUB_REPOSITORY##*/}/" npm run build` (js-yaml parse, pasted).
- **Local default base** (`npm run build`): success 4.78 s (Phase 3 run), served 200 on `npm run preview` :4173.
- **Local `/sippy/` subpath build**: `VITE_BASE=/sippy/ npx vite build --outDir sippy --emptyOutDir` → **success in 4.96 s**; served via `python3 -m http.server 4180`; **all 25 unique fetchable refs → 200, ZERO 404s** (index.html src/href, CSS url()s, og-image, favicon, both backdrops, every asset file); `/sippy/`, `/sippy/#/gallery`, `/sippy/#/features` → 200; built `index.html` carries `og:url="/sippy/"`, `og:image="/sippy/og-image.png"`, base-prefixed asset refs; intro code path present in served bundle (sessionStorage marker) + 7 intro unit tests green.
- **What "intro plays" means here (honest):** no browser in this sandbox — I verified the intro ships in the served bundle and its gating logic passes unit tests; the visual play is your check (open `https://4180-<sandbox>.e2b.app/sippy/` — live preview).

### Unverified (needs you)
- The **live deploy** (Actions run) + **live site** at https://Cooreo.github.io/SippyDippy/ — only exist after the code reaches `main` on GitHub and you enable Pages.
- **Mobile** Lighthouse pass.

### Push status (honest)
- Everything committed to this session's branch `arena/cdf76885-sippydippy` and pushed to origin. **I cannot push to `main` from this session** (Arena ties the session to its branch) — PR #1 to `main` opened.

### Deploy (executed 2026-11-27 after user enabled Pages)
- User enabled Pages (source: GitHub Actions — verified via API: `build_type: "workflow"`, `html_url: https://cooreo.github.io/SippyDippy/`) and merged **PR #1** (merge commit `d35f36b…` on main).
- **Run 45915456833 attempt 1: FAILURE** — `Setup Pages` step failed (Pages just enabled, backend race); log download blocked from sandbox (results-receiver host not on the allowlist).
- **Rerun (`gh run rerun 45915456833 --failed`): SUCCESS** — build job ✓ (checkout, setup-node, configure-pages, npm ci, build, upload-pages-artifact), deploy job ✓ "Deploy to GitHub Pages in 11s". Only annotation: Node 20 deprecation notice on the pinned action versions (informational; actions forced onto Node 24 runners).
- Deployment record created on `github-pages` environment (id 8098539720) — status still propagating at time of writing.
- Live URL not curl-able from this sandbox (`cooreo.github.io` not on the outbound allowlist — curl exit 35): **final "open in browser" check is the user's**.

## Phase 3 fix round — results (A–G)

### Code (all on `arena/cdf76885-sippydippy`)
- **A (seams):** one fixed full-viewport `.backdrop` (base colour → aurora canvas → hero-caustics art → 200px+ scrim ramp) mounted as FIRST child of `#root`, outside `.app-shell`; `html`/`body` bg = `--bg-0`; all sections transparent; hero starts at y=0; footer 1px border-top removed → footer blends with a 120px gradient fade.
- **B (hero):** two-column grid inside the shared `.wrap` (max 1280px, padding `clamp(20px,5vw,48px)`, same width as the nav); PhoneFrame rebuilt on `aspect-ratio: 9/19.5` + `container-type: size`, all inner sizes cqw, tab bar pinned with `margin-top:auto`; `LiquidProgress` now measured (ResizeObserver) instead of 196px; Glin `@source` fixed (see below); "Get the APK" = Glin ShimmerButton glass variant (utilities verified present in built CSS).
- **C (showcase):** scroll pinning/stacking deleted entirely. Desktop = 3 phones in a fixed grid, pointer scales/focuses (1.0 vs 0.88 @ ~70%), group 3D tilt via pointer; touch = one centered phone, neighbours peek (33.3333% of the 264% track = 88% of stage — the old `flex: 0 0 88%` was 232% of stage, fixed), spring snap + dots; keyboard roving tabindex + arrows; reduced motion = no tilt.
- **D (download/FAQ):** `.narrow` 760px shared container; button/paragraph/steps share one left edge; steps = `<ol>` with numbered glass chips 1–4; FAQ neutral glass tint, accent only on open border/chevron, visible plus/minus, smooth `grid-template-rows: 0fr↔1fr` height animation.
- **E (intro):** impact point computed ONCE from the logo target rect → shared CSS vars (`--impact-x/y`); droplet falls from above the viewport (ease-in ~550ms) → squash/splash at impact → 3 ripples staggered 120ms from the same centre → reveal/draw/fill; recompute on resize. **Unit test proves identity at all 4 viewports:**
  ```
  [360x640]  impact=(180, 320)  dropTip=(180, 320)   ripples=(180, 320) (180, 320) (180, 320)
  [1280x720] impact=(640, 360)  dropTip=(640, 360)   ripples=(640, 360) (640, 360) (640, 360)
  [1920x1080] impact=(960, 540) dropTip=(960, 540)   ripples=(960, 540) (960, 540) (960, 540)
  [2560x1440] impact=(1280, 720) dropTip=(1280, 720) ripples=(1280, 720) (1280, 720) (1280, 720)
  ```
- **F (richness):** asset pipeline `tooling/build-assets.py` (black-bg luminance key LO=16/HI=72 → alpha, auto-crop, WebP byte budgets 250KB hero / 120KB rest, manifest `src/lib/assets.ts` + `hasAsset()`). **10 of 14 assets built (163.2 KB total, all under budget):** hero-caustics 1376×768 74.6KB; dippy-happy/dancing/sleepy/waving; friend-plant-sprout/bloom; friend-pet-cub; droplet-sprite; ripple-ring. **4 pending — the image-generation limit (10/turn) was reached: friend-pet-adult, bg-features, bg-showcase, bg-download.** Sections degrade gracefully: `SectionArt` skips itself when the asset is absent (manifest check) → no 404s, sections rest on the backdrop gradients (same palette). Contact sheet at `/#/assets`; ambient droplets (≤12, seeded, pointer parallax, IntersectionObserver pause, absent under reduced motion); Dippy buddy next to hero phone; bento art per card; friends in showcase; Dippy waving on download card; footer ripple ring. `CREDITS.md` carries the pack attribution.
- **G (verification):** `?debug=outline` added (`src/lib/debug.ts` + App effect): outlines every element faint pink, re-scans every 1.5 s + on resize, marks any `scrollWidth > clientWidth` element with a red outline + `data-overflow-hint="<px>"`.

### Gate (real output, this round)
- `npx tsc --noEmit` → **exit 0** (no output).
- `npm run lint` → **exit 0** — `✖ 3 problems (0 errors, 3 warnings)` (the 3 = pre-existing react-refresh/only-export-components in glass/GlassToast, effects, theme).
- `npm test` → **7 files, 36/36 passed** (incl. 5 intro-impact identity tests + 13 router tests incl. new `#/assets` case).
- `npm run build` → **✓ built in 4.35–4.80 s**; CSS 72.5 kB / gzip 14.5 kB.
- **CSS size regression found + fixed:** `@source` pointed at node_modules files still generated 262.6 kB of utilities (the full Glin dist was scanned; 125 utility classes in the built CSS that no source references). Fix: local content-hint copies of only the 3 used Glin components (`src/vendor/glin/`, scan-only — runtime imports still from `@glinui/ui`) → 72.5 kB, glass utilities (`bg-white/10`, `backdrop-blur-md`, `border-white/20`) verified present.
- **eslint config:** `sippy` build dir added to ignores (was exploding lint with 2152 errors on build output); `argsIgnorePattern: "^_"` added.
- **router regression caught by tests + fixed:** loose hash matching made `#gallery` (no slash) resolve as a route; restored strict `#/`-form + test.
- Dev server (:5173) transform check: all 14 changed modules → 200, zero Vite transform/resolve errors; all 10 asset files served 200.

### Home-route image bytes (target < 1.2 MB)
| asset | bytes | where |
|---|---|---|
| hero-caustics | 76.4 KB | backdrop |
| dippy-happy | 8.5 KB | hero buddy + 1 bento card |
| dippy-dancing | 11.2 KB | bento |
| dippy-sleepy | 8.9 KB | bento |
| friend-plant-bloom | 13.9 KB | bento + showcase |
| friend-pet-cub | 10.7 KB | bento |
| droplet-sprite | 1.9 KB | bento + ambient (≤12×, cached) |
| friend-plant-sprout | 6.8 KB | showcase |
| dippy-waving | 7.4 KB | download card |
| ripple-ring | 19.2 KB | footer |
| **total unique (bg-×3 + pet-adult pending)** | **≈ 165 KB** | **< 1.2 MB ✓** |

### To check in a browser (exact URLs + viewports)
Preview base: `https://{port}-<sandbox>.e2b.app/` (dev server :5173) or the production preview :4173.
1. **Seams (A):** `/` — scroll slowly top→bottom at **360, 768, 1280, 1920, 2560** wide; look for any hard horizontal edge (old: ~90–100px below hero top; old: FAQ→footer).
2. **Hero (B):** `/` at 360 & 1280 — "Get the APK" must be a glass pill (not bare text); phone: progress bar inside its card, "Friends" tab unclipped, tab bar at the bottom, Dippy buddy beside the phone.
3. **Showcase (C):** 1280+ — move the pointer across the row (one phone scales up, others dim, subtle tilt); 360 — drag left/right (snap + dots); Tab + arrows on 1280 (visible focus ring).
4. **Download/FAQ (D):** 768 — one left edge, numbered chips, FAQ open/close animation, neutral tint.
5. **Intro (E):** `/?intro=1` (fresh) at 1280×720 & 360×640 — droplet falls from above, splash + 3 ripples centered on the logo; `?intro=0` skips; skip button works.
6. **Debug:** `/?debug=outline` — pink outlines everywhere; any red-outlined element = horizontal overflow (`data-overflow-hint` shows px).
7. **Assets:** `/#/assets` — contact sheet (10 assets; 4 shown as pending on the page footer note).
8. **Reduced motion:** OS "reduce motion" on — no tilt, no ambient droplets, intro = fade.

### Honest status
- **Verified here:** gates above, module transform, asset serving, unit-test geometry.
- **Unverified (needs a browser):** all visual/interaction items 1–8 — no browser in this sandbox.
- **Known gap:** 4 of 14 asset images pending the generation limit (sections fall back to gradients gracefully; re-run `python3 tooling/build-assets.py` after the 4 PNGs land in `.assets-src/`).

### F completion (2026-11-27, image limit reset)
All 4 pending assets generated (style prefix verbatim, no rejections — each visually reviewed: clean edges, no text, correct palette), converted, and the builder now exits 0 with **14/14 assets, 226.4 KB total**:
- friend-pet-adult 177×240 7.7 KB (alpha 51.4%)
- bg-features 1376×768 19.1 KB · bg-showcase 21.0 KB · bg-download 15.4 KB (all ≤120 KB budget)
Home-route unique image bytes now **≈226 KB** (target <1.2 MB ✓). `/#/assets` contact sheet shows all 14; pending note disappears (PENDING_ASSETS empty).
Gates re-run green after the conversion: tsc 0, lint 0 err/3 warn, 36/36, build ~4.8 s.

## Phase 3 round 2 — root causes (written BEFORE code changes, per instruction)

### R2-1. Hero phone "oval tube with a mismatched rectangle"
The frame (`.phone`) is a **single bright element**: white glass-gradient fill (`linear-gradient(160deg, rgba(255,255,255,.3) … .16)`), a thick bright inner rim (`inset 0 0 0 .55cqw rgba(255,255,255,.32)`) and a large relative radius (7cqw). A real device is a **dark bezel holding a dark screen**. When the bezel is *brighter* than the screen, figure/ground inverts: the eye reads the bright rounded-rect ring as a standalone object (the "oval tube") and the dark 9:19.5 screen as an unrelated rectangle floating inside it (the "mismatched rectangle"). The "notch" was a separate black pill floating *over* the bezel, not an island cut into the screen; there was no frame/bezel/screen hierarchy at all, and all radii were independent hard-coded values (frame 7cqw vs screen 5.4cqw) so the two rounded rects drift at render scale.
**Structural fix:** rebuild as a true device stack — dark titanium frame → 1px rim highlight → bezel → screen (radius derived: screen = frame radius − bezel, so they can never drift) → dynamic island *inside the screen* → home indicator. 9:19.5, `container-type: size`, every inner size cqw. Screen filled by LIVE components (glass card, water ring, Dippy SVG, glass chips, liquid tab bar) + a 7 s demo loop. No images of the app anywhere (rule enforced structurally: the phone is only ever component markup).

### R2-2. "Get the APK" does nothing visible on click
The handler calls `element.scrollIntoView({ behavior: "smooth" })` while **Lenis owns scrolling** (`main.tsx` runs `new Lenis({ lerp: 0.1 })` + its own rAF loop). Lenis continuously re-writes the window scroll position from its virtual state; the native smooth-scroll animation fights that loop and is clamped away → no visible movement. Compounding: no offset for the fixed nav pill (target would land under it), no focus management, no press/hover feedback, and the label promised a download that doesn't exist yet (`APP_DOWNLOAD.url = ""` — the card's own button is disabled by `getDownloadState()`).
**Structural fix:** ONE shared `CtaButton` (Glin ShimmerButton) + ONE scroll path: `lib/scroll.ts` routes through the Lenis instance (`lib/lenis.ts` singleton) when it exists — a single owner of scroll position — native `scrollIntoView` only as fallback; `scroll-margin-top` on targets; click → scroll + focus the download heading (`tabindex=-1`, `preventScroll`) + 1.2 s rim flash on the card (opacity-only overlay); label rule: `getDownloadState().enabled` ? "Get the APK" (real link) : "See install steps" (scroll). Hover/press via transform/opacity only (lift, rim overlay, shimmer sweep, arrow nudge, scale 0.97, pointer-position ripple).

### R2-3. Card "bottom gradient", band under the cards, edge at hero end, top hairline
Measured + traced, four symptoms, three mechanisms:
1. **Band with hard top edge (SS2) + faint edge where the hero ends (SS1):** `.section-art` — an absolutely positioned *image layer* per section (`inset: -60px -80px`) clipped by the section's **`overflow: clip`**. The art rect extends 60px above the section but the clip severs it **exactly at the section's top edge = a hard horizontal line at every section boundary** (hero end in SS1; the boundary under the cards in SS2). The images are bright (measured row luminance: bg-features top rows 178/176/173; bg-showcase bottom rows 169–170) → the severed top reads as a lighter band. Root cause: per-section backgrounds (forbidden by the structural rule) + a clipping edge inside the viewport.
2. **Card "bottom gradient" cut at the edge:** the glass card carries a 3-layer bottom-weighted shadow (`--glass-shadow` up to `0 28px 72px`, 0.42 alpha dark), a 1px rim whose bottom-right stop is bright (`--glass-rim-end`), and a cursor-following specular (white 0.16) that is visibly cut when the pointer nears the bottom edge. The interior/exterior discontinuity at the card edge reads as "a gradient cut off at the border."
3. **Hairline at the very top (SS1):** the hero-caustics asset's **top rows are its brightest band** (measured: 97/96/89/84 vs mid ≈ 40), and the layer pins it to the viewport top via `background-position: 50% 0` + a mask that is fully opaque at 0% → the bright band sits exactly on the top edge.
**Structural fix (per the stated rule):** delete all `.section-art` layers and the sections' `overflow: clip` (no section has a background of its own); caustics image shifted so no content edge sits at the viewport top (`background-position: 50% -16%` + top mask ramp); glass recipe = flat tint + 1px rim + ONE static top-left specular ≤ 10% alpha fading to transparent well before the border + a single soft neutral shadow (the `--glass-shadow` token — the one token adjustment this fix forces — becomes a single non-bottom-weighted layer). After the fix, the only backgrounds that exist are: base colour (infinite), caustics (fades to 0 below the viewport at all realistic heights), hero scrim (fades to 0 by 660px), aurora canvas (infinite) — **no background can end inside the viewport.**

### R2-4. Nav wider than the hero grid
`.page-nav { width: min(1280px, calc(100% - 2·pad)) }` **duplicates** the `.wrap` formula, but `.wrap` is border-box (Tailwind preflight): at viewport ≥ 1376px the nav pill is 1280px wide while `.wrap`'s *content* (what the hero text/phone actually occupy) is 1280 − 2·48 = 1184px → the nav overhangs 48px per side. Two places computing one width, in different box models, drift.
**Structural fix:** stop duplicating — the nav pill goes *inside* the same `.wrap` (`.page-nav` becomes a full-width fixed strip; the pill is the wrap's content). Same element ⇒ same width at every viewport by construction. Nav structure (brand, links, switch, CTA) unchanged.

### R2-5. Intro do-over — "surface break"
Round-1 root-cause note (lens scaled in place, no real fall, box-centre vs shape-centre) stands; the brief now replaces the whole concept. New architecture: **one WebGL fragment shader** renders the entire underwater scene (deep gradient + light ramp, caustics sampled from the SAME `hero-caustics.webp` texture the hero backdrop uses, light shafts, bubble field, 4 gooey droplets + mark stroke-draw, surface ring with scene-only refraction, and the reveal as an alpha hole) — DPR capped 1.5, paused when the tab is hidden, canvas removed + GL context released on finish. Texture-missing (>300 ms) or no-WebGL → the SAME timeline in CSS (gradients/blur/transform-only hole via 9999px box-shadow circle). The mark is DOM (crisp) and FLIPs from its intro rect to the nav logo slot. The page renders underneath from t=0 (zero CLS); hero content is `opacity: 0` while covered (stays focusable), the overlay is `aria-hidden` and never focuses. Skip = any key/click → end state within 150 ms. `?debug=perf` logs frame times. Tests: reveal centre == mark centre at 4 viewports; timeline ≤ 4500 ms; reduced-motion path never creates a WebGL context.

### Round 2 implementation status (2026-11-26)

**R2-1 phone (live recreation).** `components/app/PhoneDevice.tsx` (dark frame, 9:19.5, `container-type: size`, rim via inset box-shadow, dynamic island INSIDE the screen), `components/app/WaterRing.tsx` (real SVG ring — the one stroke-animation exception; centre number counts), `components/PhoneMock.tsx` (home/add/friends/water variants) built from the REAL components: `GlassChip` (quick-add), `GlassTabBar` (fluidkit LiquidTabs, liquid lens, pinned bottom above the home bar), `Dippy` (mood swap idle→excited on tap). ~7 s demo loop: pointer drift → Water chip tap (ring ripple from the tap point, `.pm-tap-ripple`) → ring 1,240→1,490 ml counting → Dippy expression + bubble change → reset. IntersectionObserver (0.2) pauses offscreen; reduced motion = static filled frame, no pointer/ripple/counting. Hero: pointer-parallax rotateX/Y springs (off under RM) + 6 s bob; Dippy buddy bobs beside it. Every inner size is cqw — overflow is structurally impossible.

**R2-2 CTA.** `components/CtaButton.tsx` = the ONE shared CTA (nav `sm` + hero `lg`), Glin ShimmerButton, `variant primary|ghost`. `apk` label rule: `getDownloadState().enabled` → "Get the APK" (real link), else "See install steps" — and the button is deliberately NOT `disabled` (aria-disabled styling only) because a disabled button kills the click, and the click IS the action: `lib/scroll.ts` → Lenis-owned scroll with −88px nav offset (native under RM) → focus `#download-heading` (tabindex −1, preventScroll) → 1.2 s `.rim-flash` on `#download-card`. Hover is gated behind `@media (hover:hover)`: cursor, 2px lift, brighter rim (opacity overlay), shimmer sweep, arrow +3px; press = scale 0.97 spring + pointer-position ripple (same shape as the intro tap ripple); keyboard = native button (Enter+Space, ripple from centre, `:focus-visible` ring). Ghost "How it works" → `#how`/`#how-heading`.

**R2-3 gradients/seams.** All `.section-art` layers + section `overflow: clip` deleted (sections have NO backgrounds; `SectionArt.tsx` removed). Caustics at `50% -16%` with both-ends mask ramp. Glass = flat tint + 1px rim + ONE static top-left specular ≤10% fading to transparent before the border + single non-bottom-weighted `--glass-shadow`. Pointer-following specular removed from `GlassSurface` (it was the visible "cut gradient").

**R2-4 alignment.** Nav pill lives INSIDE `.page-nav > .wrap` — same container + inline padding as hero grid and cards; edges align by construction at every viewport.

**R2-5 surface-break intro.** `lib/intro-shader.ts` — one WebGL fragment shader renders the whole scene (deep gradient + light ramp + caustic flicker 0–0.4 s; rise 0.4–1.6 s scale 1.12→1.0 + shaft sweep + bubble stream; 1.6–2.5 s four gooey droplets (exp-field merge) + mark stroke-draw; 2.5–3.0 s ring with SCENE-ONLY refraction; reveal = alpha hole centred (0.5,0.5)). `components/intro/SurfaceBreakIntro.tsx` — orchestrator: 300 ms shader-vs-CSS decision (texture + WebGL → canvas crossfades in, CSS scene hidden; else CSS timeline — never blank); rAF clock paused on `visibilitychange`; skip on ANY key/pointerdown → end state ≤150 ms (clock jump + mark teleport + delays collapse); `pointer-events:none` once hole >80 %; 3.0 s `onReveal` + mark FLIP to `.gnav-brand` icon (layout-coordinate target — offsets ignore the nav's entry transform; scale 132px→icon size) + Dippy pop from the hole edge; 4.2 s finish → GL disposed (WEBGL_lose_context), overlay unmounted. RM → component renders null (no canvas, no GL context); HomePage plays the 350 ms `.page.fade-in`. `?intro=1/0`, once-per-session (sessionStorage), never on `/#/gallery`. StrictMode-safe: the unmount cleanup releases GL but never calls `onDone` (it would kill the second dev effect run). Ring refracts the intro scene ONLY — the page is a separate DOM layer (DOM capture infeasible; stated in the component doc).

**Gates (all real output, this pass):** tsc 0 · lint 0 errors / 3 warnings (pre-existing fast-refresh) · vitest 37/37 (6 new intro tests: reveal centre == mark centre at 360×640 / 1280×720 / 1920×1080 / 2560×1440; total ≤4500 ms; renderer decision; RM renders nothing and never creates a WebGL context) · build ~4.8 s · subpath build (`VITE_BASE=/sippy/`) ✓.

**Known honest gaps:** visual quality of the shader scene, intro frame rate, and the live URL need a browser (none in this sandbox); backdrop-filter count in the hero viewport peaks at ~7 small surfaces (nav pill, 2 CTAs, 3 phone chips, liquid tab bar) — every other viewport ≤6.

## Hero: phone preview → filling water ring (round-3 task)

**SPEC.md note:** the task linked `SPEC.md`, but no such file exists anywhere in the repo (checked root, `sippy/`, `sippy/site/`, git history) — implemented from the task's 7-point spec + this file.

**What changed (nothing else):**
- `src/sections/Hero.tsx` — the hero's phone (`<PhoneMock variant="home"/>`) is gone, replaced by `<HeroWaterRing/>` in the same `.hero-visual` column (Dippy buddy + parallax + bob unchanged).
- `src/components/HeroWaterRing.tsx` (new) — glass disc fills 0→100% in 4 s (easeInOutCubic), gentle wave on the surface, centred tabular-numbers label counting in sync; at 100% a 0.7 s shimmer sweeps the disc and the label crossfades (grid cell, no layout shift) to "Goal reached"; holds 1.5 s, resets, loops. IntersectionObserver (0.2) pauses offscreen and resumes mid-cycle. `prefers-reduced-motion` → static 100% state with the label, no rAF, no animation.
- `src/styles/index.css` — `.hr*` block. `--hr-size: clamp(260px, 34vw, 520px)`; the `.hr` box IS the disc (exact bounding box). Motion = transform/opacity only: level = `translateY` on the fill, wave = `translateX` on a clipped element (the sanctioned exception; two tileable periods so the −50% drift is seamless), shimmer = transform sweep + opacity. Label colour is explicit per theme (`#0a1120` light / `#f4f7ff` dark) + a static `::before` halo (light brightens, dark darkens) so the contrast margin holds at every fill level.
- `PhoneMock`/`PhoneDevice`/app `WaterRing` + all `.pm-*`/`.device*` styles are **kept** — they are shared with the Showcase (which must not change); the hero has zero phone markup or hero-phone styles left, and no hidden copies.

**Label contrast on the disc (computed, WCAG 2.x relative luminance; caustics mean measured from the actual `hero-caustics.webp` top band = RGB(19,61,111); page-under-disc includes the 0.40-alpha hero scrim at ~50vh):**

| mode | background | RGB | no halo | with halo (shipped) |
|---|---|---|---|---|
| dark | glass only (fill 0%) | (31,52,77) | 11.83:1 | **14.06:1** |
| dark | water top, surface at centre (worst) | (48,100,182) | 5.41:1 | **8.25:1** |
| light | glass only (fill 0%) | (84,100,121) | 3.12:1 | **7.57:1** |
| light | water top, surface at centre (worst) | (68,119,198) | 4.22:1 | **8.53:1** |

All ≥ 7.5:1 with the halo — AA/AAA for large and normal text in both themes.

**Bounding box (`--hr-size: clamp(260px, 34vw, 520px)`, the `.hr` element = the disc exactly):**
- viewport 360px → 260 × 260 px (column available: 320 px ✓)
- viewport 1280px → 435.2 × 435.2 px (visual column ≈ 538 px ✓)
- viewport 2560px → 520 × 520 px (visual column ≈ 532 px ✓ — cap chosen so it never overflows the grid column)

**Gates:** tsc exit 0; lint 0 errors / 3 pre-existing warnings; 37/37 tests; build 4.79 s; subpath build (`VITE_BASE=/sippy/`) 4.79 s.
