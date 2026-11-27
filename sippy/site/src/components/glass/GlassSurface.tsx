/**
 * GlassSurface — the base of every Sippy glass element (web binding of the
 * brief's glass recipe). Layers: backdrop blur + saturation boost, tint,
 * 1px gradient rim, inner highlight, layered shadow, 3% grain (all CSS,
 * class .glass), plus optional edge refraction (Chromium-only progressive
 * enhancement, gated by lib/fx.ts → ?fx=blur forces the clean fallback).
 */
import {
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type PointerEvent,
} from "react";
import { useFxMode } from "../../lib/fx";
import { blur as blurTokens, glassSaturation, radius as radiusTokens } from "../../lib/tokens";

export interface GlassSurfaceProps extends HTMLAttributes<HTMLDivElement> {
  radius?: keyof typeof radiusTokens;
  blur?: keyof typeof blurTokens;
  /**
   * Force refraction on/off. Default: follows ?fx= / feature detection.
   * Always off unless the current fx mode is "full" (capability gate).
   */
  refraction?: boolean;
  /** Cursor-following specular highlight. */
  specular?: boolean;
  /** Override the glass tint (any CSS color). */
  tint?: string;
}

export function GlassSurface({
  radius = "card",
  blur = "md",
  refraction,
  specular = false,
  tint,
  className,
  style,
  children,
  onPointerMove,
  ...rest
}: GlassSurfaceProps) {
  const fx = useFxMode();
  const [spot, setSpot] = useState<{ x: number; y: number } | null>(null);

  const doRefract = (refraction ?? true) && fx === "full";
  const bpx = blurTokens[blur];
  const rpx =
    radius === "pill"
      ? `${radiusTokens.pill}px`
      : `${radiusTokens[radius]}px`;

  const filterChain = `blur(${bpx}px) saturate(${glassSaturation}%)${
    doRefract ? " url(#sippyRefraction)" : ""
  }`;

  const glassStyle: CSSProperties = {
    borderRadius: rpx,
    backdropFilter: filterChain,
    WebkitBackdropFilter: filterChain,
    ...(tint ? { background: tint } : null),
    ...style,
  };

  const handlePointerMove = (e: PointerEvent<HTMLDivElement>) => {
    if (specular) {
      const r = e.currentTarget.getBoundingClientRect();
      setSpot({ x: e.clientX - r.left, y: e.clientY - r.top });
    }
    onPointerMove?.(e);
  };

  return (
    <div
      className={`glass${className ? ` ${className}` : ""}`}
      style={glassStyle}
      onPointerMove={handlePointerMove}
      {...rest}
    >
      {specular && spot && (
        <span
          aria-hidden="true"
          className="spec"
          style={{
            background: `radial-gradient(160px at ${spot.x}px ${spot.y}px, rgba(255,255,255,0.16), transparent 65%)`,
          }}
        />
      )}
      {children}
    </div>
  );
}
