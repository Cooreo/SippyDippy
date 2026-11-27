import { useFxMode } from "../lib/fx";

/**
 * RefractionDefs — the SVG displacement filter referenced by GlassSurface's
 * backdrop-filter (`url(#sippyRefraction)`). Progressive enhancement:
 * Chromium honours filter references inside backdrop-filter; Safari/Firefox
 * ignore it and keep the clean blur-only fallback. Unmounted on the low
 * quality tier and under `?fx=blur` so mobile GPUs never evaluate SVG
 * `feTurbulence` + `feDisplacementMap`.
 */
export function RefractionDefs() {
  const fx = useFxMode();
  if (fx !== "full") return null;
  return (
    <svg
      width="0"
      height="0"
      style={{ position: "absolute" }}
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <filter
          id="sippyRefraction"
          x="-20%"
          y="-20%"
          width="140%"
          height="140%"
          colorInterpolationFilters="sRGB"
        >
          <feTurbulence
            type="fractalNoise"
            baseFrequency="0.012 0.02"
            numOctaves="2"
            seed="7"
            result="noise"
          />
          <feDisplacementMap
            in="SourceGraphic"
            in2="noise"
            scale="12"
            xChannelSelector="R"
            yChannelSelector="G"
          />
        </filter>
      </defs>
    </svg>
  );
}
