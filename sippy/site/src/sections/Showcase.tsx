import { useId, useState } from "react";
import { BentoCard } from "../components/BentoCard";
import { IconDroplet, IconHeart, IconSparkle } from "../components/Icons";
import { asset } from "../lib/router";

/**
 * Showcase — "Three things Sippy does best."
 *
 * Replaces the phone showcase completely. Three interactive feature cards
 * built on the shared BentoCard (cursor-following specular, ±6° spring tilt,
 * 4px hover lift, 0.98 press scale, mascot parallax; touch = press only;
 * reduced motion = static highlight only).
 *
 * Backdrop-filter budget: each card is one GlassSurface (3 total here + 1 nav
 * pill = 4 backdrop-filter surfaces in the viewport, within the 6-surface cap).
 *
 * Note on drink data: sippy/app does not exist yet (Phase 4), so Card 1
 * displays volumes only and labels the total "ml logged" (never "hydration",
 * zero invented stats).
 */

interface DrinkOption {
  id: string;
  name: string;
  ml: number;
}

const DRINKS: DrinkOption[] = [
  { id: "water", name: "Water", ml: 250 },
  { id: "tea", name: "Tea", ml: 200 },
  { id: "coffee", name: "Coffee", ml: 150 },
  { id: "juice", name: "Juice", ml: 200 },
];

interface FriendMood {
  mood: string;
  file: string;
  w: number;
  h: number;
}

interface FriendDef {
  id: string;
  name: string;
  moods: FriendMood[];
}

const FRIENDS: FriendDef[] = [
  {
    id: "dippy",
    name: "Dippy",
    moods: [
      { mood: "Happy", file: "assets/dippy-happy.webp", w: 64, h: 64 },
      { mood: "Dancing", file: "assets/dippy-dancing.webp", w: 60, h: 65 },
      { mood: "Waving", file: "assets/dippy-waving.webp", w: 59, h: 64 },
      { mood: "Sleepy", file: "assets/dippy-sleepy.webp", w: 52, h: 65 },
    ],
  },
  {
    id: "plant",
    name: "Plant",
    moods: [
      { mood: "Sprout", file: "assets/friend-plant-sprout.webp", w: 56, h: 64 },
      { mood: "Bloom", file: "assets/friend-plant-bloom.webp", w: 64, h: 63 },
    ],
  },
  {
    id: "cat",
    name: "Cat",
    moods: [
      { mood: "Playful", file: "assets/friend-pet-cub.webp", w: 68, h: 51 },
      { mood: "Grown", file: "assets/friend-pet-adult.webp", w: 48, h: 65 },
    ],
  },
];

const MIN_GOAL_ML = 750;
const MAX_GOAL_ML = 3000;
const DEFAULT_GOAL_ML = 2000;

