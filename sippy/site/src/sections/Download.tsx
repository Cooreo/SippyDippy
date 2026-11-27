import { Dippy } from "../components/Dippy";
import { GlassCard } from "../components/glass/GlassCard";
import { getDownloadState } from "../lib/download";

/**
 * Download — replaces store badges (brief). Glass card with the APK button,
 * version, file size and SHA-256. **Honest gating:** the button stays
 * disabled with a visible note until `src/lib/download.ts` holds a real URL,
 * size and checksum (set in Phase 6 from the real build). No fake downloads.
 */
export function Download() {
  const d = getDownloadState();

  return (
    <section id="download" className="download">
      <GlassCard blur="lg" className="download-card" specular>
        <div className="download-head">
          <Dippy mood={d.enabled ? "happy" : "idle"} size={64} />
          <div>
            <h2>Get Sippy for Android</h2>
            <p className="download-meta">
              {d.version} · {d.enabled ? d.sizeLabel : "size appears with the build"} ·
              Android 8.0+ (looks best on 13+)
            </p>
            {d.enabled && (
              <p className="download-sha">
                SHA-256: <code>{d.sha256}</code>
              </p>
            )}
          </div>
        </div>

        {d.enabled ? (
          <a className="pill-cta download-btn" href={d.url} download>
            {`Download ${d.fileName} (${d.sizeLabel})`}
          </a>
        ) : (
          <div className="download-disabled" role="status">
            <button type="button" className="pill-cta" disabled>
              Download APK
            </button>
            <span>
              The APK ships after the app’s build phases — this button
              activates with the real file, size and SHA-256.
            </span>
          </div>
        )}

        <ol className="install-steps">
          <li>
            <b>Allow</b> installs from this source (Settings → Security →
            Install unknown apps)
          </li>
          <li>
            <b>Open</b> the downloaded <code>{d.fileName}</code> from your
            downloads
          </li>
          <li>
            <b>Confirm</b> “Install Sippy”
          </li>
          <li>
            <b>Open</b> Sippy — Dippy is already waiting
          </li>
        </ol>
        <p className="download-note">
          Sideloaded APK — no Play Store. Your data never leaves your phone.
        </p>
      </GlassCard>
    </section>
  );
}
