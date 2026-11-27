import type { ButtonHTMLAttributes } from "react";

export interface GlassIconButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** Required: the accessible name (icons carry no text). */
  label: string;
  size?: "md" | "lg";
}

/**
 * GlassIconButton — circular glass button, 48dp minimum touch target.
 * Pass a Sippy <Icon…/> as children.
 */
export function GlassIconButton({
  label,
  size = "md",
  className,
  children,
  ...rest
}: GlassIconButtonProps) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      className={`glass pressable glass-icon-btn${
        size === "lg" ? " glass-icon-btn-lg" : ""
      }${className ? ` ${className}` : ""}`}
      {...rest}
    >
      {children}
    </button>
  );
}
