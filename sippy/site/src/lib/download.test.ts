import { describe, expect, it } from "vitest";
import { formatBytes, getDownloadState, type DownloadConfig } from "./download";

const blank: DownloadConfig = {
  url: "",
  version: "v1.0.0",
  fileName: "sippy-v1.0.0.apk",
  sizeBytes: 0,
  sha256: "",
};

describe("getDownloadState — honest gating", () => {
  it("is disabled until url, size AND sha256 are all real", () => {
    expect(getDownloadState(blank).enabled).toBe(false);
    expect(
      getDownloadState({ ...blank, url: "https://example.com/a.apk" }).enabled,
    ).toBe(false);
    expect(
      getDownloadState({ ...blank, url: "https://example.com/a.apk", sizeBytes: 1024 }).enabled,
    ).toBe(false);
    expect(
      getDownloadState({
        ...blank,
        url: "https://example.com/a.apk",
        sizeBytes: 1024,
        sha256: "abc123",
      }).enabled,
    ).toBe(true);
  });
  it("reports size label and short sha", () => {
    const s = getDownloadState({
      ...blank,
      url: "https://example.com/a.apk",
      sizeBytes: 11_534_336,
      sha256: "0123456789abcdef",
    });
    expect(s.sizeLabel).toBe("11 MB");
    expect(s.sha256Short).toBe("0123456789ab");
  });
});

describe("formatBytes", () => {
  it("formats common APK sizes", () => {
    expect(formatBytes(0)).toBe("—");
    expect(formatBytes(512)).toBe("512 B");
    expect(formatBytes(1024)).toBe("1 KB");
    expect(formatBytes(1_572_864)).toBe("1.5 MB");
    expect(formatBytes(11_534_336)).toBe("11 MB");
    expect(formatBytes(5_368_709_120)).toBe("5 GB");
  });
});
