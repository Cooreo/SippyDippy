import { useEffect, useState, type ReactNode } from "react";
import { motion } from "motion/react";
import { LiquidSwitch } from "fluidkit";
import { useTheme } from "../../lib/theme";
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
 * GlassNav — the floating glass pill navbar (reference 3: image edge visible
 * THROUGH the bar; sun/moon toggle; white pill CTA). Phase 3 adds:
 * compresses on scroll, and a liquid lens that slides to the active section.
 */
export function GlassNav({
  links,
  sections,
  cta,
  enterDelay = 0,
}: GlassNavProps) {
  const { mode, toggle } = useTheme();
  const [activeId, setActiveId] = useState<string | null>(null);
  const [scrolled, setScrolled] = useState(false);

  // Scroll-spy: which section is in the "hot band" of the viewport.
  useEffect(() => {
    if (!sections?.length) return;
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

  // Scroll compression (reference 3's "compresses on scroll").
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const navLinks: { id: string; label: string; href: string }[] =
    sections?.map((s) => ({ id: s.id, label: s.label, href: `#${s.id}` })) ??
    links?.map((l) => ({ id: l, label: l, href: `#${l.toLowerCase()}` })) ??
    [];

  return (
    <motion.nav
      initial={{ y: -26, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{
        delay: enterDelay / 1000,
        type: "spring",
        stiffness: 300,
        damping: 30,
        mass: 1,
      }}
      className={`glass gnav${scrolled ? " gnav-scrolled" : ""}`}
      style={{ borderRadius: "999px" }}
      aria-label="Primary"
    >
      <a
        className="gnav-brand"
        href={import.meta.env.BASE_URL}
        onClick={(e) => {
          e.preventDefault();
          window.scrollTo({ top: 0, behavior: "smooth" });
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
        <LiquidSwitch
          checked={mode === "dark"}
          onCheckedChange={() => toggle()}
          size={20}
          checkedTint="#2E7CF6"
          material="glass"
          aria-label="Toggle dark mode"
        />
        <span className="gnav-theme" aria-hidden="true">
          <IconMoon width={16} height={16} />
        </span>
        {cta ?? (
          // APK label rule: never promise a download that doesn't exist
          <button type="button" className="pill-cta pressable">
            See install steps
          </button>
        )}
      </div>
    </motion.nav>
  );
}
