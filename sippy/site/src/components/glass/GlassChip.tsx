import type { HTMLAttributes } from "react";
import { Chip } from "@glinui/ui";

export interface GlassChipProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: "success" | "warning" | "info" | "neutral" | "danger";
  size?: "sm" | "md" | "lg";
  active?: boolean;
}

/**
 * GlassChip — Glin UI Chip, glass variant (verified props: variant, tone,
 * size). `active` renders the info tone so selected states read as accent.
 */
export function GlassChip({
  tone = "neutral",
  size = "md",
  active = false,
  className,
  children,
  ...rest
}: GlassChipProps) {
  return (
    <Chip
      variant="glass"
      tone={active ? "info" : tone}
      size={size}
      className={`pressable${className ? ` ${className}` : ""}`}
      data-active={active || undefined}
      {...rest}
    >
      {children}
    </Chip>
  );
}
