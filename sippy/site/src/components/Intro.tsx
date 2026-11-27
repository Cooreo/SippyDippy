/**
 * The cinematic intro — 5 beats, ~3.6 s, skippable (click / tap / Esc),
 * once per session (sessionStorage, see lib/intro.ts).
 *
 *   1. near-black, a soft glow breathes
 *   2. the logo mark draws itself (stroke), then fills
 *   3. a glass droplet-lens grows from the logo (metaball wipe) — it bends
 *      the page (backdrop blur + saturation + refraction) as it passes
 *   4. the veil lifts and the hero headline words rise blur-to-sharp
 *      (the hero runs its own stagger on a matching delay)
 *   5. the nav pill drops in with a spring (GlassNav enterDelay)
 *
 * Zero CLS (fixed overlay, content always laid out underneath) and it never
 * blocks keyboard or screen readers (aria-hidden; content is in the tree).
 * Under prefers-reduced-motion the whole thing is replaced by a simple fade
 * (HomePage renders no Intro at all — mode "fade").
 */
import { useCallback, useEffect, useState } from "react";

const INTRO_MS = 3600;
const LEAVE_MS = 220;

export function Intro({ onDone }: { onDone: () => void }) {
  const [leaving, setLeaving] = useState(false);

  const finish = useCallback(() => {
    setLeaving((v) => {
      if (v) return v;
      window.setTimeout(onDone, LEAVE_MS);
      return true;
    });
  }, [onDone]);

  useEffect(() => {
    const t = window.setTimeout(finish, INTRO_MS);
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
      className={`intro${leaving ? " intro-leave" : ""}`}
      aria-hidden="true"
      onClick={finish}
    >
      <div className="intro-glow" />
      <svg
        className="intro-logo"
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
          d="M32 7 C 26.5 15.5 16 27.5 16 39 A 16 16 0 0 0 48 39 C 48 27.5 37.5 15.5 32 7 Z"
          fill="url(#introFill)"
        />
        <path
          className="intro-stroke"
          d="M32 7 C 26.5 15.5 16 27.5 16 39 A 16 16 0 0 0 48 39 C 48 27.5 37.5 15.5 32 7 Z"
          stroke="currentColor"
          strokeWidth="3.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          pathLength="100"
        />
        <path
          className="intro-glint"
          d="M23.5 33.5 Q 20.5 40 25 46.5"
          stroke="currentColor"
          strokeWidth="3"
          strokeLinecap="round"
          pathLength="100"
        />
      </svg>
      <div className="intro-veil" />
      <div className="intro-lens" />
      <span className="intro-skip-hint">click or press Esc to skip</span>
    </div>
  );
}
