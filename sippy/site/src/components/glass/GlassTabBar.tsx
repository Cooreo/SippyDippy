import { LiquidTabs, type LiquidTabsItem } from "fluidkit";

export interface GlassTabBarProps {
  items: LiquidTabsItem[];
  /** Controlled active id. */
  value?: string;
  /** Uncontrolled initial active id. */
  defaultValue?: string;
  onChange?: (id: string) => void;
  /**
   * "stretch" = taffy pill spanning two edge springs (the reference-4 liquid
   * lens that bends between tabs) · "slide" = one droplet with inertia.
   */
  flow?: "slide" | "stretch";
  size?: "sm" | "md" | "lg";
  className?: string;
}

/**
 * GlassTabBar — fluidkit LiquidTabs (verified API: items/value/onChange,
 * flow, material, tint, intensity, light, reflection). The capsule is glass
 * with a scene light from the top-left so the active lens picks up a glint.
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
  return (
    <LiquidTabs
      items={items}
      value={value}
      defaultValue={defaultValue}
      onChange={onChange}
      flow={flow}
      size={size}
      material="glass"
      tint="rgba(255,255,255,0.14)"
      intensity="present"
      reflection
      light={{ x: 48, y: 8 }}
      className={className}
    />
  );
}
