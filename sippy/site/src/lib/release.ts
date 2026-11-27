/**
 * The live release channel for the website.
 *
 * `releases/latest.json` in the repo root is written by the Android release
 * workflow and is the exact file the app's updater reads. The deploy workflow
 * copies it into the built site, so the download button is driven by the same
 * data as the updater — no hand-edited hash, no stale link.
 *
 * Everything here is defensive: a missing file, a 404, a placeholder manifest
 * or a wrong-host URL all resolve to "no build published", and the download
 * card stays disabled instead of promising a file that is not there.
 */
import { useEffect, useState } from "react";
import { asset } from "./router";

export interface ReleaseManifest {
  versionCode: number;
  versionName: string;
  apkUrl: string;
  sha256: string;
  sizeBytes: number;
  minSdk: number;
  releasedAt: string;
  notes: string[];
}

/** Where the deploy workflow stages the manifest inside the built site. */
export const RELEASE_MANIFEST_PATH = "releases/latest.json";

/**
 * Fallback source: the same file straight from `main`. Used when the staged
 * copy is missing (a deploy that ran before the first release, or a runtime
 * where `public/releases/` was not staged). Served with CORS `*`, so the
 * site stays dependency-free and the download link never goes stale just
 * because Pages has not redeployed yet.
 */
export const RELEASE_MANIFEST_RAW_URL =
  "https://raw.githubusercontent.com/Cooreo/SippyDippy/main/releases/latest.json";

/** The only host and path the site will ever link an APK from. */
export const RELEASE_URL_PREFIX =
  "https://github.com/Cooreo/SippyDippy/releases/download/";

export const MANIFEST_TIMEOUT_MS = 5000;

const SHA256_RE = /^[a-f0-9]{64}$/i;

function asInt(value: unknown): number {
  const n = typeof value === "string" ? Number(value.trim()) : Number(value);
  return Number.isFinite(n) && Number.isInteger(n) ? n : 0;
}

/**
 * Accepts only a manifest that points at a real, verifiable APK:
 * versionCode > 0, an https URL under the releases prefix, a 64-char hex
 * SHA-256 and a positive size. Anything else → null.
 */
export function parseReleaseManifest(raw: unknown): ReleaseManifest | null {
  if (typeof raw !== "object" || raw === null || Array.isArray(raw)) return null;
  const r = raw as Record<string, unknown>;

  const versionCode = asInt(r.versionCode);
  const sizeBytes = asInt(r.sizeBytes);
  const versionName = typeof r.versionName === "string" ? r.versionName.trim() : "";
  const apkUrl = typeof r.apkUrl === "string" ? r.apkUrl.trim() : "";
  const sha256 = typeof r.sha256 === "string" ? r.sha256.trim().toLowerCase() : "";

  if (versionCode <= 0) return null;
  if (versionName === "") return null;
  if (sizeBytes <= 0) return null;
  if (!SHA256_RE.test(sha256)) return null;
  if (!apkUrl.toLowerCase().startsWith(RELEASE_URL_PREFIX.toLowerCase())) return null;
  if (/\s/.test(apkUrl)) return null;

  return {
    versionCode,
    versionName,
    apkUrl,
    sha256,
    sizeBytes,
    minSdk: asInt(r.minSdk) || 26,
    releasedAt: typeof r.releasedAt === "string" ? r.releasedAt : "",
    notes: Array.isArray(r.notes)
      ? r.notes.filter((n): n is string => typeof n === "string")
      : [],
  };
}

/** `.../sippy-1.0.0.apk` → `sippy-1.0.0.apk`; anything odd falls back. */
export function fileNameFromUrl(url: string): string {
  const clean = url.split(/[?#]/)[0] ?? "";
  const last = clean.slice(clean.lastIndexOf("/") + 1);
  return last.toLowerCase().endsWith(".apk") ? last : "sippy.apk";
}

export type ReleaseStatus = "loading" | "ready" | "none";

export interface ReleaseResult {
  status: ReleaseStatus;
  manifest: ReleaseManifest | null;
}

let cache: ReleaseResult | null = null;

/** Test seam: forget the cached lookup. */
export function resetReleaseCache(): void {
  cache = null;
}

/** Same-origin staged copy first, then the raw file on `main`. */
export function releaseManifestUrls(): string[] {
  return [asset(RELEASE_MANIFEST_PATH), RELEASE_MANIFEST_RAW_URL];
}

export async function fetchReleaseManifest(
  fetchImpl?: typeof fetch,
  signal?: AbortSignal,
): Promise<ReleaseManifest | null> {
  const doFetch = fetchImpl ?? (typeof fetch === "function" ? fetch : undefined);
  if (!doFetch) return null;
  for (const url of releaseManifestUrls()) {
    try {
      const res = await doFetch(url, { signal, cache: "no-cache" });
      if (!res.ok) continue;
      const manifest = parseReleaseManifest(await res.json());
      if (manifest) return manifest;
    } catch {
      // Try the next source; a blocked or offline fetch is not an error here.
    }
  }
  return null;
}

/**
 * One fetch per page load, shared by every caller. Reports `loading` first so
 * the card can say "checking" instead of flashing a wrong state.
 */
export function useRelease(): ReleaseResult {
  const [result, setResult] = useState<ReleaseResult>(
    () => cache ?? { status: "loading", manifest: null },
  );

  useEffect(() => {
    if (cache) {
      setResult(cache);
      return;
    }
    const controller =
      typeof AbortController === "function" ? new AbortController() : undefined;
    const timer = setTimeout(() => controller?.abort(), MANIFEST_TIMEOUT_MS);
    let alive = true;
    fetchReleaseManifest(undefined, controller?.signal).then((manifest) => {
      if (!alive) return;
      cache = { status: manifest ? "ready" : "none", manifest };
      setResult(cache);
    });
    return () => {
      alive = false;
      clearTimeout(timer);
      controller?.abort();
    };
  }, []);

  return result;
}
