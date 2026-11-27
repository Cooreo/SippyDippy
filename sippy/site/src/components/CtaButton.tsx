import { useRef, useState, type PointerEvent, type ReactNode } from "react";
import { ShimmerButton } from "@glinui/ui";
import { getDownloadState } from "../lib/download";
import { focusHeading, flashRim, scrollToId } from "../lib/scroll";

interface CtaButtonProps {
  /** "primary" = strongest (Sippy Blue glass, white label, AA);
   *  "ghost" = transparent glass with rim (e.g. "How it works"). */
  variant?: "primary" | "ghost";
  /** Glin ShimmerButton size — "sm" for the compact nav, "lg" for the hero. */
  size?: "sm" | "lg";
  /** Section id to scroll to on click (nav-offset aware, Lenis-aware). */
  target?: string;
  /** Heading id to receive focus after landing (tabindex=-1 in markup). */
  focusId?: string;
  /** Element id whose rim flashes once (~1.2 s) on landing. */
  flashId?: string;
  /** Applies the APK label rule: "Get the APK" only when a real APK URL +
   *  size + SHA-256 exist (getDownloadState), otherwise "See install steps"
   *  so nothing promises a download that doesn't exist yet. */
  apk?: boolean;
  /** Explicit label (non-apk CTAs). */
  label?: ReactNode;
  /** Show the down-arrow that nudges on hover. */
  arrow?: boolean;
  className?: string;
  onClick?: () => void;
}

interface Ripple {
  id: number;
  x: number;
  y: number;
}

/**
 * CtaButton — the ONE shared CTA (nav + hero), built on Glin ShimmerButton
 * (root-cause R2-2).
 *
 *  - hover (real hover devices only, via @media (hover:hover)): pointer,
 *    2px lift, brighter rim (opacity overlay), shimmer sweep, arrow +3px
 *  - press: scale 0.97 (springy curve) + a ring ripple from the pointer
 *    position (same ripple shape as the intro)
 *  - keyboard: native <button> → Enter and Space both fire click; visible
 *    focus ring (global :focus-visible)
 *  - click: single scroll path (Lenis owner) with nav offset, focus the
 *    landing heading, one 1.2 s rim flash on the card
 *  - everything animates transform/opacity only
 */
export function CtaButton({
  variant = "primary",
  size = "lg",
  target,
  focusId,
  flashId,
  apk = false,
  label,
  arrow = false,
  className,
  onClick,
}: CtaButtonProps) {
  const btnRef = useRef<HTMLButtonElement>(null);
  const rippleSeq = useRef(0);
  const [ripples, setRipples] = useState<Ripple[]>([]);

  const d = apk ? getDownloadState() : null;
  const text = apk
    ? d && d.enabled
      ? "Get the APK"
      : "See install steps"
    : (label ?? "Get the APK");

  const spawnRipple = (e: PointerEvent<HTMLButtonElement>) => {
    const el = btnRef.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    const x = e.clientX - r.left;
    const y = e.clientY - r.top;
    const id = ++rippleSeq.current;
    setRipples((rs) => [...rs, { id, x, y }]);
    window.setTimeout(
      () => setRipples((rs) => rs.filter((q) => q.id !== id)),
      650,
    );
  };

  const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    // e.detail === 0 → keyboard activation (Enter/Space): ripple from centre
    if (e.detail === 0 || e.clientX === 0 && e.clientY === 0) {
      const el = btnRef.current;
      if (el) {
        const r = el.getBoundingClientRect();
        const id = ++rippleSeq.current;
        setRipples((rs) => [...rs, { id, x: r.width / 2, y: r.height / 2 }]);
        window.setTimeout(
          () => setRipples((rs) => rs.filter((q) => q.id !== id)),
          650,
        );
      }
    }

    if (apk && d) {
      if (d.enabled) {
        window.location.href = d.url;
      } else {
        scrollToId("download");
        focusHeading("download-heading");
        flashRim(document.getElementById("download-card"));
      }
    } else if (target) {
      scrollToId(target);
      if (focusId) focusHeading(focusId);
      if (flashId) flashRim(document.getElementById(flashId));
    }
    onClick?.();
  };

  return (
    <ShimmerButton
      ref={btnRef}
      type="button"
      variant="glass"
      size={size}
      className={`cta cta--${variant} cta--${size}${className ? ` ${className}` : ""}`}
      onPointerDown={spawnRipple}
      onClick={handleClick}
    >
      <span className="cta-label">{text}</span>
      {arrow && (
        <svg
          className="cta-arrow"
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.4"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <path d="M12 4v16M5 13l7 7 7-7" />
        </svg>
      )}
      {ripples.map((r) => (
        <span
          key={r.id}
          className="cta-ripple"
          style={{ left: r.x, top: r.y }}
          aria-hidden="true"
        />
      ))}
    </ShimmerButton>
  );
}
