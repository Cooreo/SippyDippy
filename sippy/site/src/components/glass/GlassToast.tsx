import type { ReactNode } from "react";
import {
  LiquidToastProvider,
  toast,
  type LiquidToastPosition,
} from "fluidkit";

/**
 * GlassToast — fluidkit's liquid toasts: condense in at a corner,
 * evaporate on dismiss (verified exports: LiquidToastProvider + toast()).
 * Wrap the app (or a section) in <GlassToastProvider>, then call
 * showGlassToast("…") from anywhere inside it.
 */
export function GlassToastProvider({
  children,
  position = "bottom-right",
}: {
  children: ReactNode;
  position?: LiquidToastPosition;
}) {
  return (
    <LiquidToastProvider
      position={position}
      material="glass"
      tint="rgba(255,255,255,0.14)"
      intensity="present"
      shadow
    >
      {children}
    </LiquidToastProvider>
  );
}

export function showGlassToast(message: ReactNode): void {
  toast(message);
}
