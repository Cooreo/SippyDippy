import { describe, expect, it } from "vitest";
import { getIntroMode, introForcedFromQuery } from "./intro";

describe("introForcedFromQuery", () => {
  it("?intro=1 forces play, ?intro=0 forces skip", () => {
    expect(introForcedFromQuery("?intro=1")).toBe("play");
    expect(introForcedFromQuery("?intro=0")).toBe("skip");
    expect(introForcedFromQuery("?fx=blur&intro=1")).toBe("play");
  });
  it("absent/unknown values force nothing", () => {
    expect(introForcedFromQuery("")).toBeNull();
    expect(introForcedFromQuery("?intro=maybe")).toBeNull();
  });
});

describe("getIntroMode precedence", () => {
  it("?intro=1 plays even when the session already saw it", () => {
    expect(
      getIntroMode({
        search: "?intro=1",
        reducedMotion: false,
        sessionHasPlayed: true,
      }),
    ).toBe("play");
  });
  it("?intro=0 skips even on a fresh session", () => {
    expect(
      getIntroMode({ search: "?intro=0", reducedMotion: false, sessionHasPlayed: false }),
    ).toBe("skip");
  });
  it("?intro=1 wins over reduced motion (it is a test flag)", () => {
    expect(
      getIntroMode({ search: "?intro=1", reducedMotion: true, sessionHasPlayed: false }),
    ).toBe("play");
  });
  it("reduced motion → fade (when not forced)", () => {
    expect(
      getIntroMode({ search: "", reducedMotion: true, sessionHasPlayed: false }),
    ).toBe("fade");
  });
  it("fresh session plays; seen-this-session skips", () => {
    expect(
      getIntroMode({ search: "", reducedMotion: false, sessionHasPlayed: false }),
    ).toBe("play");
    expect(
      getIntroMode({ search: "", reducedMotion: false, sessionHasPlayed: true }),
    ).toBe("skip");
  });
});
