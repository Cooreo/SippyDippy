# Sippy — website

Vite + React + TS + Tailwind 4 + motion + lenis + fluidkit + @glinui/ui.
Design tokens: **single source of truth is `../shared/tokens.md`** (consumed by this CSS and, in Phase 4, the Compose app).

## Live site

**https://Cooreo.github.io/SippyDippy/**

Deploys automatically from `main` via GitHub Actions
(`.github/workflows/deploy.yml`) — push to `main` (or run the workflow
manually from the Actions tab) and the site updates. One-time repo setup:
**Settings → Pages → Build and deployment → Source: GitHub Actions**
(a free GitHub account needs the repo to be **public** for Pages to work).

## Troubleshooting

- **Deploy failed** (Actions run is red): open
  `Actions → "Deploy Sippy website to GitHub Pages" → latest run`. The build
  step must show the Vite output ending in `✓ built in …s`. If `npm ci`
  fails, the committed `package-lock.json` is out of sync with
  `package.json` — regenerate it locally (`npm install`) and commit both.
  If the run can't deploy at all ("Pages" step errors), check
  **Settings → Pages → Source: GitHub Actions** (see Live site).
- **Blank page** (200 but nothing renders): almost always a wrong base path.
  Open DevTools → Network and look for 404s on `/…/assets/*.js`. The base
  must be `/<repo-name>/` — on CI it comes from `GITHUB_REPOSITORY`
  automatically; if you built locally, set `VITE_BASE=/<repo-name>/`.
  Verify with: `VITE_BASE=/<repo-name>/ npx vite build` then grep the built
  `index.html` for the prefixed asset URLs.
- **Right page, broken assets under a custom base**: remember JS string
  literals are not rewritten by Vite — runtime public/ references must go
  through `asset()` in `src/lib/router.ts`.

## Commands

```bash
npm install
npm run dev        # dev server → http://localhost:5173  (binds 0.0.0.0)
npm run build      # tsc --noEmit && vite build → dist/
npm run lint       # eslint
npm run typecheck  # tsc --noEmit
npm run test       # vitest (unit + smoke, happy-dom)
npm run preview    # serve the production build → http://localhost:4173
```

## Page

Full marketing page (Phase 3): cinematic skippable intro (once per session) →
always-dark hero (words rise blur-to-sharp, phone mockup built from the real
glass components, mouse parallax) → features bento (pointer tilt + cursor
specular) → scroll-pinned 3-phone showcase → how it works → download card
(**honest**: disabled until `src/lib/download.ts` holds a real APK URL + size
+ SHA-256) → FAQ → footer. Floating glass pill nav with scroll compression,
active-section liquid lens, theme switch.

Intro controls: `?intro=1` force play · `?intro=0` force skip · reduced-motion
→ simple fade.

## Routes

- `/` — the page above.
- `/gallery` (hidden) — every glass component, light & dark, over every backdrop, with a "reduce effects" switch that emulates `prefers-reduced-motion` + `prefers-reduced-transparency` and forces the blur-only refraction fallback. Also reachable via `/#/gallery` (works on any static host without SPA rewrites).

## Base path / GitHub Pages readiness

- `base` comes from **`VITE_BASE`** (`.env` default `/`, shell env overrides).
  Dev and plain deploys: no action needed.
- Every runtime asset reference is base-relative (Vite rewrites HTML/CSS/imports;
  JS string literals go through `asset()` in `src/lib/router.ts`).
- **Subpath smoke test** (proves it works under `<host>/<repo>/` before deploying):

  ```bash
  VITE_BASE=/sippy-demo/ npx vite build --outDir sippy-demo --emptyOutDir
  python3 -m http.server 4180 --bind 0.0.0.0
  # open http://localhost:4180/sippy-demo/  (and /sippy-demo/#/gallery)
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:4180/sippy-demo/
  ```

  Expected: `200`. `sippy-demo/` is gitignored + eslint-ignored build output.
- CI: `.github/workflows/deploy.yml` (repo root) builds with
  `VITE_BASE=/<repo-name>/` and deploys via the official GitHub Pages actions
  (pinned to commit SHAs). One-time setup: repo Settings → Pages → Source:
  **GitHub Actions**.

## Refraction (Chromium-only progressive enhancement)

- Default: full (SVG displacement inside `backdrop-filter` + fluidkit `refraction`) on Chromium; blur-only elsewhere (Safari/Firefox degrade to clean `blur + saturate`).
- Force the fallback anywhere: **`?fx=blur`** (or `?fx=full` to override the opposite).

## Lighthouse (run on your machine — production build preview)

Start the production preview first: `npm run build && npm run preview`
(→ http://localhost:4173). Then:

```bash
npx lighthouse http://localhost:4173 \
  --only-categories=performance,accessibility,best-practices,seo \
  --form-factor=desktop \
  --output=html --output-path=./lighthouse-report.html \
  --quiet
```

Targets: performance 90+, accessibility 95+, LCP < 2.5 s (desktop-class for a
backdrop-filter-heavy site; run mobile too for reference).
