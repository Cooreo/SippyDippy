import { describe, expect, it } from "vitest";
import {
  RIPPLE_COUNT,
  dropTip,
  impactPoint,
  rippleCenter,
} from "./intro-impact";

/**
 * Proves the droplet's impact point and EVERY ripple centre are the same
 * point at each required viewport size. The logo target is assumed to sit
 * at the exact centre of the intro (CSS grid place-items: center on the
 * fixed full-viewport overlay), so its rect is derived from the viewport —
 * exactly what Intro.tsx measures at runtime.
 */
const LOGO_SIZE = 88;
const SIZES: [number, number][] = [
  [360, 640],
  [1280, 720],
  [1920, 1080],
  [2560, 1440],
];

describe("intro impact point", () => {
  for (const [w, h] of SIZES) {
    it(`droplet tip == all ${RIPPLE_COUNT} ripple centres at ${w}x${h}`, () => {
      const logo = {
        left: (w - LOGO_SIZE) / 2,
        top: (h - LOGO_SIZE) / 2,
        width: LOGO_SIZE,
        height: LOGO_SIZE,
      };
      const p = impactPoint(logo, { width: w, height: h });
      const tip = dropTip(p);
      const ripples = Array.from({ length: RIPPLE_COUNT }, (_, i) =>
        rippleCenter(p, i),
      );

      console.log(
        `[${w}x${h}] impact=(${p.x}, ${p.y}) ` +
          `dropTip=(${tip.x}, ${tip.y}) ` +
          `ripples=${ripples.map((r) => `(${r.x}, ${r.y})`).join(" ")}`,
      );

      expect(tip.x).toBe(p.x);
      expect(tip.y).toBe(p.y);
      for (const r of ripples) {
        expect(r.x).toBe(p.x);
        expect(r.y).toBe(p.y);
      }
      // the point is the logo centre
      expect(p.x).toBe(w / 2);
      expect(p.y).toBe(h / 2);
    });
  }

  it("clamps to the viewport when the logo rect is off-screen", () => {
    const p = impactPoint(
      { left: -200, top: -200, width: 50, height: 50 },
      { width: 360, height: 640 },
    );
    expect(p.x).toBe(0);
    expect(p.y).toBe(0);
  });
});
