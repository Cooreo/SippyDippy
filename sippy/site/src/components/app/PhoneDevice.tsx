/**
 * PhoneDevice — the shared device frame (hero + showcase).
 *
 * Live recreation, not a screenshot (brief: "NO screenshots or images of
 * the app anywhere — build it from the real components").
 *
 *  - 9:19.5 frame, always-dark bezel with a subtle rim highlight
 *  - dynamic island INSIDE the screen (it is part of the display, not a
 *    bezel sticker)
 *  - `container-type: size` → every inner size is a container unit (cqw),
 *    so nothing can overflow at any render size
 */
import type { ReactNode } from "react";

export interface PhoneDeviceProps {
  children: ReactNode;
  className?: string;
}

export function PhoneDevice({ children, className }: PhoneDeviceProps) {
  return (
    <div
      className={`device${className ? ` ${className}` : ""}`}
      role="img"
      aria-label="Sippy app on a phone"
    >
      <div className="device-screen">
        <div className="device-island" aria-hidden="true" />
        <div className="pm-statusbar" aria-hidden="true">
          <span>9:41</span>
          <span className="pm-signal">
            <svg viewBox="0 0 16 10" width="12" height="7.5" fill="currentColor" aria-hidden="true">
              <rect x="0" y="6" width="3" height="4" rx="0.8" />
              <rect x="4.5" y="4" width="3" height="6" rx="0.8" />
              <rect x="9" y="1.5" width="3" height="8.5" rx="0.8" />
              <rect x="13" y="0" width="3" height="10" rx="0.8" opacity="0.35" />
            </svg>
            <svg viewBox="0 0 24 12" width="15" height="7.5" fill="currentColor" aria-hidden="true">
              <rect x="0.5" y="0.5" width="20" height="11" rx="3" fill="none" stroke="currentColor" opacity="0.5" />
              <rect x="2.5" y="2.5" width="14" height="7" rx="1.6" />
              <rect x="22" y="3.5" width="2" height="5" rx="1" opacity="0.5" />
            </svg>
          </span>
        </div>
        {children}
        <div className="pm-homebar" aria-hidden="true" />
      </div>
    </div>
  );
}
