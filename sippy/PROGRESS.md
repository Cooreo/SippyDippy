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
