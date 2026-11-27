/**
 * PhoneMock — the LIVE phone recreation (hero + showcase).
 *
 * Built from the real app components (GlassChip, GlassTabBar, WaterRing,
 * Dippy) inside the real device frame (PhoneDevice) — no screenshots, no
 * images of the app (brief R2-1). Sizes are container units (cqw) so
 * nothing can clip at any render size.
 *
 * Home screen layout, top to bottom: status bar → greeting + streak →
 * the big water ring (ml in the centre) → Dippy + mood bubble → quick-add
 * chips → friends card → liquid tab bar pinned to the bottom (home bar
 * inset below it).
 *
 * ~7 s demo loop (hero only): the pointer drifts to the Water chip, taps
 * it (ring ripple from the tap point), the ring fills 1,240 → 1,490 ml
 * with a counting number, Dippy's expression changes — then it resets.
 * Pauses when offscreen (IntersectionObserver); reduced motion renders a
 * static filled frame with no pointer, ripple or counting.
 */
import { useEffect, useRef, useState } from "react";
import { GlassChip } from "./glass/GlassChip";
import { GlassTabBar } from "./glass/GlassTabBar";
import {
  IconDroplet,
  IconFlame,
  IconHome,
  IconSparkle,
  IconUsers,
} from "./Icons";
import { Dippy } from "./Dippy";
import { asset } from "../lib/router";
import { PhoneDevice } from "./app/PhoneDevice";
import { WaterRing } from "./app/WaterRing";

/* demo-loop timeline (ms) */
const BASE = 1240;
const TAPPED = 1490;
const GOAL = 2000;

const T_POINT = 2000;
const T_TAP = 3400;
const T_FILL = 3600;
const T_HOLD = 5100;
const T_RESET = 6400;
const T_END = 7000;

interface Ripple {
  id: number;
  x: number;
  y: number;
}

function useReducedMotion(): boolean {
  const [reduced, setReduced] = useState(
    () => window.matchMedia("(prefers-reduced-motion: reduce)").matches,
  );
  useEffect(() => {
    const m = window.matchMedia("(prefers-reduced-motion: reduce)");
    const on = () => setReduced(m.matches);
    m.addEventListener("change", on);
    return () => m.removeEventListener("change", on);
  }, []);
  return reduced;
}

const easeOut = (x: number) => 1 - (1 - x) ** 3;

const TABS = [
  { id: "home", name: "Home", icon: <IconHome width="13" height="13" /> },
  { id: "friends", name: "Friends", icon: <IconUsers width="13" height="13" /> },
  { id: "more", name: "More", icon: <IconSparkle width="13" height="13" /> },
];

export type PhoneMockVariant = "home" | "add" | "friends" | "water";

export interface PhoneMockProps {
  /** "home" plays the demo loop (hero) · the rest are static showcase
   * variants of the same screens (same components, same no-image rule). */
  variant?: PhoneMockVariant;
  className?: string;
}

