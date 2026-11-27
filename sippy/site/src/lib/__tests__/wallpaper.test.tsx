import { afterEach, describe, expect, it } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import stylesCss from "../../styles/index.css?raw";
import { Backdrop } from "../../components/Backdrop";
import { EffectsProvider } from "../effects";
import { ThemeProvider, useTheme } from "../theme";
import { otherWallpaperTheme, wallpaperFor } from "../wallpaper";

afterEach(() => {
  cleanup();
  try {
    window.localStorage.clear();
  } catch {
    /* ignore */
  }
  document.documentElement.classList.remove("dark");
});

describe("wallpaperFor", () => {
  it("gives each theme its own distinct painted wallpaper", () => {
    const dark = wallpaperFor("dark");
    const light = wallpaperFor("light");
    expect(dark.src).toContain("assets/hero-caustics.webp");
    expect(light.src).toContain("assets/hero-caustics-light.webp");
    expect(light.src).not.toBe(dark.src);
    expect(light.theme).toBe("light");
    expect(dark.theme).toBe("dark");
  });

  it("ships the same three widths and never mixes themes in one srcset", () => {
    for (const theme of ["dark", "light"] as const) {
      const wp = wallpaperFor(theme);
      expect(wp.srcSet).toContain("640w");
      expect(wp.srcSet).toContain("960w");
      expect(wp.srcSet).toContain("1376w");
      expect(wp.width).toBe(1376);
      expect(wp.height).toBe(768);
    }
    const light = wallpaperFor("light");
    expect(light.srcSet).toContain("hero-caustics-light-640.webp");
    expect(light.srcSet).toContain("hero-caustics-light-960.webp");
    // the light srcset must not fall back to the night reef files
    expect(light.srcSet).not.toMatch(/hero-caustics-(640|960)\.webp/);
  });

  it("flips to the other theme", () => {
    expect(otherWallpaperTheme("dark")).toBe("light");
    expect(otherWallpaperTheme("light")).toBe("dark");
  });
});

function ThemeSwitch() {
  const { toggle, mode } = useTheme();
  return (
    <button type="button" onClick={toggle}>
      {mode}
    </button>
  );
}

describe("<Backdrop /> wallpaper", () => {
  it("paints the light wallpaper and swaps to the dark one on toggle", async () => {
    render(
      <ThemeProvider>
        <EffectsProvider>
          <Backdrop />
          <ThemeSwitch />
        </EffectsProvider>
      </ThemeProvider>,
    );

    const img = () =>
      document.querySelector<HTMLImageElement>("img.backdrop-caustics-img");

    // happy-dom reports no dark system preference → the default is light
    await waitFor(() => expect(screen.getByRole("button").textContent).toBe("light"));
    expect(img()?.getAttribute("data-wallpaper")).toBe("light");
    expect(img()?.getAttribute("src")).toContain("hero-caustics-light.webp");
    expect(img()?.getAttribute("width")).toBe("1376");

    fireEvent.click(screen.getByRole("button"));

    await waitFor(() => expect(screen.getByRole("button").textContent).toBe("dark"));
    expect(img()?.getAttribute("data-wallpaper")).toBe("dark");
    expect(img()?.getAttribute("src")).toContain("assets/hero-caustics.webp");
    expect(img()?.getAttribute("srcset")).not.toContain("light");
    expect(document.querySelector(".backdrop")?.getAttribute("data-wallpaper")).toBe(
      "dark",
    );
  });
});

describe("light-theme stylesheet wiring", () => {
  const rules = stylesCss.replace(/\/\*[\s\S]*?\*\//g, "");

  it("has a light-mode wallpaper layer and a pale scrim", () => {
    expect(rules).toContain(":root:not(.dark) .backdrop-caustics");
    expect(rules).toContain(":root:not(.dark) .backdrop-scrim");
    // no near-black veil left in the light scrim
    const lightScrim = /:root:not\(\.dark\) \.backdrop-scrim\s*\{[\s\S]*?\}/.exec(
      rules,
    )?.[0];
    expect(lightScrim).toBeDefined();
    expect(lightScrim).not.toContain("rgba(3, 6, 12");
  });

  it("flips the hero copy to ink on the light wallpaper", () => {
    expect(rules).toContain(":root:not(.dark) .hero-title");
    const lightTitle = /:root:not\(\.dark\) \.hero-title\s*\{[\s\S]*?\}/.exec(rules)?.[0];
    expect(lightTitle).toContain("var(--text-1)");
    const lightLead = /:root:not\(\.dark\) \.hero \.lead\s*\{[\s\S]*?\}/.exec(rules)?.[0];
    expect(lightLead).toContain("var(--text-2)");
    // the old white-lead override (for the always-dark hero) is gone
    expect(lightLead).not.toContain("#eef3ff");
  });

  it("uses a white cushion (not the old dark one) behind the light hero", () => {
    const cushion = /:root:not\(\.dark\) \.hero::before\s*\{[\s\S]*?\}/.exec(rules)?.[0];
    expect(cushion).toBeDefined();
    expect(cushion).toContain("rgba(255, 255, 255");
    expect(cushion).not.toContain("rgba(6, 12, 26");
  });
});
