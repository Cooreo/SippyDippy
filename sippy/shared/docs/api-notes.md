# API Notes — verified from real sources (2026-11-26)

No API in Sippy code may be used until it appears here (working agreement #5).
Sources: npm registry metadata + READMEs, codeload source tarballs, glinui.com docs pages.
All fetched into `sippy/third-party/` (trimmed to essentials).

## 1. Glin UI (website) — MIT

- Packages: `@glinui/ui@0.1.1`, `@glinui/tokens@0.1.1` (npm, peer: React ^18 || ^19).
- Install: `npm install @glinui/ui @glinui/tokens`. CLI alternative: `npx glinui add <component>` (copies source into repo).
- `@glinui/tokens` package contents (verified by unpacking the tarball): `theme.css`, `dist/index.js`, `dist/index.d.ts`.
  - Import once at entry: `@import "@glinui/tokens/theme.css";` → defines `--color-*`, `--glass-{1..5}-surface`, `--glass-{1..5}-shadow`, `--glass-blur-sm/md/lg/xl`, `--shadow-glass-*`, `--glass-refraction-top`.
  - ⚠️ The getting-started page mentions a Tailwind 3 preset (`@glinui/tokens/tailwind-preset`) and a Tailwind 4 entry — **neither file exists in the 0.1.1 tarball**. Decision: use `theme.css` for CSS variables only and map them into our own Tailwind 4 `@theme` block in Phase 2.
- `@glinui/ui` dist contains per-component modules incl. `shimmer-button.js`, `glass-card.js`, `glass-navbar.js`, `glass-dock.js`, `glass-breadcrumb.js`, `glass-toggle.js`, `aurora-background.js`, `animated-gradient.js`, `blur-spotlight.js`, `border-beam.js`, `glow-border.js`, `depth-card.js`, `liquid-button.js`, `ripple-button.js` (245 files total).
- **ShimmerButton** (from glinui.com/docs/components/shimmer-button, verified):
  ```tsx
  import { ShimmerButton } from "@glinui/ui"
  <ShimmerButton variant="glass" shimmerColor="rgb(255 255 255 / 0.5)" shimmerDuration={3}>CTA</ShimmerButton>
  ```
  | Prop | Type | Default | Notes |
  |---|---|---|---|
  | `shimmerColor` | string | text colour @ 35% | we pass Sippy Blue / white per theme |
  | `shimmerDuration` | number (s) | 2 | |
  | `variant` | ButtonVariant \| "accent" | ambient `glinr` | `glass` is opt-in; "needs a colorful or photographic backdrop" |
  | `size` | "sm" \| "md" \| "lg" | md | |
  - Accessibility: native `<button>`, 2px accent focus ring, `disabled`/`aria-busy`.
  - Reduced motion: sweep renders only at motion level `full`; removed under `prefers-reduced-motion` and `data-glin-motion="none"` — keep as-is.
- Dark mode: tokens switch on `.dark` class on `<html>`; `darkMode: "class"` (Tailwind 3) / `@custom-variant dark` (Tailwind 4).
- `Button` variants (from docs): glinr, solid, plain, soft, outline, ghost, gradient (+glass for glass-capable components).

## 2. fluidkit (website) — MIT, v0.5.0

- Package: `fluidkit` (npm). Install: `npm install fluidkit react react-dom motion` (peers: motion >=11, react >=18).
- Core is **GPU-free** (36.7 kB brotli budget); WebGL only in opt-in subpath `fluidkit/liquid-metal` (optional peer `@paper-design/shaders-react@0.0.76`). We do **not** use LiquidMetal.
- Motion principle: "animate the surface, never the text" — surface layer morphs (spring + surface tension, real metaball bezier bridges as live `clip-path`), content only cross-fades.
- Components we will use (Phase 2/3):
  - `LiquidSwitch` — `checked`, `onCheckedChange`, `label`; `material="glass"`, `intensity`, `opacity`, `tint`. (Theme toggle)
  - `LiquidTabs` — indicator flows between tabs (slide or stretch), labels tint by liquid coverage, flat or glass material. (Nav active lens; phone-mock bottom bars)
  - `MeshGradient` — ambient CSS backdrop: large blurred radial blobs drifting slowly. (Aurora assist)
  - `Ripple` (pointer-origin water ripple), `FlowStagger` (list entrances) — opportunistic.
- Material props shared: `material` ("glass"/"flat"), `tint`/`color`, `opacity` (0 clear → 1 solid), `intensity` (0–1), `light` (scene light position, null disables speculars), `reflection` (bool), `refraction` (opt-in **Chromium-only** edge lensing = SVG displacement inside backdrop-filter, degrades silently), `shadow` (bool).
- `FluidThemeProvider` theme tokens: `accent`, `surface`, `text`, `radius` (px), `mode` ("light"/"dark"), `material`, `intensity`. Precedence: explicit props → theme derivation → component defaults.
- Guarantees (README-verified): `prefers-reduced-motion` collapses every effect to static state (content still interactive); no `backdrop-filter` → frosted flat fill; loops pause off-screen (IntersectionObserver); SSR-safe; tree-shakeable (`sideEffects: false`).

## 3. Kyant0 Backdrop (app, Tier 1) — Apache-2.0

- **Coordinate: `io.github.kyant0:backdrop:2.0.1`** — Maven Central. Version confirmed by GitHub tag `2.0.1` AND `mavenPublishing { coordinates("io.github.kyant0", "backdrop", "2.0.1") }` in `backdrop/build.gradle.kts`.
- Also needed: `io.github.kyant0:shapes:1.2.1` (provides `Capsule()` shape used in all examples).
- Library requirements (from its build file): minSdk 21, **compileSdk 37**, buildTools 37.0.0. Compose-only (foundation/ui/ui-graphics) + kyant-shapes + org.jetbrains:annotations.
- **Core API (verified from source):**
  ```kotlin
  // Backdrop.kt
  interface Backdrop {
      val isCoordinatesDependent: Boolean
      fun DrawScope.drawBackdrop(density: Density, coordinates: LayoutCoordinates?, layerBlock: (GraphicsLayerScope.() -> Unit)? = null)
  }
  ```
  - Backdrops: `rememberLayerBackdrop()`, `rememberCombinedBackdrop(a, b)`, `CanvasBackdrop`, `EmptyBackdrop`, `LayerBackdrop`; `Modifier.layerBackdrop(backdrop)` marks content to be captured.
  ```kotlin
  // DrawBackdropModifier.kt (full signature, verified)
  fun Modifier.drawBackdrop(
      backdrop: Backdrop,
      shape: () -> Shape,
      effects: BackdropEffectScope.() -> Unit,
      highlight: (() -> Highlight?)? = DefaultHighlight,
      shadow: (() -> Shadow?)? = DefaultShadow,
      innerShadow: (() -> InnerShadow?)? = null,
      layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
      exportedBackdrop: LayerBackdrop? = null,
      onDrawBehind: (DrawScope.() -> Unit)? = null,
      onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdrop,
      onDrawSurface: (DrawScope.() -> Unit)? = null,
      onDrawFront: (DrawScope.() -> Unit)? = null
  ): Modifier
  ```
  - Effects (`BackdropEffectScope`, verified): `blur(radius: Float, edgeTreatment: TileMode = TileMode.Clamp)`, `lens(refractionHeight: Float, refractionAmount: Float, depthEffect: Boolean = false, chromaticAberration: Boolean = false)`, `vibrancy()`, `colorFilter(ColorFilter)`, `opacity(alpha: Float)`, `colorControls(...)`, `effect(RenderEffect)`, `runtimeShaderShader(...)`.
  - `Highlight(width: Dp = 0.5.dp, blurRadius: Dp, alpha: Float = 1f, style: HighlightStyle)` with `Highlight.Default` / `Highlight.Ambient`; `Shadow.Default`; `InnerShadow(radius, alpha)` with `InnerShadow.Default`.
  - `BackdropEffectScope` also exposes `size`, `layoutDirection`, `shape`, `padding`, `renderEffect` (mutable) and a `RuntimeShaderCache`.
- **Reference implementation:** `app/.../catalog/components/LiquidBottomTabs.kt` (289 lines, fetched) — pattern we adapt for GlassTabBar: outer capsule `drawBackdrop(backdrop, shape = { Capsule() }, effects = { vibrancy(); blur(8.dp.toPx()); lens(24.dp.toPx(), 24.dp.toPx()) }, onDrawSurface = { drawRect(containerColor) })`; a tinted content layer; a sliding lens capsule whose `lens()/Highlight/Shadow/InnerShadow` intensities scale with `pressProgress`, plus damped drag with velocity-aware scale. Also `LiquidButton.kt`, `LiquidToggle.kt`, `LiquidSlider.kt` in the same directory.
- Library ships **no high-level components** — all Sippy glass components are ours.
- Repo toolchain (for pinning): AGP 9.3.2, Kotlin 2.4.10, Gradle 9.7.1, compose-multiplatform 1.12.0, core-ktx 1.19.0, activity 1.13.0.
- Its pure-Android sample `androidApp/build.gradle.kts` (fetched) is our scaffold template: `compileSdk 37`, `targetSdk 37`, minSdk (theirs 23 → ours 26), release build `isMinifyEnabled + isShrinkResources + proguard-android-optimize`, `packaging.resources.excludes` list, `dex.useLegacyPackaging = true`, `jniLibs.useLegacyPackaging = true`, `dependenciesInfo.includeInApk = false`.

## 4. Haze (app, Tier 2 / fallbacks) — Apache-2.0, Haze 2 API

- **Coordinates (all same version, verified against GitHub tags): `dev.chrisbanes.haze:haze:2.1.0`, `dev.chrisbanes.haze:haze-blur:2.1.0`, `dev.chrisbanes.haze:haze-glass:2.1.0`** (Maven Central; optional `haze-blur-materials`, `haze-glass-material3` not needed).
- This is **Haze 2** (modular, source-backed API) — not the old `Haze`/`hazeSource`+`HazeNode` 1.x API. Verified API:
  ```kotlin
  val hazeState = rememberHazeState()
  val style = remember { HazeBlurStyle { blurRadius(20.dp) } }

  Box {
      Image(painter, null, modifier = Modifier.hazeSource(hazeState))
      Box(Modifier.matchParentSize()
          .hazeBlur(input = HazeInput.Sources(hazeState), style = style))
  }

  Modifier.hazeGlass(
      input = HazeInput.Sources(hazeState),
      style = GlassStyle.regular.then {
          tint(Color.White.copy(alpha = 0.16f))
          shape(RoundedCornerShape(20.dp))
      },
  )
  ```
  - `HazeInput.Sources(hazeState)` (captured content) vs `HazeInput.Content` (own content).
  - `GlassStyle.regular` / `GlassStyle.clear`; `.then { }` derivation; `tint()`, `shape()`; `rimHighlight(intensity, width, sides)`, `edgeOutline(color, width)`, `ContinuousRoundedCornerShape` (iOS-style corners), `GlassDefaults.continuousShape`.
  - `HazeMaterials.ultraThin()/thin()/regular()/thick()/ultraThick()` (haze-blur-materials); `HazeBlurStyle.Material3()` (haze-blur-material3) — optional.
  - Performance: `HazePerformanceMode.Default` (= fixed `Balanced`), `Quality`, `Performance`, `Fixed(...)`; `Adaptive` deprecated alias.
  - Fallbacks (verified in CHANGELOG/README): on Android 12/12L hardware uses hardware blur for fallback rendering; automatic scrim on unblurred paths (`fallbackColor` to change/disable); reduced-transparency fallback.
- Repo toolchain (for pinning): **AGP 9.5.2, Kotlin 2.4.21, Gradle 9.9.2**, compose-multiplatform 1.12.2, jetpack-compose (androidx) **1.12.1**, compose-material3 **1.9.0**, navigation-compose 2.10.0, activity 1.13.0, core-ktx 1.19.1, lifecycle 2.11.0, coil3 3.6.3 (not used by us).

## 5. Sippy app — pinning table (Phase 4a)

Rule: pin to the **higher** of the two libraries' own repo pins (compatibility), exact coordinates below.

| What | Coordinate / setting | Version | Source |
|---|---|---|---|
| Gradle | wrapper `distributionUrl` | **9.9.2** | haze repo wrapper (Kyant0: 9.7.1) |
| AGP | `com.android.application` | **9.5.2** | haze catalog (Kyant0: 9.3.2) |
| Kotlin | `org.jetbrains.kotlin.android` | **2.4.21** | haze catalog (Kyant0: 2.4.10) |
| Compose UI | `androidx.compose.ui:ui` (+ foundation, runtime, material-ripple) | **1.12.1** | haze `jetpack-compose` ref |
| Material 3 | `androidx.compose.material3:material3` | **1.9.0** ⚠️ | haze `compose-material3` ref — *verify exists on google() maven at first build; fallback: latest stable* |
| Activity | `androidx.activity:activity-compose` | **1.13.0** | both repos |
| Core | `androidx.core:core-ktx` | **1.19.1** | haze catalog |
| Lifecycle | `androidx.lifecycle:lifecycle-runtime-compose` | **2.11.0** | haze catalog |
| Navigation | `androidx.navigation:navigation-compose` | **2.10.0** ⚠️ | haze catalog (KMP group there; we use androidx group) |
| DataStore | `androidx.datastore:datastore-preferences` | **1.1.x** ⚠️ | not in either repo — latest stable at first build |
| Backdrop | `io.github.kyant0:backdrop` | **2.0.1** | tag + build file |
| Shapes | `io.github.kyant0:shapes` | **1.2.1** | Kyant0 catalog |
| Haze | `dev.chrisbanes.haze:haze` + `:haze-blur` + `:haze-glass` | **2.1.0** | GitHub tags |

⚠️ = cannot be verified from this sandbox (maven.google.com blocked). These get a "verify at first build" line in `app/BUILD.md` with fallback instructions.

**Android settings:** minSdk **26** (brief), compileSdk **37** (backdrop requires), buildTools 37.0.0, targetSdk **37** (Kyant0's own sample uses 37; sideload — no Play constraints).
**JDK:** 21 (Android Studio's bundled JDK or OpenJDK 21). If AGP 9.5.2 reports a newer requirement, the error names it.
**Deliberate deviation from brief (risk reduction):** v1 uses **DataStore only — no Room/KSP** (KSP version for Kotlin 2.4.21 not verifiable here; KSP is a major blind-build risk). Data volume is tiny (drinks, log, friends, streaks, settings) → JSON documents in DataStore. Room can be added later without data migration.
**No KMP** — pure Android (`com.android.application` + `org.jetbrains.kotlin.android` + `kotlin.plugin.compose`), matching Kyant0's androidApp sample shape.

## 6. Website stack (Phase 2) — exact versions (verified against npm registry 2026-11-26)

| Package | Version | Notes |
|---|---|---|
| vite | 7.x (latest stable at install) | |
| react / react-dom | 19.x | glinui peer ✓ |
| typescript | ~5.9 | |
| tailwindcss | 4.x | CSS-first config; @theme maps glin tokens |
| motion | 12.x | `motion/react` (fluidkit peer >=11 ✓) |
| lenis | 1.x | smooth scroll |
| @glinui/ui | **0.1.1** | pinned (latest) |
| @glinui/tokens | **0.1.1** | pinned (latest) |
| fluidkit | **0.5.0** | pinned (latest) |
| @fontsource-variable/inter, @fontsource-variable/inter-tight | latest at install | bundled, no CDN |
| vitest + happy-dom | latest at install | logic tests |

## 7. Licences (for CREDITS.md)

- Backdrop (Kyant0): **Apache-2.0**
- Haze (Chris Banes): **Apache-2.0**
- Glin UI (GLINCKER): **MIT**
- fluidkit (runvendo): **MIT**
- Inter / Inter Tight: **SIL OFL 1.1** (bundled via @fontsource)
- Backdrop images in `shared/backdrops/`: AI-generated 2026-11-26, no third-party rights.
