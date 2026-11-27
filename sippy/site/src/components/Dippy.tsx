/**
 * Dippy — the default Sippy friend (a chubby glassy droplet). User-renameable
 * in the app; on the website he appears in the hero mockup, showcase and
 * download card with simple moods.
 */
export type DippyMood = "idle" | "happy" | "sad";

export function Dippy({
  mood = "idle",
  size = 72,
  className,
}: {
  mood?: DippyMood;
  size?: number;
  className?: string;
}) {
  return (
    <svg
      viewBox="0 0 64 64"
      width={size}
      height={size}
      className={className}
      role="img"
      aria-label={`Dippy, a glass droplet friend, looks ${mood}`}
    >
      <defs>
        <linearGradient
          id="dippyBody"
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
        fill="url(#dippyBody)"
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
