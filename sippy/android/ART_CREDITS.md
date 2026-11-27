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
