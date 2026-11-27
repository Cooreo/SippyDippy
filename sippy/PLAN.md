# SIPPY — Master Plan (revised v3, per approval 2026-11-26)

Status: **Phase 3 running** · Base: `6fadff8` of `Cooreo/SippyDippy` (read-only; the ONE exception is `.github/workflows/deploy.yml` at the repo root, explicitly requested in the Phase 3 brief)

## 1. Environment audit (unchanged, verified)
Node 22 present; **no Java/Android SDK/browser/KVM**. Network: npm + GitHub API + codeload only; all Maven/Google hosts blocked → **APK built by you** (Option A, agreed). Website fully buildable/runnable here.

## 2. Product (locked)
App **Sippy**: friends (plants/pets, user-chosen names, default Dippy), hydration tracking with per-drink hydration %, custom daily minimum, streaks, local reminders, custom themes, Developer options screen (triggers every state). **v2 (Phase 7):** food DB + side calorie counter + OpenRouter smart-add (user's own key, runtime, device-local only — nothing key-related ever in repo or website).

## 3. Workspace
`/home/user/SippyDippy/sippy/` (+ root `.github/workflows/deploy.yml` per explicit instruction). Old repo untouched; nothing committed/pushed.

## 4. Phases — every phase ends with stop + your approval

### Phase 0 — Plan ✅ · Phase 1 — Foundations ✅ · Phase 2 — Site design system & shell ✅

### Phase 3 — Full website page + GitHub Pages readiness (RUNNING)
**Full page** (real copy only — no invented testimonials or stats):
- **Intro**: 5 beats, 3.5–4.5 s, skippable (click/tap/Esc), once per session via sessionStorage, **`?intro=1` forces play / `?intro=0` forces skip** (test flags), `prefers-reduced-motion` → simple fade, zero CLS, never blocks keyboard/AT.
- **Nav pill**: floating glass, compresses on scroll, **sliding liquid lens on the active section**, liquid theme switch, shimmer CTA.
- **Hero**: clamp(48–120px) headline (words rise blur-to-sharp, staggered), one-line pitch, shimmer CTA + glass secondary, **app mockup built from real Sippy glass components** (phone frame containing GlassCard + LiquidProgress + GlassTabBar + Dippy), mouse parallax + idle float.
- **Features bento**: 3 core get the biggest cards (every drink with real hydration values · friends who react · streaks & reminders); smaller: your goal, your look, Developer options. Cursor-following specular + slight pointer tilt.
- **App showcase**: scroll-pinned, three phone screens (real glass components) transitioning with scroll.
- **How it works**: 3 steps, scroll-linked.
- **Download card (honest)**: version v1.0.0; links to a **GitHub Release asset URL from a configurable constant** (`src/lib/download.ts`); shows real size + SHA-256 once set; **stays disabled until the real file exists**. 4 install steps, "Android 8+, best on 13+" note. (Supersedes the old "APK in public/downloads/" — per your instruction the binary is a Release asset; the site never contains it.)
- FAQ (short, real) · footer · OG image + complete meta · responsive 360→2560.

**GitHub Pages readiness:**
- Vite `base` configurable via **`VITE_BASE` env var** (default `/`); **no absolute `/...` URLs in runtime code** (all via `import.meta.env.BASE_URL` / public rewriting); **hash routing verified** (`/#/gallery` works under a subpath; path form works in dev).
- **Build + preview test under a fake subpath** (`VITE_BASE=/sippy-demo/`), real output pasted.
- **`.github/workflows/deploy.yml`** (repo root): build with `VITE_BASE` **derived from the repo name**, deploy via official Pages actions (`configure-pages`, `upload-pages-artifact`, `deploy-pages`) with **pinned action versions**.

**Verify:** tsc / lint / tests / build — real output pasted. Lighthouse command provided **against the production preview build** (`npm run build && npm run preview` → :4173).

### Phase 4a — App: minimal buildable slice + debug scripts (COMPILE CHECKPOINT)
- Slice: design-system theme, one glass backdrop, GlassCard, GlassButton, GlassTabBar (liquid lens), one screen.
- **`sippy/tooling/build-debug.sh` + `build-debug.ps1`**: check prerequisites (JDK 17+/21, Android SDK location, `platforms;android-37`, `build-tools;37.0.0`) and **print clear errors**; on success run the vendored wrapper → debug APK.
- `app/BUILD.md`: exact install list (JDK 21 or Android Studio's JDK; Android Studio **or** command-line SDK + the exact SDK packages) and the one build command.
- **STOP → you build → send errors/screenshots → iterate to green.**

### Phase 4b — App: full v1 implementation (after green checkpoint)
(unchanged: all screens, friends/streaks/reminders/goals/themes/dev-options, Room-less DataStore persistence, quality tiers T1/T2/T3, polish)

### Phase 5 — Release APK
- **`sippy/tooling/build-release.sh` + `build-release.ps1`**: if no keystore, generate with **keytool** at `sippy/keys/sippy-release.keystore` (**gitignored before it exists**; storepass/keypass supplied via env/args — never written to files in the repo), `./gradlew assembleRelease` (R8 on), sign → **`app/release/sippy-v1.0.0.apk`**, print **size + SHA-256** (+ write `.sha256` sidecar).
- **Nothing secret in repo or website** (keystore/passwords/API keys) — `keys/`, `*.keystore`, `*.apk` already gitignored.
- Install instructions: `adb install -r` **and** no-PC sideloading (copy APK → allow installs → open → confirm).

### Phase 6 — Deploy and integrate
- Set `APP_DOWNLOAD.url` (Release asset URL) + `sizeBytes` + `sha256` from your real file → **download card activates with real values** → redeploy.
- **`sippy/README.md` — your exact steps**: (1) create public GitHub repo, (2) push this checkout, (3) Settings → Pages → Source: **GitHub Actions**, (4) create Release `v1.0.0` + upload the APK, (5) set the download URL constant (+ size/SHA), (6) push/redeploy, (7) verify.
- Verification split: **me** — builds, subpath preview, checksums, workflow file correctness; **you** — live URL, release upload, the Actions deploy run.

### Phase 7 — Food DB + OpenRouter smart-add (post-release, per your earlier instruction)
Curated local food DB (~200 foods, bundled), side calorie panel, smart-add with the user's own key (runtime, device-local).

### Phase 8 — Final integration & report
CREDITS.md final, PROGRESS.md final, section-6 report per phase.

## 5. Risks & compromises (current, blunt)
1. APK built by you (4a checkpoint → 5 scripts). No compile here.
2. No Lighthouse/screenshots from me — your browser is the visual channel.
3. OpenRouter: runtime key field only; local DB is the guaranteed path (Phase 7).
4. GH Pages: workflow file can't be test-run here (no push) — verified by build + subpath preview only; the live deploy is yours.

## 6. Decision log
| # | Decision | Status |
|---|---|---|
| 1–6 | (v2 decisions: workspace, names, Option A, runtime key, scope additions, food DB later) | ✅ |
| 7 | Site deploys to GitHub Pages via **GitHub Actions** (Pages source = Actions), base from repo name, pinned actions | ✅ approved |
| 8 | APK distributed as a **GitHub Release asset**; download card = configurable URL constant, disabled until real | ✅ approved |
| 9 | Intro test flags `?intro=1` / `?intro=0` | ✅ approved |
| 10 | Hero mockup built from **real glass components** (no images) | ✅ approved |
| 11 | Lighthouse run against the **production preview build** (:4173) | ✅ approved |
