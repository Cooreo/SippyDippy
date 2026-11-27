/**
 * The cinematic intro — root-cause fix E (sequence per brief):
 *
 *   0.  near-black, a soft glow breathes
 *   1.  the droplet starts ABOVE the viewport at the impact x and falls
 *       with gravity-like acceleration (550 ms ease-in) with a slight stretch
 *   2.  impact at the EXACT centre of the logo target: squash + splash
 *   3.  three concentric ripples expand from that same point, staggered
 *       120 ms; the FIRST ripple reveals the logo
 *   4.  the logo draws itself (stroke), then fills
 *   5.  the overlay lifts to the hero (words rise), nav pill drops in
 *
 * The impact point is computed ONCE from the logo target's
 * getBoundingClientRect (recomputed on resize + font load) and shared with
 * the droplet, every ripple and the reveal as CSS custom properties
 * (--impact-x/--impact-y) — one origin, no hard-coded offsets.
 *
 * Skippable (click / tap / Esc), once per session (see lib/intro.ts),
 * forced with ?intro=1 / ?intro=0, reduced-motion → simple fade (HomePage
 * renders no Intro at all). Zero CLS (fixed overlay; content laid out
 * underneath) and never blocks keyboard or screen readers.
 */
import { useCallback, useEffect, useRef, useState } from "react";
import {
  IMPACT_MS,
  INTRO_TOTAL_MS,
  LOGO_DRAW_MS,
  LOGO_FILL_MS,
  LOGO_REVEAL_MS,
  RIPPLE_COUNT,
  impactPoint,
  impactVars,
} from "../lib/intro-impact";

const LEAVE_MS = 220;

export function Intro({ onDone }: { onDone: () => void }) {
  const rootRef = useRef<HTMLDivElement>(null);
  const logoRef = useRef<SVGSVGElement>(null);
  const [leaving, setLeaving] = useState(false);

  const finish = useCallback(() => {
    setLeaving((v) => {
      if (v) return v;
      window.setTimeout(onDone, LEAVE_MS);
      return true;
    });
  }, [onDone]);

  // Measure the impact point once (and on resize / font load) and publish it
  // as CSS variables shared by droplet + ripples + reveal.
  useEffect(() => {
    const measure = () => {
      const root = rootRef.current;
      const logo = logoRef.current;
      if (!root || !logo) return;
      const r = logo.getBoundingClientRect();
      const p = impactPoint(r, {
        width: window.innerWidth,
        height: window.innerHeight,
      });
      const vars = impactVars(p);
      for (const [k, v] of Object.entries(vars)) {
        root.style.setProperty(k, v as string);
      }
    };
    measure();
    window.addEventListener("resize", measure);
    let live = true;
    document.fonts?.ready?.then(() => {
      if (live) measure();
    });
    const raf = requestAnimationFrame(measure);
    return () => {
      live = false;
      cancelAnimationFrame(raf);
      window.removeEventListener("resize", measure);
    };
  }, []);

  useEffect(() => {
    const t = window.setTimeout(finish, INTRO_TOTAL_MS);
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") finish();
    };
    window.addEventListener("keydown", onKey);
    return () => {
      window.clearTimeout(t);
      window.removeEventListener("keydown", onKey);
    };
  }, [finish]);

  return (
    <div
      ref={rootRef}
      className={`intro${leaving ? " intro-leave" : ""}`}
      aria-hidden="true"
      onClick={finish}
    >
      <div className="intro-glow" />
      {/* the falling droplet — tip lands exactly on the impact point */}
      <div className="intro-drop" />
      <div className="intro-splash" />
      {/* concentric ripples from the same point, staggered 120 ms */}
      {Array.from({ length: RIPPLE_COUNT }, (_, i) => (
        <div
          key={i}
          className="intro-ripple"
          style={{ animationDelay: `${IMPACT_MS + 60 + i * 120}ms` }}
        />
      ))}
      <svg
        ref={logoRef}
        className="intro-logo"
        style={{
          animationDelay: `${LOGO_REVEAL_MS}ms`,
        }}
        viewBox="0 0 64 64"
        fill="none"
        focusable="false"
      >
        <defs>
          <linearGradient
            id="introFill"
            x1="32"
            y1="7"
            x2="32"
            y2="56"
            gradientUnits="userSpaceOnUse"
          >
            <stop offset="0" stopColor="#5B9BFF" />
            <stop offset="1" stopColor="#1D5FE0" />
          </linearGradient>
        </defs>
        <path
          className="intro-fill"
          style={{ animationDelay: `${LOGO_FILL_MS}ms` }}
          d="M32 7 C 26.5 15.5 16 27.5 16 39 A 16 16 0 0 0 48 39 C 48 27.5 37.5 15.5 32 7 Z"
          fill="url(#introFill)"
        />
        <path
          className="intro-stroke"
          style={{ animationDelay: `${LOGO_DRAW_MS}ms` }}
          d="M32 7 C 26.5 15.5 16 27.5 16 39 A 16 16 0 0 0 48 39 C 48 27.5 37.5 15.5 32 7 Z"
          stroke="currentColor"
          strokeWidth="3.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          pathLength="100"
        />
      </svg>
      <span className="intro-skip-hint">click or press Esc to skip</span>
    </div>
  );
}
