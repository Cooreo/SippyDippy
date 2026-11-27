/**
 * Debug helpers (`?debug=outline`, `?debug=perf`, `?debug=both`).
 *
 * Both are strictly OFF by default (zero DOM, zero rAF, zero observers unless
 * requested in the URL).
 */

function getDebugParam(): string | null {
  if (typeof window === "undefined") return null;
  const fromSearch = new URLSearchParams(window.location.search).get("debug");
  if (fromSearch) return fromSearch;
  const hashQuery = window.location.hash.split("?")[1];
  if (hashQuery) {
    return new URLSearchParams(hashQuery).get("debug");
  }
  return null;
}

export function debugOutlineRequested(): boolean {
  const p = getDebugParam();
  return p === "outline" || p === "both";
}

export function debugPerfRequested(): boolean {
  const p = getDebugParam();
  return p === "perf" || p === "both";
}

/**
 * ?debug=outline — outlines every element with a faint pink line and highlights
 * any element whose scrollWidth exceeds its clientWidth.
 */
export function enableDebugOutline(): () => void {
  const root = document.documentElement;
  root.classList.add("debug-outline");

  const mark = () => {
    document
      .querySelectorAll<HTMLElement>("[data-overflow-hint]")
      .forEach((el) => el.removeAttribute("data-overflow-hint"));
    document.querySelectorAll<HTMLElement>("body *").forEach((el) => {
      if (el.clientHeight > 0 && el.scrollWidth > el.clientWidth + 1) {
        el.setAttribute(
          "data-overflow-hint",
          `${el.scrollWidth - el.clientWidth}px`,
        );
      }
    });
  };

  mark();
  const interval = window.setInterval(mark, 1500);
  window.addEventListener("resize", mark);
  window.addEventListener("load", mark);

  return () => {
    window.clearInterval(interval);
    window.removeEventListener("resize", mark);
    window.removeEventListener("load", mark);
    root.classList.remove("debug-outline");
    document
      .querySelectorAll<HTMLElement>("[data-overflow-hint]")
      .forEach((el) => el.removeAttribute("data-overflow-hint"));
  };
}

function readActiveQualityLabel(): string {
  const root = document.documentElement;
  if (
    root.hasAttribute("data-emulate-motion") ||
    window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches
  ) {
    return "reduced";
  }
  return root.dataset.quality || "high";
}

/**
 * ?debug=perf — lightweight corner HUD showing:
 *  - rolling 2-second FPS
 *  - worst frame delta in the last 5 seconds
 *  - long tasks (> 50 ms) since load
 *  - active quality tier
 *
 * Updates DOM text nodes directly (~4 Hz) with zero React state updates and
 * zero backdrop-filter so the HUD itself adds negligible overhead.
 */
export function enableDebugPerfOverlay(): () => void {
  const box = document.createElement("aside");
  box.className = "perf-hud";
  box.setAttribute("aria-label", "Performance debug overlay");
  box.innerHTML = [
    `<div class="perf-hud-row"><span>FPS (2s)</span><b data-perf="fps">--</b></div>`,
    `<div class="perf-hud-row"><span>Worst (5s)</span><b data-perf="worst">--</b></div>`,
    `<div class="perf-hud-row"><span>Long &gt;50ms</span><b data-perf="long">0</b></div>`,
    `<div class="perf-hud-row"><span>Tier</span><b data-perf="tier">${readActiveQualityLabel()}</b></div>`,
  ].join("");
  document.body.appendChild(box);

  const fpsEl = box.querySelector<HTMLElement>('[data-perf="fps"]')!;
  const worstEl = box.querySelector<HTMLElement>('[data-perf="worst"]')!;
  const longEl = box.querySelector<HTMLElement>('[data-perf="long"]')!;
  const tierEl = box.querySelector<HTMLElement>('[data-perf="tier"]')!;

  const samples: Array<{ t: number; dt: number }> = [];
  let longTasks = 0;
  let hasLongTaskObserver = false;
  let po: PerformanceObserver | null = null;

  if (
    typeof PerformanceObserver !== "undefined" &&
    Array.isArray(PerformanceObserver.supportedEntryTypes) &&
    PerformanceObserver.supportedEntryTypes.includes("longtask")
  ) {
    try {
      po = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (entry.duration > 50) longTasks += 1;
        }
      });
      po.observe({ type: "longtask", buffered: true });
      hasLongTaskObserver = true;
    } catch {
      hasLongTaskObserver = false;
    }
  }

  let raf = 0;
  let running = !document.hidden;
  let lastFrame = performance.now();
  let lastPaint = 0;
  let skipNextDelta = true;

  const tick = (now: number) => {
    if (!running) return;
    const dt = now - lastFrame;
    lastFrame = now;

    if (skipNextDelta) {
      skipNextDelta = false;
    } else if (dt > 0) {
      samples.push({ t: now, dt });
      if (!hasLongTaskObserver && dt > 50) {
        longTasks += 1;
      }
      // Trim samples older than 5 seconds
      const cutoff5s = now - 5000;
      while (samples.length > 0 && samples[0].t < cutoff5s) {
        samples.shift();
      }
    }

    if (now - lastPaint >= 250) {
      lastPaint = now;
      const cutoff2s = now - 2000;
      let count2s = 0;
      let sum2s = 0;
      let worst5s = 0;
      for (let i = 0; i < samples.length; i++) {
        const s = samples[i];
        if (s.dt > worst5s) worst5s = s.dt;
        if (s.t >= cutoff2s) {
          count2s += 1;
          sum2s += s.dt;
        }
      }
      const fps = sum2s > 0 ? Math.round((count2s * 1000) / sum2s) : 0;
      fpsEl.textContent = count2s > 0 ? String(fps) : "--";
      worstEl.textContent =
        samples.length > 0 ? `${worst5s.toFixed(1)} ms` : "--";
      longEl.textContent = String(longTasks);
      tierEl.textContent = readActiveQualityLabel();
    }

    raf = requestAnimationFrame(tick);
  };

  const onVisibility = () => {
    if (document.hidden) {
      running = false;
      cancelAnimationFrame(raf);
    } else if (!running) {
      running = true;
      lastFrame = performance.now();
      skipNextDelta = true;
      raf = requestAnimationFrame(tick);
    }
  };
  document.addEventListener("visibilitychange", onVisibility);

  if (running) {
    raf = requestAnimationFrame(tick);
  }

  return () => {
    running = false;
    cancelAnimationFrame(raf);
    document.removeEventListener("visibilitychange", onVisibility);
    po?.disconnect();
    box.remove();
  };
}