export function Showcase() {
  const [totalMl, setTotalMl] = useState(0);
  const [moodIdx, setMoodIdx] = useState<Record<string, number>>({
    dippy: 0,
    plant: 0,
    cat: 0,
  });
  const [goalMl, setGoalMl] = useState(DEFAULT_GOAL_ML);
  const sliderId = useId();

  const cycleMood = (friend: FriendDef) => {
    setMoodIdx((prev) => ({
      ...prev,
      [friend.id]: ((prev[friend.id] ?? 0) + 1) % friend.moods.length,
    }));
  };

  const goalFraction =
    0.14 + 0.86 * ((goalMl - MIN_GOAL_ML) / (MAX_GOAL_ML - MIN_GOAL_ML));

  return (
    <section id="showcase" className="showcase">
      <div className="showcase-wrap">
        <h2>Three things Sippy does best.</h2>
        <div className="sc-grid">
          {/* Card 1 — Every drink counts */}
          <BentoCard className="sc-card">
            <span className="bento-icon" aria-hidden="true">
              <IconDroplet />
            </span>
            <h3>Every drink counts</h3>
            <p>
              Tea, coffee, juice, the occasional beer. Sippy counts the whole
              pour.
            </p>
            <div className="sc-widget">
              <div className="sc-drinks" role="group" aria-label="Log a drink">
                {DRINKS.map((d) => (
                  <button
                    key={d.id}
                    type="button"
                    className="sc-drink-btn"
                    onClick={() => setTotalMl((m) => m + d.ml)}
                  >
                    <span className="sc-drink-name">{d.name}</span>
                    <span className="sc-drink-ml">+{d.ml} ml</span>
                  </button>
                ))}
              </div>
              <div className="sc-total-row">
                <output className="sc-total" aria-live="polite">
                  <span className="sc-total-num">
                    {totalMl.toLocaleString("en-US")}
                  </span>{" "}
                  <span className="sc-total-unit">ml logged</span>
                </output>
                {totalMl > 0 && (
                  <button
                    type="button"
                    className="sc-reset-btn"
                    onClick={() => setTotalMl(0)}
                  >
                    Clear
                  </button>
                )}
              </div>
            </div>
          </BentoCard>

          {/* Card 2 — Friends grow with you */}
          <BentoCard className="sc-card">
            <span className="bento-icon" aria-hidden="true">
              <IconHeart />
            </span>
            <h3>Friends grow with you</h3>
            <p>
              Dippy and friends react when you sip and grow when you keep
              showing up.
            </p>
            <div className="sc-widget">
              <div
                className="sc-friends"
                role="group"
                aria-label="Friends — select to cycle mood"
              >
                {FRIENDS.map((f) => {
                  const idx = moodIdx[f.id] ?? 0;
                  const current = f.moods[idx];
                  return (
                    <button
                      key={f.id}
                      type="button"
                      className="sc-friend"
                      onClick={() => cycleMood(f)}
                      aria-label={`${f.name}: ${current.mood}. Activate to cycle mood`}
                    >
                      <span className="sc-friend-parallax">
                        <span className="sc-friend-bounce">
                          <img
                            className="sc-friend-art"
                            src={asset(current.file)}
                            alt=""
                            width={current.w}
                            height={current.h}
                            loading="lazy"
                            decoding="async"
                          />
                        </span>
                      </span>
                      <span className="sc-friend-name">{f.name}</span>
                      <span className="sc-friend-mood">{current.mood}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          </BentoCard>

          {/* Card 3 — Your goal, your rules */}
          <BentoCard className="sc-card">
            <span className="bento-icon" aria-hidden="true">
              <IconSparkle />
            </span>
            <h3>Your goal, your rules</h3>
            <p>Set your daily minimum anywhere from 750 ml to 3 litres.</p>
            <div className="sc-widget sc-goal-widget">
              <div className="sc-goal-readout">
                <output
                  htmlFor={sliderId}
                  className="sc-goal-num"
                  aria-live="polite"
                >
                  {goalMl.toLocaleString("en-US")}{" "}
                  <span className="sc-goal-unit">ml</span>
                  <span className="sr-only"> daily minimum</span>
                </output>
              </div>
              <div className="sc-goal-bar" aria-hidden="true">
                <div
                  className="sc-goal-bar-fill"
                  style={{ transform: `scaleX(${goalFraction.toFixed(4)})` }}
                />
              </div>
              <label htmlFor={sliderId} className="sr-only">
                Daily minimum in millilitres ({goalMl} ml)
              </label>
              <input
                id={sliderId}
                type="range"
                className="sc-goal-slider"
                min={MIN_GOAL_ML}
                max={MAX_GOAL_ML}
                step={50}
                value={goalMl}
                aria-valuetext={`${goalMl} millilitres daily minimum`}
                onChange={(e) => setGoalMl(Number(e.target.value))}
              />
              <div className="sc-goal-ends" aria-hidden="true">
                <span>750 ml</span>
                <span>3,000 ml</span>
              </div>
            </div>
          </BentoCard>
        </div>
      </div>
    </section>
  );
}
