# Aurora mesh backdrop — implementation spec (site, Phase 2)

A deliberately designed backdrop under every glass surface (brief: "glass is invisible on a flat background").
Canvas 2D, no WebGL needed. Reference: `fluidkit` `MeshGradient` behaviour (large blurred radial blobs drifting slowly).

## Geometry
- Full-bleed canvas, `position: fixed`, `z-index: 0`, pointer-events none.
- **4 blobs**: radial gradients (centre colour → transparent), radius 30–50vw.
  - Hues: accent 214° (Sippy Blue), violet 268°, teal 185°, magenta 300° (magenta at low alpha).
  - Dark theme alpha 0.35–0.55, light theme 0.20–0.35 (lighter composite).
- Base fill under blobs: `--bg-0`.

## Motion
- Each blob position = anchor + sum of two sines (different periods per axis: 13s/17s/21s/23s per blob, phase offsets) → organic drift, no loops visible.
- Blob radii breathe ±6% over 9–14s.
- rAF loop is **time-based** (dt from performance.now), capped at 30fps by frame skipping (backdrop ambience does not need 60fps; saves battery/thermals).
- Pointer parallax: blobs shift ±8–14px opposite to pointer, spring-lagged (stiffness 60, damping 20). Disabled on touch devices and under reduced motion.
- Pause the loop when: tab hidden, canvas off-screen (IntersectionObserver), `prefers-reduced-motion` (render one static frame).
- `prefers-reduced-transparency`: still render the aurora (it's the *backdrop*, not glass) — the opaque fallback applies to glass surfaces.

## Performance rules
- Single canvas; DPR capped at 1.5; canvas sized to viewport only.
- Gradients pre-rendered to offscreen canvases per blob (redrawn only when radius changes > 2px) — per-frame work = 4 drawImage + composite.
- Composite mode: `screen` on dark, `multiply`-tinted on light (or `screen` at low alpha — decide by eye in Phase 2).
- The aurora canvas is NOT counted against the "6 backdrop-filter surfaces" budget (it has no backdrop-filter).
- Blur: blobs are radial gradients (inherently soft) — no CSS blur on the canvas itself.

## Integration
- Mount once at app root, below everything, above body background.
- Hero + bento + download sections sit directly on it; sections with photographic backdrops (showcase) layer their own blurred image above the canvas.
- Reduced-motion static frame must still look composed (choose anchor positions for a good still).
