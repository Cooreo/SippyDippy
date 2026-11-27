/**
 * PhoneMock — the website's "app screenshots". Built from the REAL Sippy
 * glass components (GlassTabBar, GlassChip, Dippy) — no images, per brief.
 * Always rendered in dark tokens (the hero look), whatever the page theme.
 *
 * Root-cause fix B: the frame is a true 9:19.5 box (container-type: size in
 * CSS); every inner size is a container unit, the goal is a cqw-sized SVG
 * ring (no fixed-px LiquidProgress that could outgrow its card), and the
 * tab bar is flex-pinned to the bottom — the screen is always fully filled
 * and nothing can overflow or clip at any rendered width.
 */
import { GlassTabBar } from "./glass/GlassTabBar";
import { GlassChip } from "./glass/GlassChip";
import { Dippy } from "./Dippy";
import { IconDroplet, IconFlame, IconPlus } from "./Icons";

export type PhoneMockVariant = "home" | "add" | "friends";

/** 62% of a 45-radius circle (2πr ≈ 282.7) */
const RING_C = 282.74;
const RING_PCT = 0.62;

function WaterRing() {
  return (
    <div className="pm-ring">
      <svg viewBox="0 0 100 100" aria-hidden="true">
        <circle className="pm-ring-track" cx="50" cy="50" r="45" />
        <circle
          className="pm-ring-fill"
          cx="50"
          cy="50"
          r="45"
          strokeDasharray={`${RING_C * RING_PCT} ${RING_C}`}
        />
      </svg>
      <div className="pm-ring-center">
        <span className="pm-ring-ml">1240</span>
        <span className="pm-ring-sub">of 2000 ml</span>
      </div>
    </div>
  );
}

const TABS = [
  { id: "home", label: "Home" },
  { id: "drinks", label: "Drinks" },
  { id: "friends", label: "Friends" },
];

function HomeScreen() {
  return (
    <div className="pm-screen-inner">
      <div className="pm-card">
        <div className="pm-head">
          <span>Good morning</span>
          <span className="pm-streak">
            <IconFlame width={13} height={13} /> 12
          </span>
        </div>
        <WaterRing />
      </div>
      <div className="pm-card pm-buddy">
        <Dippy mood="happy" size={30} />
        <span className="pm-bubble">One more sip and I’m golden.</span>
      </div>
      <div className="pm-chips">
        <GlassChip size="sm" tone="neutral">
          Water
        </GlassChip>
        <GlassChip size="sm" tone="neutral">
          Oolong
        </GlassChip>
        <GlassChip size="sm" tone="neutral">
          Latte
        </GlassChip>
      </div>
      <div className="pm-tabbar">
        <GlassTabBar size="sm" defaultValue="home" items={TABS} />
      </div>
    </div>
  );
}

const ADD_ROWS: { name: string; ml: string }[] = [
  { name: "Iced oolong", ml: "400 ml" },
  { name: "Water", ml: "250 ml" },
  { name: "Oat latte", ml: "240 ml" },
  { name: "Kombucha", ml: "330 ml" },
];

function AddScreen() {
  return (
    <div className="pm-screen-inner">
      <div className="pm-title">Add a drink</div>
      <div className="pm-search">
        <IconDroplet width={13} height={13} />
        <span>Water, teas, coffees, milks…</span>
      </div>
      {ADD_ROWS.map((r) => (
        <div className="pm-row" key={r.name}>
          <span className="pm-row-name">{r.name}</span>
          <span className="pm-row-ml">{r.ml}</span>
          <GlassChip size="sm" tone="success">
            Add
          </GlassChip>
        </div>
      ))}
      <div className="pm-tabbar">
        <GlassTabBar size="sm" value="drinks" items={TABS} />
      </div>
    </div>
  );
}

function FriendsScreen() {
  return (
    <div className="pm-screen-inner">
      <div className="pm-title">Your friends</div>
      <div className="pm-card pm-card-friend">
        <Dippy mood="happy" size={40} />
        <div>
          <b>Dippy</b>
          <span className="pm-row-ml">droplet · streak 12 days</span>
        </div>
      </div>
      <div className="pm-card pm-card-friend">
        <Dippy mood="idle" size={40} />
        <div>
          <b>Fern</b>
          <span className="pm-row-ml">plant · watered today</span>
        </div>
      </div>
      <div className="pm-card pm-card-addfriend">
        <IconPlus width={16} height={16} />
        <span>Add a friend — pet or plant</span>
      </div>
      <div className="pm-tabbar">
        <GlassTabBar size="sm" value="friends" items={TABS} />
      </div>
    </div>
  );
}

export function PhoneMock({
  variant,
  className,
}: {
  variant: PhoneMockVariant;
  className?: string;
}) {
  return (
    <div
      className={`phone${className ? ` ${className}` : ""}`}
      aria-label={`Sippy app screen: ${variant}`}
      role="img"
    >
      <div className="phone-notch" />
      <div className="pm-status">9:41</div>
      <div className="phone-screen dark">
        {variant === "home" && <HomeScreen />}
        {variant === "add" && <AddScreen />}
        {variant === "friends" && <FriendsScreen />}
      </div>
    </div>
  );
}
