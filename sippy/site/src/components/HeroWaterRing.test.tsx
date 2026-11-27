import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { render, cleanup } from "@testing-library/react";
import indexCss from "../styles/index.css?inline";
import { HeroWaterRing } from "./HeroWaterRing";
import {
  ORB_BUBBLES,
  ORB_RISE_MS,
  ORB_HOLD_MS,
  ORB_CYCLE_MS,
  easeOrbRise,
} from "../lib/orbMotion";

describe("HeroWaterRing (Section 3)", () => {
  const origIO = globalThis.IntersectionObserver;

  beforeEach(() => {
    document.documentElement.removeAttribute("data-emulate-motion");
    class MockIO {
      observe = vi.fn();
      disconnect = vi.fn();
      unobserve = vi.fn();
      takeRecords = () => [];
    }
    globalThis.IntersectionObserver =
      MockIO as unknown as typeof IntersectionObserver;
  });

  afterEach(() => {
    cleanup();
    globalThis.IntersectionObserver = origIO;
    vi.restoreAllMocks();
  });

  it("renders two wave layers, at most 8 rising bubbles, and splash + glow layers", () => {
    const { container } = render(<HeroWaterRing />);
    expect(ORB_BUBBLES.length).toBeLessThanOrEqual(8);
    expect(container.querySelectorAll(".hr-wave")).toHaveLength(2);
    expect(container.querySelector(".hr-wave--back")).not.toBeNull();
    expect(container.querySelector(".hr-wave--front")).not.toBeNull();
    expect(container.querySelectorAll(".hr-bubble").length).toBeLessThanOrEqual(
      8,
    );
    expect(container.querySelector(".hr-glow")).not.toBeNull();
    expect(container.querySelector(".hr-fill-vivid")).not.toBeNull();
    expect(container.querySelector(".hr-splash")).not.toBeNull();
  });

  it("decelerates near the top, holds at 100% with 'Goal reached' + splash, then drains", () => {
    // Decelerating curve: first half covers > 80 %, second half slows smoothly
    expect(easeOrbRise(0.5)).toBeGreaterThan(0.8);
    expect(easeOrbRise(1) - easeOrbRise(0.85)).toBeLessThan(
      easeOrbRise(0.15) - easeOrbRise(0),
    );

    let rafCb: FrameRequestCallback | null = null;
    vi.spyOn(window, "requestAnimationFrame").mockImplementation((cb) => {
      rafCb = cb;
      return 1;
    });
    vi.spyOn(performance, "now").mockReturnValue(1000);

    const { container } = render(<HeroWaterRing />);
    const orb = container.querySelector(".hr") as HTMLElement;
    const num = container.querySelector(".hr-num") as HTMLElement;
    const goal = container.querySelector(".hr-goal") as HTMLElement;

    expect(rafCb).not.toBeNull();

    // Mid-rise (~2.25 s)
    rafCb!(1000 + ORB_RISE_MS * 0.5);
    expect(Number(orb.style.getPropertyValue("--hr-progress"))).toBeGreaterThan(
      0.8,
    );
    expect(orb.classList.contains("is-done")).toBe(false);
    expect(goal.style.opacity).toBe("0");

    // Hold at 100 % (4.5 s .. 6.0 s)
    rafCb!(1000 + ORB_RISE_MS + 200);
    expect(orb.style.getPropertyValue("--hr-progress")).toBe("1.000");
    expect(orb.classList.contains("is-done")).toBe(true);
    expect(goal.style.opacity).toBe("1");
    expect(num.style.opacity).toBe("0");

    // Mid-drain (~6.55 s)
    rafCb!(1000 + ORB_RISE_MS + ORB_HOLD_MS + 550);
    const drainProgress = Number(orb.style.getPropertyValue("--hr-progress"));
    expect(drainProgress).toBeGreaterThan(0.1);
    expect(drainProgress).toBeLessThan(0.9);
    expect(orb.classList.contains("is-done")).toBe(false);

    // Cycle restart
    rafCb!(1000 + ORB_CYCLE_MS + 10);
    expect(Number(orb.style.getPropertyValue("--hr-progress"))).toBeLessThan(
      0.05,
    );
  });

  it("freezes at static percentage with data-reduced under emulated reduced motion", () => {
    document.documentElement.setAttribute("data-emulate-motion", "");
    const { container } = render(<HeroWaterRing />);
    const orb = container.querySelector(".hr") as HTMLElement;
    const num = container.querySelector(".hr-num") as HTMLElement;
    expect(orb.dataset.reduced).toBe("true");
    expect(num.textContent).toBe("68%");
    expect(orb.style.getPropertyValue("--hr-progress")).toBe("0.680");
    expect(orb.classList.contains("is-done")).toBe(false);
  });

  it("enforces SPEC.md rule: every @keyframes hr-* rule animates ONLY transform and opacity", () => {
    const keyframeRegex =
      /@keyframes\s+(hr-[a-z0-9-]+)\s*\{([\s\S]*?\n\})/g;
    const matches = [...indexCss.matchAll(keyframeRegex)];
    expect(matches.length).toBeGreaterThanOrEqual(5);

    for (const [, name, body] of matches) {
      const propMatches = [...body.matchAll(/^\s*([a-z-]+)\s*:/gm)];
      for (const [, prop] of propMatches) {
        expect(
          prop === "transform" || prop === "opacity",
          `@keyframes ${name} animated disallowed property "${prop}"`,
        ).toBe(true);
      }
    }
  });
});
