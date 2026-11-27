/**
 * GlassSurface — the base of every Sippy glass element (web binding of the
 * brief's glass recipe). Layers: backdrop blur + saturation boost, tint,
 * 1px gradient rim, inner highlight, layered shadow, 3% grain (all CSS,
 * class .glass), plus optional edge refraction (Chromium-only progressive
 * enhancement, gated by lib/fx.ts → ?fx=blur forces the clean fallback).
 */
import {
  type CSSProperties,
  type HTMLAttributes,
} from "react";
import { useFxMode } from "../../lib/fx";
import { useQualityTier } from "../../lib/quality";
import { blur as blurTokens, glassSaturation, radius as radiusTokens } from "../../lib/tokens";

export interface GlassSurfaceProps extends HTMLAttributes<HTMLDivElement> {
  radius?: keyof typeof radiusTokens;
  blur?: keyof typeof blurTokens;
  /**
   * Force refraction on/off. Default: follows ?fx= / feature detection.
   * Always off unless the current fx mode is "full" (capability gate).
   */
  refraction?: boolean;
  /** Static top-left specular glow (≤10% alpha, fades before the border). */
  specular?: boolean;
  /** Override the glass tint (any CSS color). */
  tint?: string;
  /** Keep live backdrop-filter on the low quality tier (capped at ≤3 surfaces). */
  lowTierBlur?: boolean;
}

export function GlassSurface({
  radius = "card",
  blur = "md",
  refraction,
  specular = false,
  tint,
  lowTierBlur = false,
  className,
  style,
  children,
  ...rest
}: GlassSurfaceProps) {
  const fx = useFxMode();
  const tier = useQualityTier();
  const isLow = tier === "low";
  const useLiveBlur = !isLow || lowTierBlur;

  const doRefract = !isLow && (refraction ?? true) && fx === "full";
  const bpx = isLow ? Math.min(blurTokens[blur], 16) : blurTokens[blur];
  const sat = isLow ? 140 : glassSaturation;
  const rpx =
    radius === "pill"
      ? `${radiusTokens.pill}px`
      : `${radiusTokens[radius]}px`;

  const filterChain = `blur(${bpx}px) saturate(${sat}%)${
    doRefract ? " url(#sippyRefraction)" : ""
  }`;

  const glassStyle: CSSProperties = {
    borderRadius: rpx,
    ...(useLiveBlur
      ? {
          backdropFilter: filterChain,
          WebkitBackdropFilter: filterChain,
        }
      : {
          backdropFilter: "none",
          WebkitBackdropFilter: "none",
        }),
    ...(tint ? { background: tint } : null),
    ...style,
  };

  return (
    <div
      className={`glass${className ? ` ${className}` : ""}`}
      data-glass-static={useLiveBlur ? undefined : "true"}
      style={glassStyle}
      {...rest}
    >
      {specular && <span aria-hidden="true" className="spec" />}
      {children}
    </div>
  );
}
