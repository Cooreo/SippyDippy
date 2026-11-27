/**
 * Surface-break intro — unit tests (pure geometry + capability decision).
 *
 * Required by the brief:
 *  - the reveal centre equals the mark's centre at 360x640, 1280x720,
 *    1920x1080 and 2560x1440
 *  - the total timeline is at most 4500 ms
 *  - the reduced-motion path creates no WebGL context
 */
import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, render } from "@testing-library/react";
import { SurfaceBreakIntro } from "../components/intro/SurfaceBreakIntro";
import {
  INTRO,
  INTRO_MAX_MS,
  INTRO_TOTAL_MS,
  holeOpenFraction,
  introRenderer,
  markCentre,
  maxRevealRadius,
  revealCentre,
  revealRadius,
} from "./intro2";

const VIEWPORTS = [
  { w: 360, h: 640 },
  { w: 1280, h: 720 },
  { w: 1920, h: 1080 },
  { w: 2560, h: 1440 },
];

describe("surface-break reveal geometry", () => {
  it("reveal centre == mark centre at all four viewports", () => {
    for (const vp of VIEWPORTS) {
      const mc = markCentre(vp);
      // the reveal circle is defined by the SAME function as the mark
      expect(revealCentre(2.7, vp)).toEqual(mc);
      // and both are the viewport centre (the logo target)
      expect(mc).toEqual({ x: vp.w / 2, y: vp.h / 2 });
    }
  });

  it("hole is closed before 2.5 s and covers the viewport by 3.0 s", () => {
    for (const vp of VIEWPORTS) {
      expect(revealRadius(2.49, vp)).toBe(0);
      const full = revealRadius(3.0, vp);
      expect(full).toBeCloseTo(maxRevealRadius(vp), 3);
      expect(holeOpenFraction(3.0, vp)).toBeCloseTo(1, 3);
      // stays fully open until the end
      expect(revealRadius(4.2, vp)).toBe(full);
    }
  });
});

describe("surface-break timeline", () => {
  it("total is at most 4500 ms", () => {
    expect(INTRO_TOTAL_MS).toBeLessThanOrEqual(INTRO_MAX_MS);
    expect(Math.round(INTRO.done * 1000)).toBe(INTRO_TOTAL_MS);
    // storyboard beats are in order
    expect(INTRO.darkEnd).toBeLessThan(INTRO.riseEnd);
    expect(INTRO.riseEnd).toBeLessThan(INTRO.mergeEnd);
    expect(INTRO.mergeEnd).toBeLessThan(INTRO.breakEnd);
    expect(INTRO.breakEnd).toBeLessThan(INTRO.flyEnd);
    expect(INTRO.flyEnd).toBeLessThan(INTRO.done);
  });
});

describe("renderer decision", () => {
  it("reduced motion → 'none' (no canvas, hence no WebGL context)", () => {
    expect(
      introRenderer({
        reducedMotion: true,
        webgl: true,
        textureReady: true,
      }),
    ).toBe("none");
  });

  it("shader only when WebGL AND the texture is ready; else CSS", () => {
    expect(
      introRenderer({ reducedMotion: false, webgl: true, textureReady: true }),
    ).toBe("shader");
    expect(
      introRenderer({ reducedMotion: false, webgl: false, textureReady: true }),
    ).toBe("css");
    expect(
      introRenderer({ reducedMotion: false, webgl: true, textureReady: false }),
    ).toBe("css");
  });
});

describe("SurfaceBreakIntro under reduced motion", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders nothing and never creates a WebGL context", () => {
    const ctxSpy = vi
      .spyOn(HTMLCanvasElement.prototype, "getContext")
      .mockImplementation(() => null);
    const mql = {
      matches: true,
      media: "",
      onchange: null,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
      dispatchEvent: vi.fn(),
    } as unknown as MediaQueryList;
    vi.spyOn(window, "matchMedia").mockReturnValue(mql);

    const { container } = render(
      <SurfaceBreakIntro
        onReveal={() => {}}
        onSkip={() => {}}
        onDone={() => {}}
        perf={false}
      />,
    );

    expect(container.querySelector("canvas")).toBeNull();
    expect(ctxSpy).not.toHaveBeenCalled();
  });
});
