import { afterEach, describe, expect, it, vi } from "vitest";
import {
  fxFromQuery,
  getFxMode,
  setFxOverride,
  supportsCssRefraction,
} from "./fx";

describe("fxFromQuery", () => {
  it("parses ?fx=blur and ?fx=full", () => {
    expect(fxFromQuery("?fx=blur")).toBe("blur");
    expect(fxFromQuery("?fx=full")).toBe("full");
    expect(fxFromQuery("?other=1&fx=blur")).toBe("blur");
  });
  it("ignores unknown or missing values", () => {
    expect(fxFromQuery("")).toBeNull();
    expect(fxFromQuery("?fx=nope")).toBeNull();
    expect(fxFromQuery("?fx=")).toBeNull();
  });
});

describe("getFxMode", () => {
  afterEach(() => {
    setFxOverride(null);
    vi.unstubAllGlobals();
  });

  it("lets the runtime override win (gallery 'reduce effects')", () => {
    setFxOverride("blur");
    expect(getFxMode()).toBe("blur");
    setFxOverride("full");
    expect(getFxMode()).toBe("full");
    setFxOverride(null);
  });

  it("honours ?fx= over feature detection", () => {
    vi.stubGlobal("window", {
      ...window,
      location: { ...window.location, search: "?fx=blur" },
    });
    vi.stubGlobal("CSS", { supports: () => true });
    expect(getFxMode()).toBe("blur");
  });

  it("falls back to blur when the engine lacks refraction support", () => {
    vi.stubGlobal("CSS", { supports: () => false });
    expect(supportsCssRefraction()).toBe(false);
    expect(getFxMode()).toBe("blur");
  });

  it("uses full where refraction is supported and nothing forces blur", () => {
    vi.stubGlobal("CSS", { supports: () => true });
    expect(getFxMode()).toBe("full");
  });
});
