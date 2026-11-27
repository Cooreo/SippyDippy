/**
 * Guards the JS token mirror against drift from shared/tokens.md
 * (single source of truth). If tokens.md changes, update tokens.ts and
 * the Compose app — then these expected values.
 */
import { describe, expect, it } from "vitest";
import { blur, durations, glassSaturation, pressScale, radius, springs } from "./tokens";

describe("tokens mirror shared/tokens.md", () => {
  it("radii: 999 / 28 / 20", () => {
    expect(radius).toEqual({ pill: 999, card: 28, inner: 20 });
  });
  it("blur tiers: 24 / 32 / 40 with 165% saturation", () => {
    expect(blur).toEqual({ sm: 24, md: 32, lg: 40 });
    expect(glassSaturation).toBe(165);
  });
  it("spring presets (stiffness/damping/mass)", () => {
    expect(springs.base).toEqual({ stiffness: 300, damping: 30, mass: 1 });
    expect(springs.gentle).toEqual({ stiffness: 220, damping: 24, mass: 1 });
    expect(springs.snappy).toEqual({ stiffness: 500, damping: 38, mass: 1 });
    expect(springs.float).toEqual({ stiffness: 120, damping: 16, mass: 1.2 });
  });
  it("durations never exceed the 600ms budget (outside intro/ambient)", () => {
    for (const ms of Object.values(durations)) {
      expect(ms).toBeLessThanOrEqual(600);
    }
  });
  it("press scale is 0.97", () => {
    expect(pressScale).toBe(0.97);
  });
});
