/**
 * PhoneMock — the website's "app screenshots". Built from the REAL Sippy
 * glass components (GlassSurface, LiquidProgress, GlassTabBar, GlassChip,
 * Dippy) — no images, per the Phase 3 brief. Always rendered in dark tokens
 * (the hero look), whatever the page theme is.
 */
import { LiquidProgress } from "fluidkit";
import { GlassTabBar } from "./glass/GlassTabBar";
import { GlassChip } from "./glass/GlassChip";
import { Dippy } from "./Dippy";
import { IconDroplet, IconFlame, IconPlus, IconUsers } from "./Icons";

export type PhoneMockVariant = "home" | "add" | "friends";

function HomeScreen() {
  return (
    <div className="pm-screen-inner">
      <div className="pm-card">
        <div className="pm-head">
          <span>Good morning</span>
          <span className="pm-streak">
            <IconFlame width={14} height={14} /> 12
          </span>
        </div>
        <LiquidProgress
          value={1240}
          max={2000}
          width={196}
          height={10}
          fillTint="#5B9BFF"
          intensity="present"
        />
        <div className="pm-goal">
          <b>1240</b> / 2000 ml
        </div>
      </div>
      <div className="pm-card pm-card-dippy">
        <Dippy mood="idle" size={46} />
        <span>“One more sip and I’m golden.”</span>
      </div>
      <GlassTabBar
        size="md"
        defaultValue="home"
        items={[
          { id: "home", label: "Home" },
          { id: "drinks", label: "Drinks" },
          { id: "friends", label: "Friends" },
        ]}
      />
    </div>
  );
}

const ADD_ROWS: { name: string; ml: string }[] = [
  { name: "Iced oolong", ml: "400 ml" },
  { name: "Water", ml: "250 ml" },
  { name: "Oat latte", ml: "240 ml" },
];

function AddScreen() {
  return (
    <div className="pm-screen-inner">
      <div className="pm-title">Add a drink</div>
      <div className="pm-search">
        <IconDroplet width={15} height={15} />
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
    </div>
  );
}

function FriendsScreen() {
  return (
    <div className="pm-screen-inner">
      <div className="pm-title">Your friends</div>
      <div className="pm-card pm-card-friend">
        <Dippy mood="happy" size={54} />
        <div>
          <b>Dippy</b>
          <span className="pm-row-ml">droplet · streak 12 days</span>
        </div>
      </div>
      <div className="pm-card pm-card-addfriend">
        <IconPlus width={18} height={18} />
        <span>Add a friend — pet or plant, your choice</span>
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
      <span className="sr-only" aria-hidden>
        <IconUsers width={1} height={1} />
      </span>
    </div>
  );
}
