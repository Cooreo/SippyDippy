import {
  useEffect,
  useRef,
  useState,
  type KeyboardEvent,
} from "react";
import {
  motion,
  useMotionValue,
  useReducedMotion,
  useSpring,
  useTransform,
  type PanInfo,
} from "motion/react";
import { PhoneMock, type PhoneMockVariant } from "../components/PhoneMock";
import { asset } from "../lib/router";

const SCREENS: {
  variant: PhoneMockVariant;
  label: string;
  caption: string;
}[] = [
  { variant: "home", label: "Today", caption: "Log a sip, watch the goal fill." },
  { variant: "add", label: "Add", caption: "Pick from 24+ real drinks." },
  { variant: "friends", label: "Friends", caption: "Meet Dippy — rename, add, care." },
];

const N = SCREENS.length;
/** Carousel: each slide is 88% of the stage → neighbours peek. */
const ITEM_PCT = 88;
const TRACK_PCT = ITEM_PCT * N;
/** x (as % of the track) that centers slide i in the stage */
const slideX = (i: number) =>
  ((50 - ITEM_PCT / 2 - i * ITEM_PCT) / TRACK_PCT) * 100;

function useMedia(query: string): boolean {
  const [m, setM] = useState(() => window.matchMedia(query).matches);
  useEffect(() => {
    const q = window.matchMedia(query);
    const on = () => setM(q.matches);
    on();
    q.addEventListener("change", on);
    return () => q.removeEventListener("change", on);
  }, [query]);
  return m;
}

/**
 * App showcase (Phase 3 fix C) — NO scroll pinning, no scroll hijacking.
 *
 * Desktop (fine pointer, ≥768px): three phones in a fixed-gap row, no
 * overlap, fixed-height captions. The phone under the pointer (or focused)
 * is scale 1; the others scale 0.88 at 70% opacity (still readable). The
 * pointer drives a subtle 3D tilt of the whole group. Click or focus
 * selects (persistent).
 *
 * Touch / narrow: one phone centered with neighbours peeking, horizontal
 * drag with spring snap, dots indicator, tap left/right to advance.
 *
 * Keyboard: ← → (and Home/End) move selection with roving tabindex, visible
 * focus, aria-current. Reduced motion: no tilt, no springs.
 */
