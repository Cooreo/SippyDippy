/**
 * ?debug=outline — screenshot aid (Phase 3 fix G).
 *
 * Adds `html.debug-outline`, which outlines every element with a faint
 * pink line, and re-scan every 1.5 s (and on resize) marking any element
 * whose scrollWidth exceeds its clientWidth with a bright red outline +
 * data-overflow-hint="<px>" so layout problems are visible in screenshots
 * without a devtools panel.
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

export function debugOutlineRequested(): boolean {
  const p = new URLSearchParams(window.location.search).get("debug");
  return p === "outline" || p === "both";
}

/** ?debug=perf — log intro frame times to the console (sendable numbers). */
export function debugPerfRequested(): boolean {
  const p = new URLSearchParams(window.location.search).get("debug");
  return p === "perf" || p === "both";
}
