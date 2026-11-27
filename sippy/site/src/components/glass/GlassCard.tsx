import type { GlassSurfaceProps } from "./GlassSurface";
import { GlassSurface } from "./GlassSurface";

/**
 * GlassCard — the workhorse surface (radius 28, blur 32 by default).
 */
export function GlassCard({
  radius = "card",
  blur = "md",
  className,
  ...rest
}: GlassSurfaceProps) {
  return (
    <GlassSurface
      radius={radius}
      blur={blur}
      className={`glass-card${className ? ` ${className}` : ""}`}
      {...rest}
    />
  );
}
