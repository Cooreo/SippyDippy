/**
 * RefractionDefs — the SVG displacement filter referenced by GlassSurface's
 * backdrop-filter (`url(#sippyRefraction)`). Progressive enhancement:
 * Chromium honours filter references inside backdrop-filter; Safari/Firefox
 * ignore it and keep the clean blur-only fallback. Also never applied when
 * ?fx=blur (see lib/fx.ts).
 */
export function RefractionDefs() {
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
