import { useId, useState } from "react";
import { BentoCard } from "../components/BentoCard";
import { IconDroplet, IconHeart, IconSparkle } from "../components/Icons";
import { asset } from "../lib/router";

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
  smallFile: string;
  smallW: number;
  fullW: number;
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
      {
        mood: "Happy",
        file: "assets/dippy-happy.webp",
        smallFile: "assets/dippy-happy-128.webp",
        smallW: 128,
        fullW: 248,
        w: 64,
        h: 64,
      },
      {
        mood: "Dancing",
        file: "assets/dippy-dancing.webp",
        smallFile: "assets/dippy-dancing-128.webp",
        smallW: 128,
        fullW: 230,
        w: 60,
        h: 65,
      },
      {
        mood: "Waving",
        file: "assets/dippy-waving.webp",
        smallFile: "assets/dippy-waving-128.webp",
        smallW: 128,
        fullW: 227,
        w: 59,
        h: 64,
      },
      {
        mood: "Sleepy",
        file: "assets/dippy-sleepy.webp",
        smallFile: "assets/dippy-sleepy-120.webp",
        smallW: 120,
        fullW: 207,
        w: 52,
        h: 65,
      },
    ],
  },
  {
    id: "plant",
    name: "Plant",
    moods: [
      {
        mood: "Sprout",
        file: "assets/friend-plant-sprout.webp",
        smallFile: "assets/friend-plant-sprout-112.webp",
        smallW: 112,
        fullW: 156,
        w: 56,
        h: 64,
      },
      {
        mood: "Bloom",
        file: "assets/friend-plant-bloom.webp",
        smallFile: "assets/friend-plant-bloom-128.webp",
        smallW: 128,
        fullW: 248,
        w: 64,
        h: 63,
      },
    ],
  },
  {
    id: "cat",
    name: "Cat",
    moods: [
      {
        mood: "Playful",
        file: "assets/friend-pet-cub.webp",
        smallFile: "assets/friend-pet-cub-140.webp",
        smallW: 140,
        fullW: 281,
        w: 68,
        h: 51,
      },
      {
        mood: "Grown",
        file: "assets/friend-pet-adult.webp",
        smallFile: "assets/friend-pet-adult-104.webp",
        smallW: 104,
        fullW: 177,
        w: 48,
        h: 65,
      },
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
                            srcSet={`${asset(current.smallFile)} ${current.smallW}w, ${asset(current.file)} ${current.fullW}w`}
                            sizes="68px"
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
