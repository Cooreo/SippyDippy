import type { ButtonHTMLAttributes } from "react";
import { ShimmerButton } from "./ShimmerButton";
import { useTheme } from "../../lib/theme";

export interface GlassButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** Sweep colour. Default: white tuned per theme (Glin default is 35% of
   *  the text colour — we override per the brief: "shimmer tuned to the
   *  accent" via a light sweep that reads on the blue glass). */
  shimmerColor?: string;
  /** Seconds per sweep (Glin default 2). */
  shimmerDuration?: number;
  size?: "sm" | "md" | "lg";
}

/**
 * GlassButton — Glin UI ShimmerButton, glass variant (verified API:
 * variant="glass", shimmerColor, shimmerDuration; reduced-motion compliant:
 * the sweep is removed under prefers-reduced-motion automatically).
 */
export function GlassButton({
  shimmerColor,
  shimmerDuration = 2.6,
  size = "md",
  className,
  ...rest
}: GlassButtonProps) {
  const { mode } = useTheme();
  const defaultShimmer =
    mode === "dark" ? "rgb(255 255 255 / 0.45)" : "rgb(255 255 255 / 0.55)";

  return (
    <ShimmerButton
      variant="glass"
      size={size}
      shimmerColor={shimmerColor ?? defaultShimmer}
      shimmerDuration={shimmerDuration}
      className={`glass-btn pressable${className ? ` ${className}` : ""}`}
      {...rest}
    />
  );
}
