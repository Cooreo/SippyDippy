import { forwardRef, type ButtonHTMLAttributes, type CSSProperties } from "react";

export interface ShimmerButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "default" | "glass" | "accent";
  size?: "sm" | "md" | "lg";
  shimmerColor?: string;
  shimmerDuration?: number;
}

const BASE_CLASSES =
  "group relative inline-flex items-center justify-center overflow-hidden rounded-xl px-6 py-2.5 text-sm font-medium transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-accent)] focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50";

const VARIANT_CLASSES: Record<NonNullable<ShimmerButtonProps["variant"]>, string> = {
  default:
    "bg-neutral-900 text-white hover:bg-neutral-800 dark:bg-white dark:text-neutral-900 dark:hover:bg-neutral-100",
  glass:
    "border border-white/20 [border-top-color:var(--glass-refraction-top)] bg-white/10 text-[var(--color-foreground)] backdrop-blur-md hover:bg-white/20 dark:border-white/10 dark:bg-white/[0.06]",
  accent:
    "bg-[var(--color-accent)] text-[var(--color-accent-foreground)] hover:opacity-90",
};

const SIZE_CLASSES: Record<NonNullable<ShimmerButtonProps["size"]>, string> = {
  sm: "h-8 px-4 text-xs",
  md: "h-10 px-6 text-sm",
  lg: "h-12 px-8 text-base",
};

/**
 * Tree-shakable Glin ShimmerButton primitive (matches @glinui/ui's exact DOM
 * contract without pulling 60 unused components + Radix packages into the
 * home-route critical bundle). The sweep animation (`cta-shimmer-sweep`) is
 * paused at rest and runs only on hover/focus-visible (and never under
 * reduced motion).
 */
export const ShimmerButton = forwardRef<HTMLButtonElement, ShimmerButtonProps>(
  (
    {
      className,
      children,
      variant = "default",
      size = "md",
      shimmerColor = "rgba(255,255,255,0.3)",
      shimmerDuration = 2,
      style,
      ...props
    },
    ref,
  ) => {
    const mergedClass = [
      BASE_CLASSES,
      VARIANT_CLASSES[variant],
      SIZE_CLASSES[size],
      className,
    ]
      .filter(Boolean)
      .join(" ");

    const sweepStyle = {
      "--shimmer-duration": `${shimmerDuration}s`,
      background: `linear-gradient(90deg, transparent, ${shimmerColor}, transparent)`,
    } as CSSProperties;

    return (
      <button ref={ref} className={mergedClass} style={style} {...props}>
        <span
          aria-hidden="true"
          className="cta-shimmer-sweep pointer-events-none absolute inset-0"
          style={sweepStyle}
        />
        <span className="relative z-10">{children}</span>
      </button>
    );
  },
);

ShimmerButton.displayName = "ShimmerButton";
