import { useRef, useState, type PointerEvent, type ReactNode } from "react";
import { ShimmerButton } from "@glinui/ui";
import { APK_CTA_LABEL, APK_CTA_LABELS, getDownloadState } from "../lib/download";
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
  /** Applies the APK label rule from APK_CTA_LABEL: "See install steps"
   *  until a real APK URL + size + SHA-256 exist, then "Get the APK". */
  apk?: boolean;
  /** Explicit label (non-apk CTAs). */
  label?: ReactNode;
  className?: string;
  onClick?: () => void;
}

interface Ripple {
  id: number;
  x: number;
  y: number;
}

/**
 * CtaButton — the ONE shared CTA (nav + hero), built on Glin ShimmerButton.
 *
 *  - single-line label (white-space: nowrap), content-sized pill with token
 *    padding (no fixed Glin height, no down-arrow)
 *  - hover (real hover devices only, via @media (hover:hover)): pointer,
 *    2px lift, brighter rim, shimmer sweep
 *  - press: scale 0.97 (springy curve) + a ring ripple from the pointer
 *    position
 *  - keyboard: native <button> → Enter and Space both fire click; visible
 *    focus ring (:focus-visible)
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
  className,
  onClick,
}: CtaButtonProps) {
  const btnRef = useRef<HTMLButtonElement>(null);
  const rippleSeq = useRef(0);
  const [ripples, setRipples] = useState<Ripple[]>([]);

  const d = apk ? getDownloadState() : null;
  const text = apk ? APK_CTA_LABEL : (label ?? APK_CTA_LABELS.ready);

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
    if (e.detail === 0 || (e.clientX === 0 && e.clientY === 0)) {
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
