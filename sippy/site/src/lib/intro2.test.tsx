/**
 * Surface-break intro — Round 3 unit tests.
 *
 * Verifies every invariant in Section 4 / SPEC.md section 3:
 *  1. Overlay opacity reaches 0 BEFORE removal (`onDone`), for full play,
 *     150 ms skip, and 500 ms reduced-motion crossfade.
 *  2. No keyframe or `.sb*` rule or shader uses `#000` / pure black.
 *  3. Final frame colour equals the page backdrop colour (and WebGL clears
 *     to the darkest backdrop tone at `alpha = 1`, never `(0, 0, 0, 0)`).
 *  4. Reveal centre equals mark centre at 360x640, 1280x720, 1920x1080,
 *     and 2560x1440.
 *  5. Total timeline <= 4500 ms (with 800 ms exit crossfade 3.0–3.8 s).
 *  6. Reduced motion creates no `<canvas>` and no WebGL context.
 */
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render } from "@testing-library/react";
import indexCss from "../styles/index.css?inline";
import shaderSource from "./intro-shader.ts?raw";
import { SurfaceBreakIntro } from "../components/intro/SurfaceBreakIntro";
import { createIntroGL } from "./intro-shader";
import {
  DARKEST_BACKDROP_RGB,
  INTRO,
  INTRO_CROSSFADE_MS,
  INTRO_MAX_MS,
  INTRO_REDUCED_FADE_MS,
  INTRO_SKIP_FADE_MS,
  INTRO_TOTAL_MS,
  compositeIntroFrameColor,
  holeOpenFraction,
  introRenderer,
  markCentre,
  maxRevealRadius,
  overlayOpacity,
  parseCssColorToRgb01,
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
  it("reveal centre == mark centre at 360x640, 1280x720, 1920x1080, 2560x1440", () => {
    for (const vp of VIEWPORTS) {
      const mc = markCentre(vp);
      expect(revealCentre(2.5, vp)).toEqual(mc);
      expect(revealCentre(2.75, vp)).toEqual(mc);
      expect(revealCentre(3.0, vp)).toEqual(mc);
      expect(mc).toEqual({ x: vp.w / 2, y: vp.h / 2 });
    }
  });

  it("hole is closed before 2.5 s and covers the viewport by 3.0 s", () => {
    for (const vp of VIEWPORTS) {
      expect(revealRadius(2.49, vp)).toBe(0);
      const full = revealRadius(3.0, vp);
      expect(full).toBeCloseTo(maxRevealRadius(vp), 3);
      expect(holeOpenFraction(3.0, vp)).toBeCloseTo(1, 3);
      expect(revealRadius(3.8, vp)).toBe(full);
    }
  });
});

describe("surface-break timeline", () => {
  it("total timeline is at most 4500 ms and last crossfade is 800 ms", () => {
    expect(INTRO_TOTAL_MS).toBeLessThanOrEqual(INTRO_MAX_MS);
    expect(INTRO_CROSSFADE_MS).toBe(800);
    expect(Math.round(INTRO.done * 1000)).toBe(INTRO_TOTAL_MS);
    expect(INTRO.darkEnd).toBeLessThan(INTRO.riseEnd);
    expect(INTRO.riseEnd).toBeLessThan(INTRO.mergeEnd);
    expect(INTRO.mergeEnd).toBeLessThan(INTRO.breakEnd);
    expect(INTRO.breakEnd).toBeLessThan(INTRO.flyEnd);
    expect(INTRO.flyEnd).toBeLessThan(INTRO.done);
  });
});

