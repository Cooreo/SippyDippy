import { useState, type CSSProperties } from "react";
import { asset } from "../lib/router";
import { LiquidProgress, LiquidSlider } from "fluidkit";
import { GlassCard as GlinCard } from "@glinui/ui";
import { GlassSurface } from "../components/glass/GlassSurface";
import { GlassCard } from "../components/glass/GlassCard";
import { GlassButton } from "../components/glass/GlassButton";
import { GlassIconButton } from "../components/glass/GlassIconButton";
import { GlassChip } from "../components/glass/GlassChip";
import { GlassToggle } from "../components/glass/GlassToggle";
import { GlassTextField } from "../components/glass/GlassTextField";
import { GlassTabBar } from "../components/glass/GlassTabBar";
import { GlassNav } from "../components/glass/GlassNav";
import {
  GlassToastProvider,
  showGlassToast,
} from "../components/glass/GlassToast";
import {
  IconDroplet,
  IconFlame,
  IconGear,
  IconHeart,
  IconHome,
  IconMusic,
  IconNext,
  IconPause,
  IconPlay,
  IconPlus,
  IconPrev,
  IconSparkle,
  IconUsers,
} from "../components/Icons";
import { useTheme } from "../lib/theme";
import { useEffects } from "../lib/effects";
import { setFxOverride, useFxMode } from "../lib/fx";
import { navigate } from "../lib/router";

type BackdropKind = "aurora" | "meadow" | "blobs" | "flat" | "busy";

const BACKDROPS: { id: BackdropKind; label: string }[] = [
  { id: "aurora", label: "Aurora" },
  { id: "meadow", label: "Meadow" },
  { id: "blobs", label: "Blobs" },
  { id: "flat", label: "Flat (trap)" },
  { id: "busy", label: "Busy" },
];

const AURORA_CSS: CSSProperties = {
  background:
    "radial-gradient(60% 50% at 30% 30%, rgba(46,124,246,0.4), transparent 70%), radial-gradient(50% 45% at 75% 65%, rgba(139,92,246,0.35), transparent 70%), radial-gradient(40% 40% at 55% 20%, rgba(20,184,166,0.25), transparent 70%), var(--bg-0)",
};

function Stage({ kind }: { kind: BackdropKind }) {
  if (kind === "aurora") return null; // global AuroraBackdrop shows through
  const style: CSSProperties | undefined =
    kind === "meadow"
      ? { backgroundImage: `url(${asset("backdrops/landscape-dusk-emerald.jpg")})` }
      : kind === "blobs"
        ? { backgroundImage: `url(${asset("backdrops/blobs-dark.jpg")})` }
        : kind === "flat"
          ? { background: "var(--bg-0)" }
          : undefined;
  return (
    <div
      className={`stage${kind === "busy" ? " stage-busy" : ""}`}
      style={style}
      aria-hidden="true"
    />
  );
}

function MatrixCell({
  theme,
  bg,
  bgStyle,
}: {
  theme: "light" | "dark";
  bg: string;
  bgStyle: CSSProperties;
}) {
  return (
    <div className={`matrix-frame${theme === "dark" ? " dark" : ""}`}>
      <div className="matrix-bg" style={bgStyle} aria-hidden="true" />
      <span className="matrix-tag">
        {theme} · {bg}
      </span>
      <GlassCard radius="inner" blur="sm" style={{ padding: 16 }}>
        <b style={{ fontFamily: "var(--ff-display)", letterSpacing: "-0.02em" }}>
          GlassCard
        </b>
        <p
          style={{
            fontSize: 13,
            color: "var(--text-2)",
            margin: "6px 0 12px",
          }}
        >
          blur 24 · sat 165% · rim + grain
        </p>
        <GlassButton size="sm">Get Sippy</GlassButton>
      </GlassCard>
      <GlassTabBar
        defaultValue="a"
        items={[
          { id: "a", label: "Home" },
          { id: "b", label: "Drinks" },
          { id: "c", label: "Friends" },
        ]}
      />
      <GlassToggle label="Reminders" defaultChecked />
    </div>
  );
}

