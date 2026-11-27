/**
 * Download configuration — the ONLY place the APK link rules live.
 *
 * The link is no longer hand-edited: the site reads `releases/latest.json`
 * (the same manifest the in-app updater consumes, staged into the build by
 * the deploy workflow) and converts it with `downloadConfigFromManifest`.
 * `APP_DOWNLOAD` stays as the "nothing published" fallback; while it is blank
 * the card stays DISABLED — no fake downloads, ever.
 *
 * Never put secrets in this file: it ships to the website.
 */
import { fileNameFromUrl, type ReleaseManifest } from "./release";

export const APP_DOWNLOAD = {
  url: "",
  version: "v1.0.0",
  fileName: "sippy-v1.0.0.apk",
  sizeBytes: 0,
  sha256: "",
} as const;

/**
 * Single source of truth for the APK CTA label across nav and hero:
 * "See install steps" while no release is configured, "Get the APK" once one
 * is. The nav/hero CTA scrolls to the download card, which resolves the live
 * release itself.
 */
export const APK_CTA_LABELS = {
  pending: "See install steps",
  ready: "Get the APK",
} as const;

export interface DownloadConfig {
  url: string;
  version: string;
  fileName: string;
  sizeBytes: number;
  sha256: string;
}

export interface DownloadState {
  enabled: boolean;
  url: string;
  version: string;
  fileName: string;
  sizeLabel: string;
  /** First 12 chars for compact display; full hash shown in the card. */
  sha256Short: string;
  sha256: string;
}

export function getDownloadState(cfg: DownloadConfig = APP_DOWNLOAD): DownloadState {
  const enabled =
    cfg.url.trim() !== "" && cfg.sizeBytes > 0 && cfg.sha256.trim() !== "";
  return {
    enabled,
    url: cfg.url,
    version: cfg.version,
    fileName: cfg.fileName,
    sizeLabel: formatBytes(cfg.sizeBytes),
    sha256Short: cfg.sha256.slice(0, 12),
    sha256: cfg.sha256,
  };
}

export function getApkCtaLabel(cfg: DownloadConfig = APP_DOWNLOAD): string {
  return getDownloadState(cfg).enabled
    ? APK_CTA_LABELS.ready
    : APK_CTA_LABELS.pending;
}

export const APK_CTA_LABEL = getApkCtaLabel();

/**
 * A published release (from releases/latest.json) → the download config the
 * card renders. Kept here so the gating rules have exactly one implementation.
 */
export function downloadConfigFromManifest(m: ReleaseManifest): DownloadConfig {
  return {
    url: m.apkUrl,
    version: `v${m.versionName.replace(/^v/i, "")}`,
    fileName: fileNameFromUrl(m.apkUrl),
    sizeBytes: m.sizeBytes,
    sha256: m.sha256,
  };
}

export function formatBytes(n: number): string {
  if (!n || n <= 0) return "—";
  const units = ["B", "KB", "MB", "GB"];
  let v = n;
  let i = 0;
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024;
    i += 1;
  }
  const digits = v >= 100 || i === 0 ? 0 : 1;
  const num = v.toFixed(digits).replace(/\.0$/, "");
  return `${num} ${units[i]}`;
}