describe("colour & no-black-frame invariants", () => {
  it("no @keyframes or .sb intro rule uses #000 / pure black", () => {
    const keyframes = [
      ...indexCss.matchAll(/@keyframes\s+[a-z0-9-]+\s*\{([\s\S]*?\n\})/gi),
    ];
    expect(keyframes.length).toBeGreaterThan(0);
    for (const [block] of keyframes) {
      expect(block).not.toMatch(/#000(?:000)?\b/i);
      expect(block).not.toMatch(/rgba?\(\s*0\s*,\s*0\s*,\s*0(?:\s*,\s*1)?\s*\)/i);
    }

    // Intro `.sb*` rules must never paint `#000` or `#020409`
    const sbRules = [...indexCss.matchAll(/\.sb[a-z0-9-]*\s*\{[^}]*\}/gi)];
    expect(sbRules.length).toBeGreaterThan(5);
    for (const [rule] of sbRules) {
      expect(rule).not.toMatch(/#000(?:000)?\b/i);
      expect(rule).not.toMatch(/#020409/i);
    }
    expect(indexCss).toMatch(/\.sb\s*\{[^}]*background:\s*transparent/);

    // Shader source must not hardcode vec3(0.0) or #000 deep water
    expect(shaderSource).not.toMatch(/vec3\(\s*0\.0\s*,\s*0\.0\s*,\s*0\.0\s*\)/);
  });

  it("final frame colour equals the page backdrop colour in dark and light themes", () => {
    const darkBackdrop = parseCssColorToRgb01("#060a14")!;
    const lightBackdrop = parseCssColorToRgb01("#e3ebfa")!;

    // Darkest frame at t = 0 is strictly > 0 in every channel (#060a14, never #000)
    expect(DARKEST_BACKDROP_RGB.r).toBeGreaterThan(0);
    expect(DARKEST_BACKDROP_RGB.g).toBeGreaterThan(0);
    expect(DARKEST_BACKDROP_RGB.b).toBeGreaterThan(0);

    for (const vp of VIEWPORTS) {
      for (const pageBg of [darkBackdrop, lightBackdrop]) {
        const atCrossfadeEnd = compositeIntroFrameColor(
          INTRO.flyEnd,
          vp,
          pageBg,
        );
        const atDone = compositeIntroFrameColor(INTRO.done, vp, pageBg);
        expect(atCrossfadeEnd.r).toBeCloseTo(pageBg.r, 5);
        expect(atCrossfadeEnd.g).toBeCloseTo(pageBg.g, 5);
        expect(atCrossfadeEnd.b).toBeCloseTo(pageBg.b, 5);
        expect(atDone).toEqual(pageBg);
      }
    }
  });

  it("clears WebGL canvas to darkest backdrop tone at alpha = 1 (never transparent black)", () => {
    const clearColor = vi.fn();
    const clear = vi.fn();
    const mockGl = {
      COLOR_BUFFER_BIT: 0x4000,
      VERTEX_SHADER: 0x8b31,
      FRAGMENT_SHADER: 0x8b30,
      COMPILE_STATUS: 0x8b81,
      LINK_STATUS: 0x8b82,
      ARRAY_BUFFER: 0x8892,
      STATIC_DRAW: 0x88e4,
      FLOAT: 0x1406,
      TEXTURE0: 0x84c0,
      TEXTURE_2D: 0x0de1,
      TRIANGLES: 0x0004,
      drawingBufferWidth: 1280,
      drawingBufferHeight: 720,
      clearColor,
      clear,
      createShader: () => ({}),
      shaderSource: () => {},
      compileShader: () => {},
      getShaderParameter: () => true,
      createProgram: () => ({}),
      attachShader: () => {},
      linkProgram: () => {},
      getProgramParameter: () => true,
      useProgram: () => {},
      createBuffer: () => ({}),
      bindBuffer: () => {},
      bufferData: () => {},
      getAttribLocation: () => 0,
      enableVertexAttribArray: () => {},
      vertexAttribPointer: () => {},
      getUniformLocation: () => ({}),
      createTexture: () => ({}),
      activeTexture: () => {},
      bindTexture: () => {},
      uniform1i: () => {},
      uniform1f: () => {},
      uniform2f: () => {},
      uniform3f: () => {},
      drawArrays: () => {},
      viewport: () => {},
      deleteTexture: () => {},
      deleteBuffer: () => {},
      deleteProgram: () => {},
      deleteShader: () => {},
      getExtension: () => null,
    };

    const canvas = document.createElement("canvas");
    vi.spyOn(canvas, "getContext").mockReturnValue(
      mockGl as unknown as WebGLRenderingContext,
    );

    const introGl = createIntroGL(canvas);
    expect(introGl).not.toBeNull();
    expect(clearColor).toHaveBeenCalledWith(
      DARKEST_BACKDROP_RGB.r,
      DARKEST_BACKDROP_RGB.g,
      DARKEST_BACKDROP_RGB.b,
      1.0,
    );
    expect(clear).toHaveBeenCalledWith(0x4000);
    introGl?.dispose();
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

describe("overlay opacity reaches 0 strictly BEFORE removal", () => {
  beforeEach(() => {
    document.documentElement.removeAttribute("data-emulate-motion");
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("fades overlay opacity to 0 over 3.0–3.8 s before calling onDone at end", () => {
    expect(overlayOpacity(2.5)).toBe(1);
    expect(overlayOpacity(3.0)).toBe(1);
    expect(overlayOpacity(3.5)).toBeGreaterThan(0.2);
    expect(overlayOpacity(3.5)).toBeLessThan(0.8);
    expect(overlayOpacity(3.8)).toBe(0);

    let rafCb: FrameRequestCallback | null = null;
    vi.spyOn(window, "requestAnimationFrame").mockImplementation((cb) => {
      rafCb = cb;
      return 1;
    });
    let now = 1000;
    vi.spyOn(performance, "now").mockImplementation(() => now);

    let opacityAtDone = "unset";
    let overlayEl: HTMLElement | null = null;
    const onDone = vi.fn(() => {
      opacityAtDone = overlayEl?.style.opacity ?? "missing";
    });
    const onReveal = vi.fn();

    const { container } = render(
      <SurfaceBreakIntro
        onReveal={onReveal}
        onSkip={() => {}}
        onDone={onDone}
        perf={false}
      />,
    );
    overlayEl = container.querySelector(".sb");
    expect(overlayEl).not.toBeNull();

    // At 3.5 s: mid-crossfade, NOT removed yet
    now = 1000 + 3500;
    rafCb!(now);
    expect(onReveal).toHaveBeenCalledTimes(1);
    expect(Number(overlayEl!.style.opacity)).toBeGreaterThan(0);
    expect(onDone).not.toHaveBeenCalled();

    // At 3.8 s: opacity hits "0", guard waits one painted frame before removal
    now = 1000 + 3800;
    rafCb!(now);
    expect(overlayEl!.style.opacity).toBe("0");
    expect(onDone).not.toHaveBeenCalled();

    // At 3.85 s (`INTRO.done`): now that opacity === "0" was painted, `onDone` fires
    now = 1000 + INTRO_TOTAL_MS;
    rafCb!(now);
    expect(onDone).toHaveBeenCalledTimes(1);
    expect(opacityAtDone).toBe("0");
  });

  it("skip crossfades overlay opacity to 0 over 150 ms before removal", () => {
    let rafCb: FrameRequestCallback | null = null;
    vi.spyOn(window, "requestAnimationFrame").mockImplementation((cb) => {
      rafCb = cb;
      return 1;
    });
    let now = 2000;
    vi.spyOn(performance, "now").mockImplementation(() => now);

    let opacityAtDone = "unset";
    let overlayEl: HTMLElement | null = null;
    const onSkip = vi.fn();
    const onDone = vi.fn(() => {
      opacityAtDone = overlayEl?.style.opacity ?? "missing";
    });

    const { container } = render(
      <SurfaceBreakIntro
        onReveal={() => {}}
        onSkip={onSkip}
        onDone={onDone}
        perf={false}
      />,
    );
    overlayEl = container.querySelector(".sb");

    // Press Escape at t = 0.8 s
    now = 2800;
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onSkip).toHaveBeenCalledTimes(1);
    expect(onDone).not.toHaveBeenCalled();

    // Mid-skip (75 ms): fading smoothly, not removed
    now = 2800 + 75;
    rafCb!(now);
    expect(Number(overlayEl!.style.opacity)).toBeGreaterThan(0);
    expect(Number(overlayEl!.style.opacity)).toBeLessThan(1);
    expect(onDone).not.toHaveBeenCalled();

    // 150 ms: opacity reaches "0" first
    now = 2800 + INTRO_SKIP_FADE_MS;
    rafCb!(now);
    expect(overlayEl!.style.opacity).toBe("0");
    expect(onDone).not.toHaveBeenCalled();

    // Next frame: unmounts after opacity === "0"
    now = 2800 + INTRO_SKIP_FADE_MS + 16;
    rafCb!(now);
    expect(onDone).toHaveBeenCalledTimes(1);
    expect(opacityAtDone).toBe("0");
  });

  it("reduced motion plays a 500 ms mark-to-page crossfade with NO canvas and NO WebGL context", () => {
    const ctxSpy = vi
      .spyOn(HTMLCanvasElement.prototype, "getContext")
      .mockImplementation(() => null);
    let rafCb: FrameRequestCallback | null = null;
    vi.spyOn(window, "requestAnimationFrame").mockImplementation((cb) => {
      rafCb = cb;
      return 1;
    });
    let now = 5000;
    vi.spyOn(performance, "now").mockImplementation(() => now);

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

    let opacityAtDone = "unset";
    let overlayEl: HTMLElement | null = null;
    const onDone = vi.fn(() => {
      opacityAtDone = overlayEl?.style.opacity ?? "missing";
    });

    const { container } = render(
      <SurfaceBreakIntro
        onReveal={() => {}}
        onSkip={() => {}}
        onDone={onDone}
        perf={false}
      />,
    );

    overlayEl = container.querySelector(".sb");
    expect(overlayEl).not.toBeNull();
    expect(container.querySelector("canvas")).toBeNull();
    expect(container.querySelector(".sb-ring")).toBeNull();
    expect(container.querySelector(".sb-mark")).not.toBeNull();
    expect(ctxSpy).not.toHaveBeenCalled();

    // Mid-crossfade at 250 ms
    now = 5000 + 250;
    rafCb!(now);
    expect(Number(overlayEl!.style.opacity)).toBeGreaterThan(0.1);
    expect(onDone).not.toHaveBeenCalled();

    // 500 ms: opacity hits "0", then unmounts on next frame
    now = 5000 + INTRO_REDUCED_FADE_MS;
    rafCb!(now);
    expect(overlayEl!.style.opacity).toBe("0");
    expect(onDone).not.toHaveBeenCalled();

    now = 5000 + INTRO_REDUCED_FADE_MS + 16;
    rafCb!(now);
    expect(onDone).toHaveBeenCalledTimes(1);
    expect(opacityAtDone).toBe("0");
  });
});