export function GalleryPage() {
  const { mode, toggle } = useTheme();
  const { reduceEffects, setReduceEffects } = useEffects();
  const fx = useFxMode();
  const [backdrop, setBackdrop] = useState<BackdropKind>("aurora");
  const [tabValue, setTabValue] = useState("home");
  const [goal, setGoal] = useState(2000);
  const [playing, setPlaying] = useState(true);

  const meadowBg: CSSProperties = {
    backgroundImage: `url(${asset("backdrops/landscape-dusk-emerald.jpg")})`,
  };
  const blobsBg: CSSProperties = {
    backgroundImage: `url(${asset("backdrops/blobs-dark.jpg")})`,
  };

  return (
    <GlassToastProvider position="bottom-right">
      <Stage kind={backdrop} />
      <div className="gallery">
        <header
          className="glass controls"
          style={{ borderRadius: "999px" }}
          aria-label="Gallery controls"
        >
          <div className="control-group">
            <span className="control-label">Backdrop</span>
            {BACKDROPS.map((b) => (
              <GlassChip
                key={b.id}
                active={backdrop === b.id}
                onClick={() => setBackdrop(b.id)}
              >
                {b.label}
              </GlassChip>
            ))}
          </div>
          <div className="control-group">
            <GlassToggle
              checked={mode === "dark"}
              onCheckedChange={() => toggle()}
              label={mode === "dark" ? "Dark" : "Light"}
            />
          </div>
          <div className="control-group">
            <GlassToggle
              checked={reduceEffects}
              onCheckedChange={setReduceEffects}
              label="Reduce effects"
            />
          </div>
          <div className="control-group">
            <button
              type="button"
              className="fx-pill"
              data-fx={fx}
              onClick={() => setFxOverride(fx === "full" ? "blur" : "full")}
            >
              refraction: {fx}
              {fx === "blur" ? " (fallback)" : ""}
            </button>
            <span className="home-hint">
              or <code>?fx=blur</code> in the URL
            </span>
          </div>
          <div className="control-group">
            <button
              type="button"
              className="fx-pill"
              onClick={() => navigate("/")}
            >
              ← shell
            </button>
          </div>
        </header>

        <div className="section">
          <h2>Matrix — every key component, both themes, every backdrop</h2>
          <p className="hint">
            Fixed 2×4 grid so light and dark over each backdrop are always
            visible at once. The "Flat (trap)" cell is deliberate: glass over
            a flat background is invisible — that's the rule the brief sets
            and the aurora/backdrops exist to break.
          </p>
          <div className="matrix">
            <MatrixCell theme="light" bg="aurora" bgStyle={AURORA_CSS} />
            <MatrixCell theme="dark" bg="aurora" bgStyle={AURORA_CSS} />
            <MatrixCell theme="light" bg="meadow" bgStyle={meadowBg} />
            <MatrixCell theme="dark" bg="meadow" bgStyle={meadowBg} />
            <MatrixCell theme="light" bg="blobs" bgStyle={blobsBg} />
            <MatrixCell theme="dark" bg="blobs" bgStyle={blobsBg} />
            <MatrixCell theme="light" bg="flat" bgStyle={{ background: "var(--bg-0)" }} />
            <MatrixCell theme="dark" bg="flat" bgStyle={{ background: "var(--bg-0)" }} />
          </div>
        </div>

        <div className="section">
          <h2>GlassSurface — blur tiers &amp; radii</h2>
          <p className="hint">
            Same recipe, three blur levels (24/32/40) and the three radii
            (999/28/20). Refraction follows the pill in the controls above.
          </p>
          <div className="grid-cards">
            <GlassSurface blur="sm" radius="inner" style={{ minHeight: 110 }}>
              <b>blur 24</b>
              <div style={{ color: "var(--text-2)", fontSize: 13 }}>
                small surfaces
              </div>
            </GlassSurface>
            <GlassSurface blur="md" radius="inner" style={{ minHeight: 110 }}>
              <b>blur 32</b>
              <div style={{ color: "var(--text-2)", fontSize: 13 }}>
                cards
              </div>
            </GlassSurface>
            <GlassSurface blur="lg" radius="inner" style={{ minHeight: 110 }}>
              <b>blur 40</b>
              <div style={{ color: "var(--text-2)", fontSize: 13 }}>
                nav / sheets
              </div>
            </GlassSurface>
            <GlassSurface radius="pill" style={{ minHeight: 110 }}>
              <b>radius 999</b>
              <div style={{ color: "var(--text-2)", fontSize: 13 }}>pill</div>
            </GlassSurface>
            <GlassSurface radius="card" style={{ minHeight: 110 }}>
              <b>radius 28</b>
              <div style={{ color: "var(--text-2)", fontSize: 13 }}>card</div>
            </GlassSurface>
            <GlassSurface radius="inner" style={{ minHeight: 110 }}>
              <b>radius 20</b>
              <div style={{ color: "var(--text-2)", fontSize: 13 }}>inner</div>
            </GlassSurface>
          </div>
        </div>

        <div className="section">
          <h2>Cards</h2>
          <p className="hint">
            Left: Sippy GlassCard (our recipe, specular on hover). Right: Glin
            UI GlassCard wearing the Sippy token overrides — same look, two
            code paths, one token source.
          </p>
          <div className="row-wrap">
            <GlassCard specular style={{ width: 320 }}>
              <b style={{ fontFamily: "var(--ff-display)" }}>GlassCard (Sippy)</b>
              <p style={{ color: "var(--text-2)", fontSize: 14, margin: "8px 0 0" }}>
                28px radius · blur 32 · gradient rim · inner highlight ·
                3-layer shadow · grain.
              </p>
            </GlassCard>
            <div className="glin-compare" style={{ width: 320 }}>
              <GlinCard size="md">
                <b style={{ fontFamily: "var(--ff-display)" }}>GlassCard (Glin)</b>
                <p style={{ color: "var(--text-2)", fontSize: 14, margin: "8px 0 0" }}>
                  <code>@glinui/ui</code> component reading the Sippy token
                  overrides.
                </p>
              </GlinCard>
            </div>
          </div>
        </div>

        <div className="section">
          <h2>Buttons</h2>
          <p className="hint">
            Glin ShimmerButton, glass variant — sweep colour and duration
            tuned per the brief. Press any: scale 0.97 + spring back.
          </p>
          <div className="row-wrap">
            <GlassButton>Get Sippy</GlassButton>
            <GlassButton size="lg">Start today</GlassButton>
            <GlassButton
              shimmerColor="rgb(91 155 255 / 0.6)"
              shimmerDuration={3.6}
            >
              Slow accent sweep
            </GlassButton>
            <GlassButton disabled>Disabled</GlassButton>
            <button type="button" className="pill-cta pressable">
              White pill CTA
            </button>
          </div>
        </div>

        <div className="section">
          <h2>Icon buttons · chips · toggle · field</h2>
          <p className="hint">
            One rounded stroke set, 1.75px, 48px minimum targets. Capsule
            field per reference 2.
          </p>
          <div className="row-wrap" style={{ marginBottom: 16 }}>
            <GlassIconButton label="Add drink">
              <IconDroplet />
            </GlassIconButton>
            <GlassIconButton label="Streaks">
              <IconFlame />
            </GlassIconButton>
            <GlassIconButton label="Friends">
              <IconUsers />
            </GlassIconButton>
            <GlassIconButton label="Favourite">
              <IconHeart />
            </GlassIconButton>
            <GlassIconButton label="New">
              <IconPlus />
            </GlassIconButton>
            <GlassIconButton label="Settings" size="lg">
              <IconGear />
            </GlassIconButton>
          </div>
          <div className="row-wrap" style={{ marginBottom: 16 }}>
            <GlassChip>neutral</GlassChip>
            <GlassChip tone="success">success</GlassChip>
            <GlassChip tone="warning">warning</GlassChip>
            <GlassChip tone="danger">danger</GlassChip>
            <GlassChip active>active (info)</GlassChip>
          </div>
          <div className="row-wrap" style={{ marginBottom: 16 }}>
            <GlassToggle label="Reminders" defaultChecked />
            <GlassToggle label="Sugary drink warning" checkedTint="#F5B84D" defaultChecked />
            <GlassToggle label="Disabled" disabled />
          </div>
          <GlassTextField
            label="Add a drink"
            placeholder="e.g. Iced oolong, 400 ml"
            aria-label="Add a drink"
          />
        </div>

        <div className="section">
          <h2>GlassTabBar — the liquid lens (reference 4)</h2>
          <p className="hint">
            fluidkit LiquidTabs. Top: <code>flow="stretch"</code> — the taffy
            pill that bends and stretches between tabs. Middle:{" "}
            <code>flow="slide"</code> — one droplet with inertia. Bottom:
            icon-only tabs with aria-labels.
          </p>
          <div style={{ display: "flex", flexDirection: "column", gap: 18, maxWidth: 520 }}>
            <GlassTabBar
              value={tabValue}
              onChange={setTabValue}
              items={[
                { id: "home", label: "Home" },
                { id: "drinks", label: "Drinks" },
                { id: "friends", label: "Friends" },
              ]}
            />
            <GlassTabBar
              flow="slide"
              defaultValue="now"
              items={[
                { id: "now", label: "Now" },
                { id: "today", label: "Today" },
                { id: "week", label: "Week" },
              ]}
            />
            <GlassTabBar
              defaultValue="home"
              items={[
                { id: "home", ariaLabel: "Home", icon: <IconHome width={18} height={18} /> },
                { id: "drinks", ariaLabel: "Drinks", icon: <IconDroplet width={18} height={18} /> },
                { id: "friends", ariaLabel: "Friends", icon: <IconUsers width={18} height={18} /> },
                { id: "streaks", ariaLabel: "Streaks", icon: <IconFlame width={18} height={18} /> },
              ]}
            />
          </div>
        </div>

        <div className="section">
          <h2>Hydration controls — progress &amp; slider</h2>
          <p className="hint">
            The daily-minimum UI the app uses (liquid vessel + droplet thumb
            on the meniscus edge).
          </p>
          <GlassCard style={{ maxWidth: 520 }}>
            <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
              <LiquidProgress
                value={1240}
                max={goal}
                width={460}
                height={14}
                fillTint="#2E7CF6"
                intensity="present"
                aria-label="Daily hydration progress"
              />
              <div style={{ color: "var(--text-2)", fontSize: 13 }}>
                1240 ml of {goal} ml
              </div>
              <LiquidSlider
                value={goal}
                onValueChange={(v) => setGoal(v)}
                min={500}
                max={3000}
                step={50}
                width={460}
                label="Daily minimum (ml)"
                fillTint="#2E7CF6"
                intensity="present"
              />
            </div>
          </GlassCard>
        </div>

        <div className="section">
          <h2>Glass toast</h2>
          <p className="hint">
            fluidkit toast — condenses in at the corner, evaporates on dismiss.
          </p>
          <GlassButton onClick={() => showGlassToast("Glass toast — condenses in, evaporates out.")}>
            <IconSparkle width={18} height={18} style={{ verticalAlign: "-3px", marginRight: 8 }} />
            Show a glass toast
          </GlassButton>
        </div>

        <div
          className="photo-section"
          style={meadowBg}
          aria-label="Floating nav over photo demo"
        >
          <GlassNav
            links={["Features", "Showcase", "Download"]}
            cta={
              <button type="button" className="pill-cta pressable">
                Get Sippy
              </button>
            }
          />
          <GlassCard style={{ maxWidth: 520 }}>
            <b style={{ fontFamily: "var(--ff-display)" }}>
              Reference 3 — floating nav over a photo
            </b>
            <p style={{ color: "var(--text-2)", fontSize: 14, margin: "8px 0 0" }}>
              The landscape's edge stays visible through the bar (blur +
              saturation, not a solid fill). Sun/moon toggle and white pill
              CTA are live — try them.
            </p>
          </GlassCard>
        </div>

        <div className="section" style={{ position: "relative" }}>
          <h2>Reference 5 — dark glass app UI (music-style)</h2>
          <p className="hint">
            The style the app showcase will use: glass cards, big artwork,
            pill transport controls, glowing active line, glass tab bar.
            Force-wrapped in dark tokens so the dark look shows in both
            themes.
          </p>
          <div className="dark" style={{ borderRadius: "var(--r-card)" }}>
            <GlassCard
              blur="lg"
              specular
              style={{ padding: 28, maxWidth: 640, background: "rgba(8,12,24,0.42)" }}
            >
              <div className="row-wrap" style={{ alignItems: "flex-start", gap: 20 }}>
                <div className="music-art">
                  <img
                    src={asset("backdrops/blobs-dark.jpg")}
                    alt="Album artwork — abstract blue and violet blobs"
                  />
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 8, color: "var(--text-2)" }}>
                    <IconMusic width={16} height={16} />
                    <span style={{ fontSize: 13 }}>Sippy Sound</span>
                  </div>
                  <h3 style={{ margin: "4px 0 14px", fontSize: 22 }}>
                    Midnight Droplet
                  </h3>
                  <div className="transport">
                    <GlassIconButton label="Previous track">
                      <IconPrev />
                    </GlassIconButton>
                    <GlassIconButton
                      label={playing ? "Pause" : "Play"}
                      size="lg"
                      onClick={() => setPlaying((p) => !p)}
                    >
                      {playing ? <IconPause /> : <IconPlay />}
                    </GlassIconButton>
                    <GlassIconButton label="Next track">
                      <IconNext />
                    </GlassIconButton>
                  </div>
                </div>
              </div>
              <div style={{ marginTop: 22, display: "flex", flexDirection: "column", gap: 8 }}>
                <div className="lyric">The water remembers every sip you skip —</div>
                <div className="lyric lyric-active">so drink, and let the lens bend the light,</div>
                <div className="lyric">streak by streak the whole world's lit.</div>
              </div>
              <div style={{ marginTop: 22 }}>
                <GlassTabBar
                  size="md"
                  defaultValue="now"
                  items={[
                    { id: "now", label: "Now playing" },
                    { id: "queue", label: "Queue" },
                    { id: "library", label: "Library" },
                  ]}
                />
              </div>
            </GlassCard>
          </div>
        </div>

        <footer className="home-foot">
          <p>
            Honesty notes: refraction (edge bending) is a Chromium-only
            progressive enhancement — force the fallback with the pill above
            or <code>?fx=blur</code>. "Reduce effects" emulates{" "}
            <code>prefers-reduced-motion</code> +{" "}
            <code>prefers-reduced-transparency</code> (and forces the
            blur-only refraction path); the real OS settings are always
            respected on their own.
          </p>
          <p>
            Phase 3 adds the full page: cinematic intro, hero, feature bento,
            scroll-pinned app showcase, how-it-works, download card, FAQ,
            footer — and the real nav behaviour (scroll compression +
            active-section lens).
          </p>
        </footer>
      </div>
    </GlassToastProvider>
  );
}
