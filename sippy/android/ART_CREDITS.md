# Friend and cosmetic art — how it was made, and what was thrown away

Everything here was generated for this app in round 7 (B3) with the image tool
available to the agent, then cut out and compressed locally. Nothing is
third-party art, and no font or icon from another project is bundled.

## Pipeline

1. Generate one character or one item per image on a flat pure-magenta field
   (`#FF00FF`), explicitly "no text, no watermark, nothing else in frame".
2. Chroma-key the magenta away with a flood fill seeded from the border only, so
   character colours that merely *look* magenta-ish (violet, lavender) survive.
   A second pass de-fringes any magenta left on the silhouette.
3. Crop to the alpha bounding box, pad to a square canvas, resize, save as WebP
   quality 88 (method 6).

Friends are 512 × 512, cosmetics 256 × 256. Every file is well under the
120 KB budget — the largest is `friend_turtle.webp` at 35 KB.

## Generated and shipped

| File | Prompt in one line |
| --- | --- |
| `friend_koi.webp` | round glossy koi, `#2E7CF6` body, coral-pink `#FF9A8F` patches, aqua highlights |
| `friend_otter.webp` | round glossy sea otter sitting upright, lagoon teal `#12A594`, mint belly |
| `friend_turtle.webp` | round glossy sea turtle standing, sea-green `#1FA971` shell, mint `#3ECF8E` skin |
| `friend_axolotl.webp` | round glossy axolotl with pale-periwinkle gills, violet `#7C5CFF` body |
| `item_koi_hat.webp` | tiny sailor hat, coral band, aqua anchor stud |
| `item_otter_hat.webp` | diving helmet, round porthole, teal brass rim |
| `item_turtle_hat.webp` | shell-shaped sun hat with a mint ribbon |
| `item_axolotl_hat.webp` | periwinkle party crown, violet tips |
| `item_koi_scarf.webp` | kelp scarf with a coral-pink knot |

## Rejected

| Attempt | Why it was thrown away |
| --- | --- |
| `axolotl_base.png` v1 | its lavender gills matched the magenta key, so the first cut ate holes around the head and left black halos. Regenerated with paler gills and a stricter key rule. |
| `otter_base.png` v1 | same prompt, but the fur drew a dark contact shadow into the magenta field; re-generated without a shadow. |
| `turtle_base.png` v1 | a thin magenta fringe was welded under the feet after keying; kept for a second pass, then cleaned by the de-fringe step and re-exported. |

## Not yet produced

Two more outfits were planned (otter, turtle) and a third (axolotl) was cut off
by the image tool's per-turn cap. See `PROGRESS.md` — the cosmetic table only
lists items whose art exists, so nothing in the app points at a missing file.

## Round 8 (B2, B4) — backdrop and drink icons

Same tool, same rule: generated here, compressed here, nothing third-party. The
PNG sources are not kept in the repository; the prompt below plus the pipeline is
enough to regenerate them, and the shipped WebP is the artefact.

| File | Size | Prompt in one line |
| --- | --- | --- |
| `res/drawable-nodpi/backdrop.webp` | 1080 × 2400, 35 KB | tall portrait underwater scene: aqua at the top with wide god rays, deep royal blue in the middle, near-black navy at the bottom, faint caustics and out-of-focus bubbles, no objects, no text |
| `res/drawable-nodpi/drink_*.webp` | 192 × 192, see below | glossy translucent 3D drink glyph on a transparent field, one per category |

Pipeline for the backdrop: `convert src.png -resize 1080x2400^ -gravity center
-extent 1080x2400 -strip -quality 84 backdrop.webp` (WebP, method 6). Budget was
250 KB; it lands at 35 KB, so there is room to raise quality later if the banding
ever shows on a phone.

### Drink icons — what actually worked

Asking for "a transparent background" does not produce transparency: the model
paints the *checkerboard* that means transparency, and it is then part of the
image. Keying that out leaves grey crumbs inside the glass and is not worth
shipping (tried on three icons, rejected). What works:

1. Ask for one flat solid magenta `#FF00FF` background, square canvas, no frame,
   no badge, no plate, no shadow. Three of the first nine attempts also came back
   with a rounded-square badge; the word "badge" and "plate" in the prompt fixed it.
2. Key it: `-alpha set -fuzz 24% -transparent "#FF00FF"`.
3. Trim, then fit: `-fuzz 30% -trim +repage -resize 192x192`.
4. **De-spill the magenta that shows through the glass.** A translucent body picks
   up the key colour and turns every rim pink — the one thing that makes an icon
   unusable. Per pixel, `m = min(R-G, B-G)`; where `m > 0`, subtract `m` from R and
   from B. Orange and amber drinks are untouched (`B < G` there), green and aqua
   are untouched, and the pink rims come back as the blue-violet a glass edge
   should be.
5. `-strip -quality 90`, which lands every icon between 5.7 and 9.2 KB — the budget
   was 40 KB.

| Icon | Size | Shipped |
| --- | --- | --- |
| `drink_water.webp` | 192px, 5.7 KB | yes |
| `drink_sparkling.webp` | 192px, 7.1 KB | yes |
| `drink_tea.webp` | 192px, 8.6 KB | yes |
| `drink_coffee.webp` | 192px, 9.2 KB | yes |
| `drink_juice.webp` | 192px, 8.6 KB | yes |
| `drink_milk.webp` | 192px, 7.3 KB | yes |
| `drink_smoothie.webp` | 192px, 7.8 KB | yes |
| `drink_soda.webp` | — | **not yet** — category falls back to the drawn droplet |
| `drink_alcohol.webp` | — | **not yet** — same fallback |
| `drink_other.webp` | — | **not yet** — same fallback |

Rejected along the way: nine earlier attempts with checkerboard transparency (all
discarded), and one milk where the key left a magenta slab on the right shoulder
that the de-spill turned grey enough to keep.
