import { useEffect, useState } from "react";
import { MotionConfig, useReducedMotion } from "motion/react";
import { LiquidTabs, type LiquidTabsItem } from "fluidkit";
import { isReducedMotionActive, useQualityTier } from "../../lib/quality";

export interface GlassTabBarProps {
  items: LiquidTabsItem[];
  /** Controlled active id. */
  value?: string;
  /** Uncontrolled initial active id. */
  defaultValue?: string;
  onChange?: (id: string) => void;
  /**
   * "stretch" = taffy pill spanning two edge springs · "slide" = one droplet.
   */
  flow?: "slide" | "stretch";
  size?: "sm" | "md" | "lg";
  className?: string;
}

/**
 * GlassTabBar — fluidkit LiquidTabs with `MotionConfig isStatic` gating so
 * LiquidTabs's internal `useAnimationFrame` runs ONLY during the 950 ms tab
 * transition and stays at 0 rAF when idle, hidden, or under reduced motion.
 */
export function GlassTabBar({
  items,
  value,
  defaultValue,
  onChange,
  flow = "stretch",
  size = "lg",
  className,
}: GlassTabBarProps) {
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

  const handleChange = (id: string) => {
    if (!reduced && !document.hidden) setSettling(true);
    onChange?.(id);
  };

  return (
    <MotionConfig isStatic={!settling || reduced}>
      <LiquidTabs
        items={items}
        value={value}
        defaultValue={defaultValue}
        onChange={handleChange}
        flow={flow}
        size={size}
        material={tier === "low" ? "flat" : "glass"}
        tint="rgba(255,255,255,0.14)"
        intensity="present"
        reflection
        light={{ x: 48, y: 8 }}
        className={className}
      />
    </MotionConfig>
  );
}
