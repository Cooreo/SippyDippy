/**
 * Sippy icon set — one rounded stroke set, 1.75px stroke, 24px grid,
 * consistent across the whole product (brief: "Icons: one rounded stroke
 * set at about 1.75px stroke, used consistently").
 */
import type { SVGProps } from "react";

type P = SVGProps<SVGSVGElement>;

function base(props: P): P {
  return {
    xmlns: "http://www.w3.org/2000/svg",
    viewBox: "0 0 24 24",
    width: 24,
    height: 24,
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.75,
    strokeLinecap: "round",
    strokeLinejoin: "round",
    "aria-hidden": true,
    ...props,
  };
}

export const IconDroplet = (p: P) => (
  <svg {...base(p)}>
    <path d="M12 3.5c-2.5 4-6 8-6 11.5a6 6 0 0 0 12 0c0-3.5-3.5-7.5-6-11.5Z" />
    <path d="M9.1 14.6c-.4 2 0 3.4 1.4 4.4" />
  </svg>
);

export const IconSun = (p: P) => (
  <svg {...base(p)}>
    <circle cx="12" cy="12" r="4" />
    <path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M18.7 5.3l-1.4 1.4M6.7 17.3l-1.4 1.4" />
  </svg>
);

export const IconMoon = (p: P) => (
  <svg {...base(p)}>
    <path d="M20 14.5A8 8 0 1 1 9.5 4 6.5 6.5 0 0 0 20 14.5Z" />
  </svg>
);

export const IconHome = (p: P) => (
  <svg {...base(p)}>
    <path d="M4 11l8-7 8 7" />
    <path d="M6 9.5V20h4.5v-5h3v5H18V9.5" />
  </svg>
);

export const IconHeart = (p: P) => (
  <svg {...base(p)}>
    <path d="M12 20s-7.5-4.6-9-9c-.9-2.7.7-5.5 3.5-6 2-.3 3.9.7 5.5 2.7 1.6-2 3.5-3 5.5-2.7 2.8.5 4.4 3.3 3.5 6-1.5 4.4-9 9-9 9Z" />
  </svg>
);

export const IconUsers = (p: P) => (
  <svg {...base(p)}>
    <circle cx="12" cy="8" r="3.5" />
    <path d="M5.5 19.5a6.5 6.5 0 0 1 13 0" />
  </svg>
);

export const IconPlus = (p: P) => (
  <svg {...base(p)}>
    <path d="M12 5v14M5 12h14" />
  </svg>
);

export const IconSparkle = (p: P) => (
  <svg {...base(p)}>
    <path d="M12 4.5l1.7 4.4 4.3 1.6-4.3 1.6L12 16.5l-1.7-4.4L6 10.5l4.3-1.6L12 4.5Z" />
    <path d="M18.5 16.5l.8 2 2 .8-2 .8-.8 2-.8-2-2-.8 2-.8.8-2Z" />
  </svg>
);

export const IconBell = (p: P) => (
  <svg {...base(p)}>
    <path d="M6.5 9.5a5.5 5.5 0 0 1 11 0c0 4.5 1.8 5.7 1.8 5.7H4.7s1.8-1.2 1.8-5.7Z" />
    <path d="M10.3 18.8a1.8 1.8 0 0 0 3.4 0" />
  </svg>
);

export const IconFlame = (p: P) => (
  <svg {...base(p)}>
    <path d="M12 3.5c2 2.6 5 5.2 5 8.7a5 5 0 0 1-10 0c0-1.8.8-3.3 1.9-4.5.2 1.3 1 2.1 2.2 2.3-.4-2.3.4-4.7.9-6.5Z" />
  </svg>
);

export const IconGear = (p: P) => (
  <svg {...base(p)}>
    <circle cx="12" cy="12" r="3.2" />
    <path d="M12 2.8v2.4M12 18.8v2.4M2.8 12h2.4M18.8 12h2.4M5.5 5.5l1.7 1.7M16.8 16.8l1.7 1.7M18.5 5.5l-1.7 1.7M7.2 16.8l-1.7 1.7" />
  </svg>
);

export const IconPlay = (p: P) => (
  <svg {...base(p)}>
    <path d="M8.5 5.5l9 6.5-9 6.5v-13Z" />
  </svg>
);

export const IconPause = (p: P) => (
  <svg {...base(p)}>
    <path d="M9 5.5v13M15 5.5v13" />
  </svg>
);

export const IconPrev = (p: P) => (
  <svg {...base(p)}>
    <path d="M17.5 5.5L9 12l8.5 6.5v-13Z" />
    <path d="M6.5 5.5v13" />
  </svg>
);

export const IconNext = (p: P) => (
  <svg {...base(p)}>
    <path d="M6.5 5.5L15 12l-8.5 6.5v-13Z" />
    <path d="M17.5 5.5v13" />
  </svg>
);

export const IconMusic = (p: P) => (
  <svg {...base(p)}>
    <circle cx="7" cy="17" r="2.5" />
    <circle cx="17" cy="15" r="2.5" />
    <path d="M9.5 17V6.8L19.5 4.5V15" />
  </svg>
);
