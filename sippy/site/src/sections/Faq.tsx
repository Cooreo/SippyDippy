import { useState } from "react";

const FAQS = [
  {
    q: "Do I need an account or internet?",
    a: "No. Sippy is fully local — drinks, goals, friends, streaks and reminders all live on your device.",
  },
  {
    q: "Which Android versions work?",
    a: "Android 8.0 (API 26) and up. The glass effects are deepest on Android 13+, with clean frosted fallbacks on older devices.",
  },
  {
    q: "What counts toward my goal?",
    a: "Every drink has a real hydration value — water counts 100%, teas and coffees mostly, sugary drinks less. Juice, smoothies, kombucha and even beer each have their own.",
  },
  {
    q: "How do reminders work?",
    a: "Local notifications on a schedule you set — a time window and an interval. Nothing phones home, and they stay quiet outside your window.",
  },
  {
    q: "Why a developer options screen?",
    a: "Because nothing should hide. One screen in Settings can trigger every friend state, growth stage and streak milestone — handy for screenshots, honest by design.",
  },
];

/**
 * FAQ (Phase 3 fix D) — same centered 760px container as the download card.
 * Rows: neutral glass tint (no saturated blue/purple fill); the accent
 * appears only on the border and the chevron when a row is open; the
 * plus/minus chevron is a visible 10px icon; height animates smoothly both
 * ways via the grid-template-rows 0fr↔1fr technique (button-based, so close
 * animates too — native <details> cannot animate closing).
 */
export function Faq() {
  const [open, setOpen] = useState<number | null>(0);

  return (
    <section id="faq" className="faq">
      <div className="narrow">
        <h2>Questions</h2>
        <div className="faq-list">
          {FAQS.map((f, i) => {
            const isOpen = open === i;
            return (
              <div key={f.q} className="faq-item" data-open={isOpen}>
                <button
                  type="button"
                  id={`faq-q-${i}`}
                  className="faq-q"
                  aria-expanded={isOpen}
                  aria-controls={`faq-a-${i}`}
                  onClick={() => setOpen(isOpen ? null : i)}
                >
                  <span>{f.q}</span>
                  <span className="faq-icon" aria-hidden="true" />
                </button>
                <div
                  id={`faq-a-${i}`}
                  className="faq-a"
                  role="region"
                  aria-labelledby={`faq-q-${i}`}
                >
                  <div className="faq-a-inner">
                    <p>{f.a}</p>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
