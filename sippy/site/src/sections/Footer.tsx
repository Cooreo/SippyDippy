import { IconDroplet } from "../components/Icons";
import { asset } from "../lib/router";

export function Footer() {
  const base = import.meta.env.BASE_URL;
  return (
    <footer className="footer">
      <img
        className="footer-ripple"
        src={asset("assets/ripple-ring.webp")}
        alt=""
        width={240}
        height={138}
        loading="lazy"
        decoding="async"
        aria-hidden="true"
      />
      <div className="footer-brand">
        <IconDroplet width={20} height={20} />
        Sippy
      </div>
      <p>
        Liquid glass, shared tokens, no Play Store. Your data stays on your
        phone.
      </p>
      <nav className="footer-links" aria-label="Footer">
        <a href="#hero">Top</a>
        <a href="#features">Features</a>
        <a href="#how">How it works</a>
        <a href="#download">Download</a>
        <a href={`${base}#/gallery`}>Component gallery (dev)</a>
      </nav>
    </footer>
  );
}
