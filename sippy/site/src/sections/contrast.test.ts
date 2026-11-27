import { describe, expect, it } from "vitest";
import indexCss from "../styles/index.css?inline";

type Rgb = [number, number, number];
type Rgba = [number, number, number, number];

function hexRgb(hex: string): Rgb {
  const h = hex.replace("#", "").trim();
  return [
    parseInt(h.slice(0, 2), 16),
    parseInt(h.slice(2, 4), 16),
    parseInt(h.slice(4, 6), 16),
  ];
}

function srgbLin(c: number): number {
  const s = c / 255;
  return s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
}

function luminance([r, g, b]: Rgb): number {
  return 0.2126 * srgbLin(r) + 0.7152 * srgbLin(g) + 0.0722 * srgbLin(b);
}

function contrastRatio(fg: Rgb, bg: Rgb): number {
  const l1 = luminance(fg);
  const l2 = luminance(bg);
  return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
}

function over([r, g, b, a]: Rgba, bg: Rgb): Rgb {
  return [
    r * a + bg[0] * (1 - a),
    g * a + bg[1] * (1 - a),
    b * a + bg[2] * (1 - a),
  ];
}

describe("WCAG AA (>= 4.5:1) small-text & CTA contrast in dark and light themes", () => {
  it("keeps no small-text rule on --text-3 and clears >= 4.5:1 across all sections", () => {
    // None of the small-text selectors may still reference var(--text-3)
    for (const sel of [
      "\\.download-note",
      "\\.download-disabled span",
      "\\.download-meta",
      "\\.download-sha",
      "\\.features \\.hint",
      "\\.milestone\\s*\\{",
      "\\.footer > p",
    ]) {
      const m = new RegExp(`${sel}[\\s\\S]*?\\}`, "m").exec(indexCss);
      expect(m, `selector ${sel} should exist`).not.toBeNull();
      expect(m![0]).not.toContain("var(--text-3)");
    }

    // Light mode composited stack (against worst-case bright aurora #eef3fc)
    const lightBg = hexRgb("#eef3fc");
    const lightGlass = over([255, 255, 255, 0.52], lightBg);
    const lightT1 = hexRgb("#0c1424");
    const lightT2 = hexRgb("#43506b");
    expect(contrastRatio(lightT2, lightGlass)).toBeGreaterThanOrEqual(4.5); // download-note, download-disabled span, bento p
    expect(contrastRatio(lightT2, lightBg)).toBeGreaterThanOrEqual(4.5); // features .hint, how-step p
    expect(
      contrastRatio(lightT2, over([...lightT1, 0.08], lightGlass)),
    ).toBeGreaterThanOrEqual(4.5); // .milestone, .sc-drink-ml
    expect(
      contrastRatio(
        hexRgb("#1648b8"),
        over([...hexRgb("#2e7cf6"), 0.18], lightGlass),
      ),
    ).toBeGreaterThanOrEqual(4.5); // .milestone-done light
    expect(
      contrastRatio(lightT2, over([...lightT1, 0.07], lightBg)),
    ).toBeGreaterThanOrEqual(4.5); // .footer > p light

    // Dark mode composited stack (against worst-case bright aurora #111f3c)
    const darkBg = hexRgb("#111f3c");
    const darkGlass = over([255, 255, 255, 0.08], darkBg);
    const darkT1 = hexRgb("#f4f7ff");
    const darkT2 = hexRgb("#a9b4ce");
    expect(contrastRatio(darkT2, darkGlass)).toBeGreaterThanOrEqual(4.5); // download-note, download-disabled span, bento p
    expect(contrastRatio(darkT2, darkBg)).toBeGreaterThanOrEqual(4.5); // features .hint, how-step p
    expect(
      contrastRatio(darkT2, over([...darkT1, 0.08], darkGlass)),
    ).toBeGreaterThanOrEqual(4.5); // .milestone, .sc-drink-ml
    expect(
      contrastRatio(
        hexRgb("#8ec0ff"),
        over([...hexRgb("#2e7cf6"), 0.18], darkGlass),
      ),
    ).toBeGreaterThanOrEqual(4.5); // .milestone-done dark
    expect(
      contrastRatio(darkT2, over([...darkT1, 0.07], darkBg)),
    ).toBeGreaterThanOrEqual(4.5); // .footer > p dark
  });
});
