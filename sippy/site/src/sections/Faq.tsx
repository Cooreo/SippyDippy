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

/** FAQ — short, real answers. Native <details> for keyboard/screen-reader. */
export function Faq() {
  return (
    <section id="faq" className="faq">
      <h2>Questions</h2>
      <div className="faq-list">
        {FAQS.map((f) => (
          <details key={f.q} className="glass faq-item">
            <summary>{f.q}</summary>
            <p>{f.a}</p>
          </details>
        ))}
      </div>
    </section>
  );
}
