import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render } from "@testing-library/react";
import { ThemeProvider } from "./theme";
import { EffectsProvider } from "./effects";
import { enableDebugOutline } from "./debug";
import { HomePage } from "../pages/HomePage";
import { GalleryPage } from "../pages/GalleryPage";

describe("Phase 3 round 3 integration smoke", () => {
  const origIO = globalThis.IntersectionObserver;
  const origLocationSearch = window.location.search;

  beforeEach(() => {
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
    window.history.replaceState({}, "", window.location.pathname + origLocationSearch);
    document.documentElement.classList.remove("debug-outline");
    globalThis.IntersectionObserver = origIO;
    vi.restoreAllMocks();
  });

  it("renders HomePage cleanly with ?intro=0 and zero ?debug=outline overflow hints at 1280px and 360px", () => {
    window.history.replaceState({}, "", "/?intro=0&debug=outline");

    for (const width of [1280, 360]) {
      Object.defineProperty(window, "innerWidth", {
        configurable: true,
        writable: true,
        value: width,
      });
      const disableDebug = enableDebugOutline();
      const { container, unmount } = render(
        <ThemeProvider>
          <EffectsProvider>
            <HomePage />
          </EffectsProvider>
        </ThemeProvider>,
      );

      expect(document.documentElement.classList.contains("debug-outline")).toBe(
        true,
      );
      expect(container.querySelector(".sb")).toBeNull();
      expect(container.querySelectorAll(".sc-card")).toHaveLength(3);
      expect(
        container.querySelectorAll("[data-overflow-hint]"),
      ).toHaveLength(0);

      disableDebug();
      unmount();
    }
  });

  it("never renders the intro overlay on /#/gallery even with ?intro=1", () => {
    window.history.replaceState({}, "", "/?intro=1#/gallery");
    const { container } = render(
      <ThemeProvider>
        <EffectsProvider>
          <GalleryPage />
        </EffectsProvider>
      </ThemeProvider>,
    );
    expect(container.querySelector(".sb")).toBeNull();
  });
});