export function PhoneMock({ variant = "home", className }: PhoneMockProps) {
  const reduced = useReducedMotion();
  const loop = variant === "home" && !reduced;

  const [ml, setMl] = useState(variant === "water" ? TAPPED : BASE);
  const [tapped, setTapped] = useState(variant === "water");
  const [tab, setTab] = useState("home");
  const [pointer, setPointer] = useState<{ x: number; y: number } | null>(null);
  const [ripples, setRipples] = useState<Ripple[]>([]);

  const contentRef = useRef<HTMLDivElement>(null);
  const running = useRef(true);
  const rafRef = useRef(0);
  const rippleSeq = useRef(0);

  /* pause when offscreen */
  useEffect(() => {
    const el = contentRef.current;
    if (!el) return;
    const io = new IntersectionObserver(
      (entries) => {
        running.current = entries[0].isIntersecting;
      },
      { threshold: 0.2 },
    );
    io.observe(el);
    return () => io.disconnect();
  }, []);

  /* the ~7 s loop (hero home variant only) */
  useEffect(() => {
    if (!loop) return;

    const content = contentRef.current;
    const target = content?.querySelector<HTMLElement>('[data-chip="water"]');
    let t0 = 0;
    let targetPos = { x: 40, y: 120 };

    const frame = (now: number) => {
      if (!t0) t0 = now;
      const t = now - t0;
      if (!running.current) {
        rafRef.current = requestAnimationFrame(frame);
        return;
      }

      // pointer drift: idle → water chip
      if (t < T_TAP) {
        if (target && content) {
          const r = target.getBoundingClientRect();
          const c = content.getBoundingClientRect();
          targetPos = {
            x: r.left - c.left + r.width / 2,
            y: r.top - c.top + r.height / 2,
          };
        }
        if (t > T_POINT) {
          const k = easeOut(Math.min(1, (t - T_POINT) / (T_TAP - T_POINT)));
          setPointer({ x: 26 + (targetPos.x - 26) * k, y: 34 + (targetPos.y - 34) * k });
        } else {
          setPointer({ x: 26, y: 34 });
        }
      }

      // tap → ripple from the pointer position + chip press
      if (t >= T_TAP && t < T_TAP + 30) {
        setTapped(true);
        const id = ++rippleSeq.current;
        const p = targetPos;
        setRipples((rs) => [...rs, { id, x: p.x, y: p.y }]);
        window.setTimeout(
          () => setRipples((rs) => rs.filter((q) => q.id !== id)),
          700,
        );
      }

      // ring fill with counting number
      if (t >= T_FILL && t < T_HOLD) {
        const k = easeOut(Math.min(1, (t - T_FILL) / (T_HOLD - T_FILL)));
        setMl(Math.round(BASE + (TAPPED - BASE) * k));
      }

      // reset
      if (t >= T_RESET && t < T_RESET + 40) {
        setTapped(false);
        setMl(BASE);
      }
      if (t >= T_END) t0 = now - 400; // small breath, then loop

      rafRef.current = requestAnimationFrame(frame);
    };

    rafRef.current = requestAnimationFrame(frame);
    return () => cancelAnimationFrame(rafRef.current);
  }, [loop]);

  const friendsTab = variant === "friends";
  const addTab = variant === "add";
  const ringMl = friendsTab ? 1560 : ml;

  return (
    <PhoneDevice className={className}>
      <div ref={contentRef} className="pm-screen-content">
        <div className="pm-screen-inner">
          {/* greeting + streak */}
          <div className="pm-head">
            <span>Good morning, Alex</span>
            <span className="pm-streak">
              <IconFlame width="11" height="11" />
              4d streak
            </span>
          </div>

          {/* the big water ring */}
          <WaterRing
            fraction={ringMl / GOAL}
            centre={ringMl.toLocaleString("en-US")}
            sub="of 2,000 ml"
          />

          {addTab ? (
            /* add screen: the drink picker */
            <>
              <div className="pm-search">
                <IconDroplet width="10" height="10" />
                Search 24+ drinks
              </div>
              <div className="pm-card">
                {[
                  { name: "Water", ml: "+250 ml" },
                  { name: "Green tea", ml: "+200 ml" },
                  { name: "Orange juice", ml: "+250 ml" },
                  { name: "Coconut water", ml: "+300 ml" },
                ].map((d) => (
                  <div key={d.name} className="pm-row pm-card-friend">
                    <span className="pm-row-name">
                      <IconDroplet width="10" height="10" style={{ verticalAlign: "-0.2cqw" }} />{" "}
                      {d.name}
                    </span>
                    <span className="pm-row-ml">{d.ml}</span>
                  </div>
                ))}
              </div>
            </>
          ) : friendsTab ? (
            /* friends screen: the friends card with rows */
            <div className="pm-card">
              <div className="pm-head">
                <span>Friends</span>
                <span className="pm-friend-more">See all</span>
              </div>
              <div className="pm-row pm-card-friend">
                <img className="pm-friend-img" src={asset("assets/friend-plant-bloom.webp")} alt="Fern" />
                <span className="pm-row-name">Fern</span>
                <span className="pm-row-ml">1,720 ml</span>
              </div>
              <div className="pm-row pm-card-friend">
                <img className="pm-friend-img" src={asset("assets/friend-pet-adult.webp")} alt="Mochi" />
                <span className="pm-row-name">Mochi</span>
                <span className="pm-row-ml">1,910 ml</span>
              </div>
              <div className="pm-row pm-card-friend">
                <img className="pm-friend-img" src={asset("assets/friend-plant-sprout.webp")} alt="Basil" />
                <span className="pm-row-name">Basil</span>
                <span className="pm-row-ml">980 ml</span>
              </div>
            </div>
          ) : (
            <>
              {/* Dippy + mood bubble — the expression changes on tap */}
              <div className="pm-buddy">
                <Dippy
                  mood={tapped ? "excited" : "happy"}
                  className="pm-dippy"
                  style={{ width: "15cqw", height: "auto" }}
                />
                <span className="pm-bubble" style={{ width: "19cqw" }}>
                  {tapped ? "Nice! +250 ml logged." : "Time for a sip?"}
                </span>
              </div>

              {/* quick-add chips — Water is the demo-loop target */}
              <div className="pm-chips">
                <span className={`pm-chip-wrap${tapped ? " pm-chip-pressed" : ""}`}>
                  <GlassChip data-chip="water" size="sm">
                    <IconDroplet width="11" height="11" />
                    Water
                  </GlassChip>
                </span>
                <GlassChip size="sm">
                  <IconDroplet width="11" height="11" />
                  Tea
                </GlassChip>
                <GlassChip size="sm">
                  <IconDroplet width="11" height="11" />
                  Juice
                </GlassChip>
              </div>

              {/* small friends row */}
              <div className="pm-card">
                <div className="pm-friends-row">
                  <span className="pm-friend">
                    <img className="pm-friend-img" src={asset("assets/friend-plant-sprout.webp")} alt="" />
                    Fern
                  </span>
                  <span className="pm-friend">
                    <img className="pm-friend-img" src={asset("assets/friend-pet-cub.webp")} alt="" />
                    Mochi
                  </span>
                  <span className="pm-friend-more">See all</span>
                </div>
              </div>
            </>
          )}

          {/* liquid tab bar pinned at the bottom */}
          <div className="pm-tabbar">
            <GlassTabBar
              size="sm"
              flow="slide"
              items={TABS.map((t) => ({ id: t.id, icon: t.icon, label: t.name }))}
              value={friendsTab ? "friends" : tab}
              onChange={setTab}
            />
          </div>
        </div>

        {/* demo-loop pointer + tap ripple (transform/opacity only) */}
        {loop && pointer && (
          <div
            className="pm-pointer"
            style={{ transform: `translate(${pointer.x}px, ${pointer.y}px)` }}
            aria-hidden="true"
          />
        )}
        {loop &&
          ripples.map((r) => (
            <span
              key={r.id}
              className="pm-tap-ripple"
              style={{ left: r.x, top: r.y }}
              aria-hidden="true"
            />
          ))}
      </div>
    </PhoneDevice>
  );
}
