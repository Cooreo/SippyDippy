import stylesheet from "../../styles/index.css?raw";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  parseQualityOverride,
  resolveQualityTier,
  syncDocHiddenDataset,
  syncQualityDataset,
} from "../quality";

interface EnvOptions {
  width?: number;
  cores?: number;
  memory?: number;
  touch?: boolean;
  search?: string;
}

function stubEnv({
  width = 1280,
  cores = 8,
  memory,
  touch = false,
  search = "",
}: EnvOptions = {}) {
  const matchMedia = (query: string) => ({
    matches: touch
      ? query.includes("hover: none") || query.includes("pointer: coarse")
      : query.includes("hover: hover"),
    media: query,
    onchange: null,
    addEventListener: () => {},
    removeEventListener: () => {},
    addListener: () => {},
    removeListener: () => {},
    dispatchEvent: () => false,
  });
  vi.stubGlobal("window", {
    innerWidth: width,
    location: { search, hash: "", href: `http://localhost/${search}` },
    navigator: { hardwareConcurrency: cores, deviceMemory: memory },
    matchMedia,
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
  delete document.documentElement.dataset.quality;
  delete document.documentElement.dataset.docHidden;
});

describe("parseQualityOverride", () => {
  it("reads ?quality=low|high from the search string", () => {
    expect(parseQualityOverride("?quality=low", "")).toBe("low");
    expect(parseQualityOverride("?a=1&quality=high", "")).toBe("high");
  });

  it("reads the query out of a hash route", () => {
    expect(parseQualityOverride("", "#/?quality=low")).toBe("low");
  });

  it("ignores missing or unknown values", () => {
    expect(parseQualityOverride("", "")).toBeNull();
    expect(parseQualityOverride("?quality=medium", "")).toBeNull();
    expect(parseQualityOverride("?quality=", "")).toBeNull();
  });
});

describe("resolveQualityTier", () => {
  it("is high on a wide desktop with a fine pointer and enough cores", () => {
    stubEnv();
    expect(resolveQualityTier()).toBe("high");
  });

  it("honours the ?quality= override over hardware", () => {
    stubEnv({ search: "?quality=low" });
    expect(resolveQualityTier()).toBe("low");
    stubEnv({ search: "?quality=high", width: 360, cores: 2, touch: true });
    expect(resolveQualityTier()).toBe("high");
  });

  it("is low on phones (narrow viewport or touch-only)", () => {
    stubEnv({ width: 390 });
    expect(resolveQualityTier()).toBe("low");
    stubEnv({ touch: true });
    expect(resolveQualityTier()).toBe("low");
  });

  it("is low on weak hardware", () => {
    stubEnv({ cores: 2 });
    expect(resolveQualityTier()).toBe("low");
    stubEnv({ memory: 4 });
    expect(resolveQualityTier()).toBe("low");
  });
});

describe("dataset sync", () => {
  it("mirrors the tier onto <html data-quality>", () => {
    expect(syncQualityDataset("low")).toBe("low");
    expect(document.documentElement.dataset.quality).toBe("low");
    syncQualityDataset("high");
    expect(document.documentElement.dataset.quality).toBe("high");
  });

  it("sets data-doc-hidden only while the tab is hidden", () => {
    const hidden = vi.spyOn(document, "hidden", "get");
    hidden.mockReturnValue(true);
    syncDocHiddenDataset();
    expect(document.documentElement.dataset.docHidden).toBe("true");
    hidden.mockReturnValue(false);
    syncDocHiddenDataset();
    expect(document.documentElement.dataset.docHidden).toBeUndefined();
    hidden.mockRestore();
  });
});

describe("stylesheet quality gates", () => {
  const css = stylesheet as string;
  // declarations only — the prose in comments may mention the old values
  const rules = css.replace(/\/\*[\s\S]*?\*\//g, "");

  it("kills live backdrop-filter on low tier except the three survivors", () => {
    expect(css).toContain('html[data-quality="low"] [data-glass-static="true"]');
    expect(css).toContain('html[data-quality="low"] .faq-item');
    expect(css).toContain('html[data-quality="low"] .page-nav .gnav');
    expect(css).toContain('html[data-quality="low"] .download-card');
    expect(css).toContain('html[data-quality="low"] .hero .cta--ghost');
  });

  it("pauses CSS animations while the document is hidden", () => {
    expect(css).toContain("html[data-doc-hidden] *::after");
    expect(css).toMatch(/html\[data-doc-hidden\][\s\S]*?animation-play-state: paused !important/);
  });

  it("keeps the CTA shimmer paused at rest", () => {
    const sweep = css.slice(css.indexOf(".cta-shimmer-sweep {"));
    expect(sweep).toContain("animation-play-state: paused");
  });

  it("no longer ships idle will-change hints or a blurred hero glow", () => {
    expect(rules.includes("will-change")).toBe(false);
    expect(rules.includes("filter: blur(48px)")).toBe(false);
  });

  it("self-hosts the two Latin font faces (no @fontsource import)", () => {
    expect(rules.includes("@fontsource-variable")).toBe(false);
    expect(rules).toContain("/fonts/inter-latin-var.woff2");
    expect(rules).toContain("/fonts/inter-tight-latin-var.woff2");
  });
});