export function Showcase() {
  const reduced = useReducedMotion() ?? false;
  const isCoarse = useMedia("(pointer: coarse)");
  const isNarrow = useMedia("(max-width: 767px)");
  const carousel = isCoarse || isNarrow;

  const [selected, setSelected] = useState(0);
  const [hovered, setHovered] = useState<number | null>(null);
  const stageRef = useRef<HTMLDivElement>(null);
  const itemRefs = useRef<(HTMLDivElement | null)[]>([]);

  // subtle 3D tilt of the group from pointer position (desktop only)
  const px = useMotionValue(0);
  const py = useMotionValue(0);
  const rotY = useSpring(useTransform(px, [-0.5, 0.5], [-3.2, 3.2]), {
    stiffness: 120,
    damping: 20,
  });
  const rotX = useSpring(useTransform(py, [-0.5, 0.5], [2.4, -2.4]), {
    stiffness: 120,
    damping: 20,
  });

  const active = hovered ?? selected;
  const spring = reduced
    ? { duration: 0.12 }
    : { type: "spring" as const, stiffness: 260, damping: 30 };

  const onGroupPointerMove = (e: React.PointerEvent) => {
    if (carousel || reduced) return;
    const el = stageRef.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    px.set((e.clientX - r.left) / r.width - 0.5);
    py.set((e.clientY - r.top) / r.height - 0.5);
  };
  const onGroupPointerLeave = () => {
    setHovered(null);
    px.set(0);
    py.set(0);
  };

  const onKey = (e: KeyboardEvent<HTMLDivElement>) => {
    const focusItem = (i: number) => {
      const t = itemRefs.current[i];
      if (t) t.focus();
      setSelected(i);
    };
    if (e.key === "ArrowRight" || e.key === "ArrowDown") {
      e.preventDefault();
      focusItem((selected + 1) % N);
    } else if (e.key === "ArrowLeft" || e.key === "ArrowUp") {
      e.preventDefault();
      focusItem((selected - 1 + N) % N);
    } else if (e.key === "Home") {
      e.preventDefault();
      focusItem(0);
    } else if (e.key === "End") {
      e.preventDefault();
      focusItem(N - 1);
    }
  };

  const onDragEnd = (_: unknown, info: PanInfo) => {
    const dist = info.offset.x;
    const vel = info.velocity.x;
    if (dist < -40 || vel < -400) setSelected((s) => Math.min(N - 1, s + 1));
    else if (dist > 40 || vel > 400) setSelected((s) => Math.max(0, s - 1));
  };

  const phones = SCREENS.map((s, i) => {
    const isActive = i === active;
    return (
      <div
        key={s.variant}
        ref={(el) => {
          itemRefs.current[i] = el;
        }}
        className="sc-item"
        style={
          carousel
            ? undefined
            : {
                scale: isActive ? 1 : 0.88,
                opacity: isActive ? 1 : 0.7,
                transition: reduced
                  ? "none"
                  : "scale .35s cubic-bezier(.22,1,.36,1), opacity .35s ease",
              }
        }
        role="group"
        id={`sc-panel-${i}`}
        aria-label={`Screen ${i + 1} of ${N}: ${s.label} — ${s.caption}`}
        tabIndex={i === selected ? 0 : -1}
        onPointerEnter={() => !carousel && setHovered(i)}
        onClick={() => {
          setSelected(i);
          itemRefs.current[i]?.focus();
        }}
        onFocus={() => setSelected(i)}
      >
        <div className="sc-phone">
          <PhoneMock variant={s.variant} />
        </div>
        <div className="sc-caption">
          <b>{s.label}</b>
          <span>{s.caption}</span>
        </div>
      </div>
    );
  });

  const dots = (
      <div className="sc-dots" role="tablist" aria-label="Choose screen">
      {SCREENS.map((s, i) => (
        <button
          key={s.variant}
          type="button"
          role="tab"
          aria-selected={i === selected}
          aria-controls={`sc-panel-${i}`}
          aria-label={`Go to screen ${i + 1}: ${s.label}`}
          className={`sc-dot${i === selected ? " sc-dot-on" : ""}`}
          onClick={() => setSelected(i)}
        />
      ))}
    </div>
  );

  return (
    <section id="showcase" className="showcase">
      {/* friends art flanking the phones (alpha-keyed, feathered by the art) */}
      <img
        className="showcase-friend showcase-friend-l"
        src={asset("assets/friend-plant-sprout.webp")}
        alt=""
        width={120}
        height={138}
        loading="lazy"
        decoding="async"
        aria-hidden="true"
      />
      <img
        className="showcase-friend showcase-friend-r"
        src={asset("assets/friend-plant-bloom.webp")}
        alt=""
        width={120}
        height={118}
        loading="lazy"
        decoding="async"
        aria-hidden="true"
      />
      <div className="wrap">
        <h2>Three screens. One habit.</h2>
      </div>
      <div
        className="showcase-stage"
        ref={stageRef}
        onPointerMove={onGroupPointerMove}
        onPointerLeave={onGroupPointerLeave}
        onKeyDown={onKey}
      >
        {carousel ? (
          <motion.div
            className="sc-track"
            drag="x"
            dragElastic={0.25}
            onDragEnd={onDragEnd}
            animate={{ x: `${slideX(selected)}%` }}
            transition={spring}
          >
            {phones}
          </motion.div>
        ) : (
          <motion.div
            className="sc-row"
            style={reduced ? undefined : { rotateX: rotX, rotateY: rotY }}
          >
            {phones}
          </motion.div>
        )}
        {dots}
      </div>
      <p className="sr-only" aria-live="polite">
        Screen {selected + 1} of {N}: {SCREENS[selected].label}
      </p>
    </section>
  );
}
