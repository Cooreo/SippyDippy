import { describe, expect, it, vi } from "vitest";
import {
  RELEASE_MANIFEST_RAW_URL,
  RELEASE_URL_PREFIX,
  fetchReleaseManifest,
  fileNameFromUrl,
  parseReleaseManifest,
} from "./release";
import { downloadConfigFromManifest, getDownloadState } from "./download";

const SHA = "f".repeat(64);
const URL = `${RELEASE_URL_PREFIX}v1.0.0/sippy-1.0.0.apk`;

const good = {
  versionCode: 1,
  versionName: "1.0.0",
  apkUrl: URL,
  sha256: SHA,
  sizeBytes: 9_437_184,
  minSdk: 26,
  releasedAt: "2026-12-01T10:00:00Z",
  notes: ["First release"],
};

function jsonResponse(body: unknown, ok = true): Response {
  return {
    ok,
    json: async () => body,
  } as unknown as Response;
}

describe("parseReleaseManifest — only a real, verifiable APK passes", () => {
  it("accepts a full manifest from the release workflow", () => {
    const m = parseReleaseManifest(good);
    expect(m?.versionCode).toBe(1);
    expect(m?.versionName).toBe("1.0.0");
    expect(m?.sha256).toBe(SHA);
    expect(m?.sizeBytes).toBe(9_437_184);
    expect(m?.notes).toEqual(["First release"]);
  });

  it("keeps working when the manifest grows unknown fields", () => {
    const m = parseReleaseManifest({ ...good, futureField: { anything: true } });
    expect(m).not.toBeNull();
  });

  it("rejects the seeded placeholder manifest", () => {
    expect(
      parseReleaseManifest({
        versionCode: 0,
        versionName: "0.0.0",
        apkUrl: "",
        sha256: "",
        sizeBytes: 0,
      }),
    ).toBeNull();
  });

  it("rejects a manifest with any piece missing or wrong", () => {
    expect(parseReleaseManifest(null)).toBeNull();
    expect(parseReleaseManifest([])).toBeNull();
    expect(parseReleaseManifest("nope")).toBeNull();
    expect(parseReleaseManifest({ ...good, versionCode: 0 })).toBeNull();
    expect(parseReleaseManifest({ ...good, versionName: "  " })).toBeNull();
    expect(parseReleaseManifest({ ...good, sizeBytes: 0 })).toBeNull();
    expect(parseReleaseManifest({ ...good, sha256: "abc123" })).toBeNull();
    expect(parseReleaseManifest({ ...good, apkUrl: "" })).toBeNull();
  });

  it("refuses an APK hosted anywhere but our own releases", () => {
    expect(
      parseReleaseManifest({ ...good, apkUrl: "https://evil.example/sippy-1.0.0.apk" }),
    ).toBeNull();
    expect(
      parseReleaseManifest({ ...good, apkUrl: `http://github.com/Cooreo/SippyDippy/releases/download/v1/sippy.apk` }),
    ).toBeNull();
    // surrounding whitespace is trimmed (a JSON writer's newline is harmless),
    // whitespace inside the URL is not
    expect(parseReleaseManifest({ ...good, apkUrl: `${URL}\n` })).not.toBeNull();
    expect(parseReleaseManifest({ ...good, apkUrl: `${URL} extra` })).toBeNull();
    // case differences in the repo path are fine
    expect(
      parseReleaseManifest({
        ...good,
        apkUrl: `${RELEASE_URL_PREFIX.toLowerCase()}v1.0.0/sippy-1.0.0.apk`,
      }),
    ).not.toBeNull();
  });
});

describe("fileNameFromUrl", () => {
  it("uses the real asset name", () => {
    expect(fileNameFromUrl(URL)).toBe("sippy-1.0.0.apk");
    expect(fileNameFromUrl(`${URL}?download=1`)).toBe("sippy-1.0.0.apk");
  });
  it("falls back for anything that is not an APK", () => {
    expect(fileNameFromUrl("https://example.com/thing.zip")).toBe("sippy.apk");
    expect(fileNameFromUrl("")).toBe("sippy.apk");
  });
});

describe("downloadConfigFromManifest — the card goes live", () => {
  it("enables the button with the published URL, size and hash", () => {
    const state = getDownloadState(downloadConfigFromManifest(parseReleaseManifest(good)!));
    expect(state.enabled).toBe(true);
    expect(state.url).toBe(URL);
    expect(state.fileName).toBe("sippy-1.0.0.apk");
    expect(state.version).toBe("v1.0.0");
    expect(state.sizeLabel).toBe("9 MB");
    expect(state.sha256).toBe(SHA);
  });
});

describe("fetchReleaseManifest — never throws, never lies", () => {
  it("returns the parsed manifest on 200", async () => {
    const fetchImpl = vi.fn(async () => jsonResponse(good)) as unknown as typeof fetch;
    const m = await fetchReleaseManifest(fetchImpl);
    expect(m?.versionCode).toBe(1);
  });

  it("returns null on 404, on garbage and on a network error", async () => {
    const notFound = vi.fn(async () => jsonResponse({}, false)) as unknown as typeof fetch;
    expect(await fetchReleaseManifest(notFound)).toBeNull();

    const garbage = vi.fn(async () => jsonResponse({ hello: "world" })) as unknown as typeof fetch;
    expect(await fetchReleaseManifest(garbage)).toBeNull();

    const broken = vi.fn(async () => {
      throw new Error("offline");
    }) as unknown as typeof fetch;
    expect(await fetchReleaseManifest(broken)).toBeNull();

    const noJson = vi.fn(async () => ({
      ok: true,
      json: async () => {
        throw new SyntaxError("bad json");
      },
    })) as unknown as typeof fetch;
    expect(await fetchReleaseManifest(noJson)).toBeNull();
  });

  it("asks for the staged manifest with revalidation", async () => {
    const fetchImpl = vi.fn(async () => jsonResponse(good)) as unknown as typeof fetch;
    await fetchReleaseManifest(fetchImpl);
    const [url, init] = (fetchImpl as unknown as { mock: { calls: unknown[][] } }).mock.calls[0];
    expect(String(url)).toContain("releases/latest.json");
    expect((init as RequestInit).cache).toBe("no-cache");
  });

  it("falls back to the raw manifest on main when the staged copy is missing", async () => {
    const calls: string[] = [];
    const fetchImpl = vi.fn(async (input: RequestInfo | URL) => {
      calls.push(String(input));
      return String(input).startsWith("https://raw.githubusercontent.com")
        ? jsonResponse(good)
        : jsonResponse({}, false);
    }) as unknown as typeof fetch;
    const m = await fetchReleaseManifest(fetchImpl);
    expect(m?.versionCode).toBe(1);
    expect(calls).toHaveLength(2);
    expect(calls[1]).toBe(RELEASE_MANIFEST_RAW_URL);
  });

  it("gives up quietly when both sources fail", async () => {
    const fetchImpl = vi.fn(async () => jsonResponse({}, false)) as unknown as typeof fetch;
    expect(await fetchReleaseManifest(fetchImpl)).toBeNull();
  });
});
