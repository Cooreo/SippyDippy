import { useEffect, useState, type ReactNode } from "react";
import { MotionConfig, useReducedMotion } from "motion/react";
import { LiquidSwitch } from "fluidkit";
import { isReducedMotionActive, useQualityTier } from "../../lib/quality";

export interface GlassToggleProps {
  checked?: boolean;
  defaultChecked?: boolean;
  onCheckedChange?: (checked: boolean) => void;
  /** Label rendered beside the track (fluidkit associates it natively). */
  label?: ReactNode;
  /** Thumb diameter in px (fluidkit default 24). */
  size?: number;
  /** Track tint while on. Sippy Blue by default. */
  checkedTint?: string;
  disabled?: boolean;
}

/**
 * GlassToggle — fluidkit LiquidSwitch with `MotionConfig isStatic` gating so
 * its internal `useAnimationFrame` loop runs ONLY during the 950 ms toggle
 * settle window and stays at 0 rAF when idle, hidden, or under reduced motion.
 */
export function GlassToggle({
  checked,
  defaultChecked,
  onCheckedChange,
  label,
  size = 24,
  checkedTint = "#2E7CF6",
  disabled,
}: GlassToggleProps) {
  const tier = useQualityTier();
  const osReduced = useReducedMotion() ?? false;
  const reduced = osReduced || isReducedMotionActive();
  const [settling, setSettling] = useState(false);

  useEffect(() => {
    if (!settling) return;
    const timer = window.setTimeout(() => setSettling(false), 950);
    const onVis = () => {
      if (document.hidden) setSettling(false);
    };
    document.addEventListener("visibilitychange", onVis);
    return () => {
      window.clearTimeout(timer);
      document.removeEventListener("visibilitychange", onVis);
    };
  }, [settling]);

  const handleChange = (next: boolean) => {
    if (!reduced && !document.hidden) setSettling(true);
    onCheckedChange?.(next);
  };

  return (
    <MotionConfig isStatic={!settling || reduced}>
      <LiquidSwitch
        checked={checked}
        defaultChecked={defaultChecked}
        onCheckedChange={handleChange}
        label={label}
        size={size}
        checkedTint={checkedTint}
        material={tier === "low" ? "flat" : "glass"}
        intensity="present"
        disabled={disabled}
      />
    </MotionConfig>
  );
}
