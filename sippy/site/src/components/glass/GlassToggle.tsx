import type { ReactNode } from "react";
import { LiquidSwitch } from "fluidkit";

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
 * GlassToggle — fluidkit LiquidSwitch (verified API: checked,
 * onCheckedChange, label, size, checkedTint; real hidden checkbox
 * underneath; reduced motion degrades to a snapping thumb automatically).
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
  return (
    <LiquidSwitch
      checked={checked}
      defaultChecked={defaultChecked}
      onCheckedChange={onCheckedChange}
      label={label}
      size={size}
      checkedTint={checkedTint}
      material="glass"
      intensity="present"
      disabled={disabled}
    />
  );
}
