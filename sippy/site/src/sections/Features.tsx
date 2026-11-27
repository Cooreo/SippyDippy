import { BentoCard } from "../components/BentoCard";
import { SectionArt } from "../components/SectionArt";
import {
  IconDroplet,
  IconFlame,
  IconGear,
  IconHeart,
  IconBell,
} from "../components/Icons";
import { asset } from "../lib/router";

const STREAK_MILESTONES = [
  { d: "3", done: true },
  { d: "7", done: true },
  { d: "14", done: true },
  { d: "30", done: false },
  { d: "100", done: false },
];

/**
 * Features — bento grid of glass cards (cursor specular + pointer tilt via
 * BentoCard). The 3 core features get the biggest cards; every card carries
 * an illustration or a Dippy state from the generated pack.
 */
export function Features() {
  return (
    <section id="features" className="features">
      <SectionArt name="bg-features" />
      <h2>Built around how you actually drink.</h2>
      <p className="hint">
        Not just a water counter. Sippy counts the full pour — tea, coffee,
        juice, the occasional beer — with what each one really hydrates.
      </p>
      <div className="bento-grid">
        <BentoCard className="bento-span-7">
          <span className="bento-icon">
            <IconDroplet />
          </span>
          <h3>Every drink you actually drink</h3>
          <p>
            Water, teas, coffees, milks, juice, smoothies, kombucha — 24+
            drinks, each with real hydration values. Your goal counts what
            your body actually gets, not what you wish it was.
          </p>
          <img
            className="bento-art"
            src={asset("assets/droplet-sprite.webp")}
            alt="A small glossy water droplet"
            width={64}
            height={83}
            loading="lazy"
            decoding="async"
          />
        </BentoCard>

        <BentoCard className="bento-span-5">
          <span className="bento-icon">
            <IconHeart />
          </span>
          <h3>Friends who care</h3>
          <p>
            Dippy and friends — pets or plants, your names — react when you
            sip, hit your goal, or overdo the sugary stuff.
          </p>
          <img
            className="bento-art"
            src={asset("assets/dippy-happy.webp")}
            alt="Dippy the droplet friend, looking happy"
            width={84}
            height={84}
            loading="lazy"
            decoding="async"
          />
        </BentoCard>

        <BentoCard className="bento-span-12">
          <span className="bento-icon">
            <IconFlame />
          </span>
          <h3>Streaks &amp; reminders</h3>
          <p>
            A streak for every day you hit your minimum, milestone
            celebrations along the way, and local reminders on a schedule
            you set. No cloud, no account, no nagging.
          </p>
          <div className="bento-milestones" aria-label="Streak milestones">
            <IconBell width={16} height={16} />
            {STREAK_MILESTONES.map((m) => (
              <span key={m.d} className={`milestone${m.done ? " milestone-done" : ""}`}>
                {m.d}
              </span>
            ))}
          </div>
          <img
            className="bento-art bento-art-lg"
            src={asset("assets/dippy-dancing.webp")}
            alt="Dippy dancing — goal reached"
            width={104}
            height={113}
            loading="lazy"
            decoding="async"
          />
        </BentoCard>

        <BentoCard className="bento-span-4">
          <span className="bento-icon">
            <IconDroplet />
          </span>
          <h3>Your goal</h3>
          <p>
            Set your own daily drinking minimum. 750 ml or 3 litres — it’s
            your body.
          </p>
          <img
            className="bento-art"
            src={asset("assets/dippy-sleepy.webp")}
            alt="Dippy looking sleepy and thirsty"
            width={72}
            height={91}
            loading="lazy"
            decoding="async"
          />
        </BentoCard>

        <BentoCard className="bento-span-4">
          <span className="bento-icon">
            <IconHeart />
          </span>
          <h3>Your look</h3>
          <p>
            Dark or light, and an accent that’s yours. The glass bends either
            way.
          </p>
          <img
            className="bento-art"
            src={asset("assets/friend-plant-bloom.webp")}
            alt="A glossy plant friend with a violet bloom"
            width={84}
            height={83}
            loading="lazy"
            decoding="async"
          />
        </BentoCard>

        <BentoCard className="bento-span-4">
          <span className="bento-icon">
            <IconGear />
          </span>
          <h3>Developer options</h3>
          <p>
            A screen in Settings that can trigger every state and animation.
            Built so nothing ever hides.
          </p>
          <img
            className="bento-art"
            src={asset("assets/friend-pet-cub.webp")}
            alt="A chubby glossy pet friend"
            width={84}
            height={63}
            loading="lazy"
            decoding="async"
          />
        </BentoCard>
      </div>
    </section>
  );
}
