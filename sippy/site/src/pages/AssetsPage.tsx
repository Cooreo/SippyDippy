import { asset } from "../lib/router";
import { ASSETS, PENDING_ASSETS } from "../lib/assets";

/**
 * Hidden /#/assets contact sheet (Phase 3 richness pass) — every generated
 * asset with its real dimensions, byte size, alpha status and usage, so the
 * pack can be reviewed in one place.
 */
const USAGE: Record<string, string> = {
  "hero-caustics": "Backdrop layer — hero region (fixed, behind glass)",
  "dippy-happy": "Hero (floating by the phone) · Friends bento card",
  "dippy-dancing": "Streaks & reminders bento card (goal reached)",
  "dippy-sleepy": "Your goal bento card (thirsty, needs a sip)",
  "dippy-waving": "Download card head (greeting you)",
  "friend-plant-sprout": "Showcase friends art · pet/plant growth stage 1",
  "friend-plant-bloom": "Your look bento card · plant growth stage 2",
  "friend-pet-cub": "Developer options bento card · pet growth stage 1",
  "friend-pet-adult": "Pet growth stage 2 (contact sheet review)",
  "droplet-sprite": "Ambient droplets (≤12 on the page) · Drinks bento card",
  "ripple-ring": "Footer ambient ripple · intro impact texture",
  "bg-features": "Features section art (feathered 200px+ edges)",
  "bg-showcase": "Showcase section art (feathered 200px+ edges)",
  "bg-download": "Download section art (feathered 200px+ edges)",
};

function kb(bytes: number): string {
  return `${(bytes / 1024).toFixed(1)} KB`;
}

export function AssetsPage() {
  const total = ASSETS.reduce((s, a) => s + a.bytes, 0);

  return (
    <div className="page">
      <div className="wrap" style={{ padding: "120px 0 80px" }}>
        <h1 style={{ fontSize: "clamp(28px, 4vw, 44px)", letterSpacing: "-0.02em" }}>
          Asset pack — contact sheet
        </h1>
        <p style={{ color: "var(--text-3)", maxWidth: "70ch" }}>
          {ASSETS.length} generated assets · {kb(total)} transferred total ·
          style: glossy translucent water-droplet, Sippy Blue #2E7CF6 with
          violet accents, soft studio lighting, subtle rim light. Mascots
          carry a keyed alpha channel (black studio background removed);
          scenery is full-bleed. Hero budget &lt; 250 KB, all others &lt; 120 KB.
        </p>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))",
            gap: 18,
            marginTop: 36,
          }}
        >
          {ASSETS.map((a) => (
            <div key={a.name} className="glass asset-card">
              <div
                className="asset-thumb"
                style={{
                  backgroundImage:
                    "repeating-conic-gradient(rgba(127,127,127,.25) 0% 25%, transparent 0% 50%) 0 0 / 16px 16px",
                }}
              >
                <img
                  src={asset(a.file)}
                  alt={
                    a.alpha
                      ? `${a.name} — transparent mascot artwork`
                      : `${a.name} — background artwork`
                  }
                  width={Math.min(a.width, 400)}
                  height={Math.round(
                    Math.min(a.width, 400) * (a.height / a.width),
                  )}
                  loading="lazy"
                  decoding="async"
                />
              </div>
              <div className="asset-meta">
                <b>{a.name}</b>
                <span>
                  {a.width}×{a.height} · {kb(a.bytes)} (budget {kb(a.budget)}) ·{" "}
                  {a.alpha ? `alpha, opaque ${a.opaquePct ?? "n/a"}%` : "no alpha"}
                </span>
                <span className="asset-usage">{USAGE[a.name] ?? "—"}</span>
              </div>
            </div>
          ))}
        </div>
        {PENDING_ASSETS.length > 0 && (
          <p style={{ marginTop: 16, fontSize: 13, color: "var(--text-3)" }}>
            Pending (source image not generated yet — the affected sections
            fall back to the backdrop gradients):{" "}
            <code>{PENDING_ASSETS.join(", ")}</code>
          </p>
        )}
        <p style={{ marginTop: 16, fontSize: 13, color: "var(--text-3)" }}>
          Source PNGs: <code>sippy/site/.assets-src/</code> (gitignored) ·
          rebuild: <code>python3 tooling/build-assets.py</code> · credits:{" "}
          <code>sippy/CREDITS.md</code>
        </p>
      </div>
    </div>
  );
}
