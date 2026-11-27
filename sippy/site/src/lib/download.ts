/**
 * Download configuration — the ONLY place the APK link lives.
 *
 * Phase 6: set `url` to the GitHub Release asset URL (e.g.
 * https://github.com/<user>/<repo>/releases/download/v1.0.0/sippy-v1.0.0.apk),
 * plus the real `sizeBytes` and `sha256` printed by tooling/build-release.*
 * Until all three are real, the download card stays DISABLED (honest, per
 * the brief — no fake downloads, ever).
 *
 * Never put secrets in this file: it ships to the website.
 */
export const APP_DOWNLOAD = {
  url: "",
  version: "v1.0.0",
  fileName: "sippy-v1.0.0.apk",
  sizeBytes: 0,
  sha256: "",
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
