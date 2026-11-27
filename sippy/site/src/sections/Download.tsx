import { GlassCard } from "../components/glass/GlassCard";
import { downloadConfigFromManifest, getDownloadState } from "../lib/download";
import { useRelease } from "../lib/release";
import { asset } from "../lib/router";

/**
 * Download (Phase 3 fix D) — one left edge: head, button, note and steps
 * all align to the card's single padding edge. Steps are an <ol> with
 * numbered glass chips 1–4. Card and FAQ share the same centered 760px
 * container.
 *
 * The button is driven by `releases/latest.json` (see lib/release.ts), so it
 * goes live with the first signed release and always matches the APK the
 * updater will offer. Remaining honest states: "checking", and "nothing
 * published yet" — the card never promises a file that is not there.
 */
const STEPS = [
  <><b>Allow</b> installs from this source (Settings &gt; Security &gt; Install unknown apps)</>,
  <><b>Open</b> the downloaded file from your downloads</>,
  <><b>Confirm</b> “Install Sippy”</>,
  <><b>Open</b> Sippy — Dippy is already waiting</>,
];

export function Download() {
  const release = useRelease();
  const d = getDownloadState(
    release.manifest ? downloadConfigFromManifest(release.manifest) : undefined,
  );
  const checking = release.status === "loading";

  return (
    <section id="download" className="download">
      <div className="narrow">
        <GlassCard id="download-card" blur="lg" lowTierBlur className="download-card" specular>
          <div className="download-head">
            <img
              src={asset("assets/dippy-waving.webp")}
              srcSet={`${asset("assets/dippy-waving-128.webp")} 128w, ${asset("assets/dippy-waving.webp")} 227w`}
              sizes="72px"
              alt="Dippy the droplet friend, waving hello"
              width={72}
              height={78}
              loading="lazy"
              decoding="async"
            />
            <div className="download-head-text">
              {/* focus target for CTA landing (preventScroll) */}
              <h2 id="download-heading" tabIndex={-1}>
                Get Sippy for Android
              </h2>
              <p className="download-meta">
                {d.enabled ? `${d.version} · ${d.sizeLabel} · ` : ""}
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
            <div className="download-disabled" role="status" aria-live="polite">
              <button type="button" className="pill-cta" disabled>
                Download APK
              </button>
              <span>
                {checking
                  ? "Checking for the latest signed build…"
                  : "No signed build is published yet. This button activates the moment the first release lands — the link, size and SHA-256 come straight from the release manifest."}
              </span>
            </div>
          )}

          <ol className="install-steps">
            {STEPS.map((s, i) => (
              <li key={i}>
                <span className="step-num" aria-hidden="true">
                  {i + 1}
                </span>
                <span>{s}</span>
              </li>
            ))}
          </ol>
          <p className="download-note">
            Sideloaded APK — no Play Store. Your data never leaves your phone.
          </p>
        </GlassCard>
      </div>
    </section>
  );
}
