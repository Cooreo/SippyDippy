/**
 * Dippy — the default Sippy friend (a chubby glassy droplet). User-renameable
 * in the app; on the website he appears in the hero mockup, showcase and
 * download card with simple moods.
 */
import { useId } from "react";
import type { CSSProperties } from "react";

export type DippyMood = "idle" | "happy" | "excited" | "sad";

export function Dippy({
  mood = "idle",
  size = 72,
  className,
  style,
}: {
  mood?: DippyMood;
  size?: number;
  className?: string;
  style?: CSSProperties;
}) {
  // per-instance gradient id — several Dippies share one page, so a fixed
  // id would collide
  const bodyId = `dippyBody${useId().replace(/[^a-zA-Z0-9]/g, "")}`;
  return (
    <svg
      viewBox="0 0 64 64"
      width={size}
      height={size}
      className={className}
      style={style}
      role="img"
      aria-label={`Dippy, a glass droplet friend, looks ${mood}`}
    >
      <defs>
        <linearGradient
          id={bodyId}
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
        d="M32 7 C 26.5 15.5 16 27.5 16 39 A 16 16 0 0 0 48 39 C 48 27.5 37.5 15.5 32 7 Z"
        fill={`url(#${bodyId})`}
      />
      <path
        d="M23.5 33.5 Q 20.5 40 25 46.5"
        stroke="rgba(255,255,255,0.85)"
        strokeWidth="2.6"
        strokeLinecap="round"
        fill="none"
      />
      {mood === "idle" && (
        <g>
          <circle cx="26.5" cy="40" r="2.4" fill="#0B1B3D" />
          <circle cx="37.5" cy="40" r="2.4" fill="#0B1B3D" />
          <path
            d="M27.5 46.5 Q 32 49.8 36.5 46.5"
            stroke="#0B1B3D"
            strokeWidth="2.2"
            strokeLinecap="round"
            fill="none"
          />
        </g>
      )}
      {mood === "happy" && (
        <g>
          <path
            d="M24.5 40.5 Q 26.5 38.2 28.5 40.5"
            stroke="#0B1B3D"
            strokeWidth="2.2"
            strokeLinecap="round"
            fill="none"
          />
          <path
            d="M35.5 40.5 Q 37.5 38.2 39.5 40.5"
            stroke="#0B1B3D"
            strokeWidth="2.2"
            strokeLinecap="round"
            fill="none"
          />
          <path
            d="M26.5 46 Q 32 51 37.5 46"
            stroke="#0B1B3D"
            strokeWidth="2.4"
            strokeLinecap="round"
            fill="none"
          />
          <ellipse cx="22.6" cy="44.4" rx="2.6" ry="1.6" fill="rgba(255,150,170,0.5)" />
          <ellipse cx="41.4" cy="44.4" rx="2.6" ry="1.6" fill="rgba(255,150,170,0.5)" />
        </g>
      )}
      {mood === "excited" && (
    <g>
      {/* wide-open smile, spark eyes, little joy sparks */}
      <path
        d="M24 38.5 L27.5 40 L24 41.5"
        fill="#0B1B3D"
        stroke="#0B1B3D"
        strokeWidth="1.4"
        strokeLinejoin="round"
      />
      <path
        d="M40 38.5 L36.5 40 L40 41.5"
        fill="#0B1B3D"
        stroke="#0B1B3D"
        strokeWidth="1.4"
        strokeLinejoin="round"
      />
      <path
        d="M25.5 45.5 Q 32 53.5 38.5 45.5 Q 32 48.5 25.5 45.5 Z"
        fill="#0B1B3D"
      />
      <ellipse cx="21.4" cy="43.4" rx="2.8" ry="1.8" fill="rgba(255,150,170,0.55)" />
      <ellipse cx="42.6" cy="43.4" rx="2.8" ry="1.8" fill="rgba(255,150,170,0.55)" />
      <path
        d="M14.5 27 l1.2 2.4 2.6 .4 -1.9 1.8 .5 2.6 -2.4 -1.3 -2.4 1.3 .5 -2.6 -1.9 -1.8 2.6 -.4 Z"
        fill="rgba(255,255,255,0.75)"
      />
      <path
        d="M49.5 24.5 l1 2 2.2 .3 -1.6 1.5 .4 2.2 -2 -1.1 -2 1.1 .4 -2.2 -1.6 -1.5 2.2 -.3 Z"
        fill="rgba(255,255,255,0.65)"
      />
    </g>
  )}
  {mood === "sad" && (
        <g>
          <circle cx="26.5" cy="41" r="2.4" fill="#0B1B3D" />
          <circle cx="37.5" cy="41" r="2.4" fill="#0B1B3D" />
          <path
            d="M28 48.8 Q 32 46 36 48.8"
            stroke="#0B1B3D"
            strokeWidth="2.2"
            strokeLinecap="round"
            fill="none"
          />
        </g>
      )}
    </svg>
  );
}
