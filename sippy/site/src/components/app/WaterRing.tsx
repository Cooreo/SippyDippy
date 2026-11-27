/**
 * WaterRing — the big water ring, the app's hero widget.
 *
 * Real SVG sized in container-query units, so it scales with the device
 * and nothing can clip. The ring stroke is the ONE documented
 * stroke-animation exception (brief: "water ring's stroke"); the centre
 * number counts with it.
 */
const R = 46;
const CIRC = 2 * Math.PI * R;

export interface WaterRingProps {
  /** 0..1 — how full the ring is. */
  fraction: number;
  /** Big centre number (caller formats). */
  centre: string;
  /** Small caption under the number. */
  sub?: string;
  className?: string;
}

export function WaterRing({ fraction, centre, sub, className }: WaterRingProps) {
  const f = Math.max(0, Math.min(1, fraction));
  return (
    <div className={className ?? "pm-ring"}>
      <svg viewBox="0 0 100 100" aria-hidden="true">
        <circle className="pm-ring-track" cx="50" cy="50" r={R} />
        <g className="pm-ring-ticks">
          {Array.from({ length: 24 }, (_, i) => {
            const a = (i / 24) * Math.PI * 2;
            return (
              <circle
                key={i}
                cx={50 + 49.4 * Math.cos(a)}
                cy={50 + 49.4 * Math.sin(a)}
                r="0.8"
                fill="rgba(255,255,255,0.28)"
              />
            );
          })}
        </g>
        <circle
          className="pm-ring-fill"
          cx="50"
          cy="50"
          r={R}
          strokeDasharray={`${(f * CIRC).toFixed(1)} ${CIRC.toFixed(1)}`}
        />
      </svg>
      <div className="pm-ring-center">
        <div className="pm-ring-ml">{centre}</div>
        {sub ? <div className="pm-ring-sub">{sub}</div> : null}
      </div>
    </div>
  );
}
