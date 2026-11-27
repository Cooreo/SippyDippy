import { useEffect, useRef, useState, type ReactNode } from "react";
import { MotionConfig, motion, useReducedMotion } from "motion/react";
import { LiquidSwitch } from "fluidkit";
import { useTheme } from "../../lib/theme";
import { getLenis, wakeLenis } from "../../lib/lenis";
import { isReducedMotionActive, useQualityTier } from "../../lib/quality";
import { APK_CTA_LABEL } from "../../lib/download";
import { IconDroplet, IconMoon, IconSun } from "../Icons";

export interface GlassNavSection {
  id: string;
  label: string;
}

export interface GlassNavProps {
  /** Simple links (gallery demo mode — hrefs as given). */
  links?: string[];
  /** Page sections: enables scroll-spy + the sliding liquid lens (reference 4). */
  sections?: GlassNavSection[];
  /**
   * Shared CTA node (round 2: the CtaButton, compact size). When omitted,
   * the built-in pill-cta is rendered (gallery/legacy callers).
   */
  cta?: ReactNode;
  /** Entry animation delay in ms — the intro's "nav pill drops in" beat. */
  enterDelay?: number;
}

/**
 * GlassNav — the floating glass pill navbar. Scroll compression mutates classList
 * directly on the DOM ref (zero React setState on scroll), and LiquidSwitch's
 * internal `useAnimationFrame` loop is gated via `MotionConfig isStatic` so it
 * ticks ONLY during the 900 ms liquid toggle animation and runs 0 rAF at rest.
 */
export function GlassNav({
  links,
  sections,
  cta,
  enterDelay = 0,
}: GlassNavProps) {
  const { mode, toggle } = useTheme();
  const tier = useQualityTier();
  const osReduced = useReducedMotion() ?? false;
  const reduced = osReduced || isReducedMotionActive();
  const navRef = useRef<HTMLElement>(null);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [switchSettling, setSwitchSettling] = useState(false);

  // Scroll-spy: skipped on narrow viewports where `.gnav-links` is hidden.
  useEffect(() => {
    if (!sections?.length) return;
    if (typeof window !== "undefined" && window.innerWidth > 0 && window.innerWidth <= 780) {
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) setActiveId(entry.target.id);
        }
      },
      { rootMargin: "-35% 0px -55% 0px" },
    );
    sections.forEach((s) => {
      const el = document.getElementById(s.id);
      if (el) observer.observe(el);
    });
    return () => observer.disconnect();
  }, [sections]);

  // Scroll compression — classList toggled only on threshold crossings (0 React renders).
  useEffect(() => {
    const nav = navRef.current;
    if (!nav) return;
    let isScrolled = window.scrollY > 24;
    nav.classList.toggle("gnav-scrolled", isScrolled);
    const onScroll = () => {
      const next = window.scrollY > 24;
      if (next !== isScrolled) {
        isScrolled = next;
        nav.classList.toggle("gnav-scrolled", next);
      }
    };
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  // Pause LiquidSwitch's internal rAF if tab becomes hidden mid-settle.
  useEffect(() => {
    if (!switchSettling) return;
    const timer = window.setTimeout(() => setSwitchSettling(false), 950);
    const onVis = () => {
      if (document.hidden) setSwitchSettling(false);
    };
    document.addEventListener("visibilitychange", onVis);
    return () => {
      window.clearTimeout(timer);
      document.removeEventListener("visibilitychange", onVis);
    };
  }, [switchSettling]);

  const handleThemeToggle = () => {
    if (!reduced && !document.hidden) {
      setSwitchSettling(true);
    }
    toggle();
  };

  const navLinks: { id: string; label: string; href: string }[] =
    sections?.map((s) => ({ id: s.id, label: s.label, href: `#${s.id}` })) ??
    links?.map((l) => ({ id: l, label: l, href: `#${l.toLowerCase()}` })) ??
    [];

  return (
    <motion.nav
      ref={navRef}
      initial={{ y: -26, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{
        delay: enterDelay / 1000,
        type: "spring",
        stiffness: 300,
        damping: 30,
        mass: 1,
      }}
      className="glass gnav"
      style={{ borderRadius: "999px" }}
      aria-label="Primary"
    >
      <a
        className="gnav-brand"
        href={import.meta.env.BASE_URL}
        onClick={(e) => {
          e.preventDefault();
          const lenis = getLenis();
          if (lenis && !reduced) {
            wakeLenis();
            lenis.scrollTo(0, { duration: 0.9 });
          } else {
            window.scrollTo({ top: 0, behavior: reduced ? "auto" : "smooth" });
          }
        }}
      >
        <IconDroplet width={20} height={20} />
        Sippy
      </a>
      <div className="gnav-links">
        {navLinks.map((l) => (
          <a
            key={l.id}
            className={`gnav-link${activeId === l.id ? " gnav-link-active" : ""}`}
            href={l.href}
          >
            {activeId === l.id && (
              <motion.span
                layoutId="nav-lens"
                className="nav-lens"
                transition={{ type: "spring", stiffness: 500, damping: 38, mass: 1 }}
              />
            )}
            {l.label}
          </a>
        ))}
      </div>
      <div className="gnav-actions">
        <span className="gnav-theme" aria-hidden="true">
          <IconSun width={16} height={16} />
        </span>
        <MotionConfig isStatic={!switchSettling || reduced}>
          <LiquidSwitch
            checked={mode === "dark"}
            onCheckedChange={handleThemeToggle}
            size={20}
            checkedTint="#2E7CF6"
            material={tier === "low" ? "flat" : "glass"}
            aria-label="Toggle dark mode"
          />
        </MotionConfig>
        <span className="gnav-theme" aria-hidden="true">
          <IconMoon width={16} height={16} />
        </span>
        {cta ?? (
          <button type="button" className="pill-cta pressable">
            {APK_CTA_LABEL}
          </button>
        )}
      </div>
    </motion.nav>
  );
}
