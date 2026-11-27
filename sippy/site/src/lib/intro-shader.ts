/**
 * Surface-break intro — the one WebGL fragment shader (round 2).
 *
 * Renders the ENTIRE underwater scene: deep-water gradient + light ramp,
 * caustics sampled from the SAME hero-caustics asset the page backdrop
 * uses (or a procedural caustic if the texture is unavailable), sweeping
 * light shafts, a two-layer bubble field, the four gooey droplets that
 * merge into the mark, the mark's stroke-draw, the surface-break ring
 * (refracting the SCENE ONLY — the page underneath is a separate DOM
 * layer and can never be refracted by the shader), and the reveal itself
 * as an alpha hole centred on the mark.
 *
 * DPR is capped at 1.5 by the caller; the context is released by dispose().
 */

import { DARKEST_BACKDROP_RGB, type Rgb01 } from "./intro2";

const VERT = `
attribute vec2 a_pos;
void main() {
  gl_Position = vec4(a_pos, 0.0, 1.0);
}
`;

const FRAG = `
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform vec2  u_res;      // canvas size in DEVICE px (drawing buffer)
uniform float u_time;     // seconds into the intro
uniform float u_revealR;  // reveal hole radius in DEVICE px (0 = closed)
uniform vec2  u_mark;     // mark centre in uv (0.5, 0.5)
uniform vec3  u_base;     // darkest page backdrop tone (#060a14, never #000)
uniform sampler2D u_tex;  // hero caustics texture
uniform float u_hasTex;

float hash21(vec2 p) {
  p = fract(p * vec2(234.34, 435.345));
  p += dot(p, p + 34.23);
  return fract(p.x * p.y);
}
vec2 hash22(vec2 p) {
  float n = hash21(p);
  return vec2(n, hash21(p + n + 17.17));
}
float sdSegment(vec2 p, vec2 a, vec2 b) {
  vec2 pa = p - a, ba = b - a;
  float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
  return length(pa - ba * h);
}
/* teardrop in mark units (box [-0.5..0.5], +y down) — matches IconDroplet */
float sdMark(vec2 p) {
  float dC = length(p - vec2(0.0, 0.145)) - 0.25;
  float d1 = sdSegment(p, vec2(0.0, -0.292), vec2(-0.24, 0.12));
  float d2 = sdSegment(p, vec2(0.0, -0.292), vec2(0.24, 0.12));
  return min(dC, min(d1, d2));
}

/* — the underwater scene (q: centred, units of viewport height) — */
vec3 scene(vec2 q, float t) {
  float light = smoothstep(0.4, 1.6, t);
  float flick = t < 0.4
    ? 0.55 + 0.45 * abs(sin(t * 31.0) * sin(t * 13.7 + 1.2))
    : 1.0;
  float sc = mix(1.12, 1.0, smoothstep(0.4, 1.6, t)); // rise: 1.12 -> 1.0
  vec2 uv = (q / 2.0) / sc + 0.5;

  // deep water: darkest frame is u_base (#060a14, never #000) -> lit
  vec3 deep = mix(u_base, vec3(0.045, 0.11, 0.26),
                  smoothstep(0.0, 1.0, uv.y));
  deep = mix(deep, deep * 1.7 + vec3(0.015, 0.045, 0.10), light * 0.8);

  // caustics — same asset as the hero backdrop, same cover-ish framing
  vec3 caust;
  if (u_hasTex > 0.5) {
    caust = texture2D(u_tex, uv * vec2(1.0, 1.3) + vec2(0.0, -t * 0.012)).rgb;
  } else {
    float c1 = sin((uv.x * 7.0 + t * 0.22) * 3.14159)
             * sin((uv.y * 5.0 - t * 0.18) * 3.14159);
    float c2 = sin((uv.x - uv.y) * 9.0 + t * 0.3);
    caust = vec3(0.3, 0.55, 1.0) * smoothstep(0.2, 1.0, c1 * 0.6 + c2 * 0.4);
  }
  vec3 col = deep + caust * (0.22 + 0.78 * light) * flick * 0.5;

  // light shafts (sweep in with the rise)
  float shafts = 0.0;
  for (int i = 0; i < 3; i++) {
    float fi = float(i);
    float x = uv.x * 0.9 + fi * 0.37 + uv.y * 0.28 + t * 0.02 * (1.0 + fi * 0.5);
    shafts += smoothstep(0.12, 0.0, abs(fract(x) - 0.5));
  }
  col += vec3(0.35, 0.55, 0.95) * shafts * 0.16 * light;

  // bubbles — two layers rising
  float bub = 0.0;
  {
    vec2 g1 = vec2(uv.x * 10.0, (uv.y - t * 0.10) * 6.0);
    vec2 id1 = floor(g1);
    vec2 f1 = fract(g1) - 0.5;
    vec2 r1 = hash22(id1);
    bub += smoothstep(0.16, 0.06, length(f1 - (r1 - 0.5) * 0.8))
          * (0.4 + 0.6 * r1.x);
    vec2 g2 = vec2(uv.x * 5.0 + 7.0, (uv.y - t * 0.16) * 3.0);
    vec2 id2 = floor(g2);
    vec2 f2 = fract(g2) - 0.5;
    vec2 r2 = hash22(id2 + 31.7);
    bub += smoothstep(0.2, 0.08, length(f2 - (r2 - 0.5) * 0.8))
          * (0.4 + 0.6 * r2.x) * 0.8;
  }
  // first 0.4 s: bubbles only at the bottom edge
  float gate = t < 0.4 ? smoothstep(0.45, 0.0, 1.0 - uv.y) : 1.0;
  col += vec3(0.7, 0.85, 1.0) * bub * 0.32 * (0.25 + 0.75 * light) * gate;

  return col;
}

void main() {
  float t = u_time;
  vec2 uv = gl_FragCoord.xy / u_res;
  vec2 q = (uv - 0.5) * vec2(u_res.x / u_res.y, 1.0);
  float d0 = length(q);

  // surface break (2.5-3.0): ring + SCENE-ONLY refraction around it
  float ringT = smoothstep(2.5, 2.55, t) * (1.0 - smoothstep(2.9, 3.0, t));
  float ringR = mix(0.03, 1.05, smoothstep(2.5, 3.0, t));
  float band = abs(d0 - ringR);
  vec2 refr = (d0 > 0.0001 ? q / d0 : vec2(0.0))
            * smoothstep(0.18, 0.0, band) * 0.045 * ringT;

  vec3 col = scene(q - refr, t);

  // four glossy droplets merging into the mark (1.6-2.5), gooey field
  float mergeP = smoothstep(1.6, 2.45, t);
  float field = 0.0;
  for (int i = 0; i < 4; i++) {
    float fi = float(i);
    float a = 0.7854 + fi * 1.5708;
    vec2 dir = vec2(cos(a), sin(a));
    vec2 sp = mix(dir * 1.2, dir * 0.05, mergeP);
    field += exp(-dot(q - sp, q - sp) * 260.0);
  }
  float mask = smoothstep(0.45, 0.85, field)
             * (1.0 - smoothstep(2.45, 2.58, t)); // DOM mark takes over
  if (mask > 0.001) {
    float rim = smoothstep(0.75, 1.0, field) - smoothstep(0.35, 0.6, field);
    vec3 dropCol = vec3(0.3, 0.55, 1.0) * (0.5 + 0.5 * smoothstep(0.4, 1.6, t));
    col = mix(col, dropCol, mask * 0.92);
    col += vec3(0.85, 0.92, 1.0) * rim * 0.85 * (0.4 + 0.6 * smoothstep(0.4, 1.6, t));
  }

  // mark stroke-draw as it forms (1.85-2.5)
  float markVis = smoothstep(1.85, 2.0, t) * (1.0 - smoothstep(2.45, 2.6, t));
  if (markVis > 0.001) {
    // mark units (132px box). q's +y is UP in GL space, the mark is
    // authored +y DOWN (IconDroplet) — flip y so the tip points up.
    vec2 pm = vec2(q.x, -q.y) * (u_res.y / 132.0);
    float ang = atan(pm.y, pm.x) / 6.28318 + 0.5;
    float draw = smoothstep(1.85, 2.5, t);
    float reveal = smoothstep(draw - 0.5, draw, ang);
    float stroke = smoothstep(0.012, 0.0, abs(sdMark(pm))) * reveal;
    col += vec3(0.55, 0.75, 1.0) * stroke * markVis
         * (0.55 + 0.6 * smoothstep(0.4, 1.6, t));
  }

  // the bright ring
  float ring = smoothstep(0.05, 0.0, band) * ringT;
  col = mix(col, vec3(0.85, 0.93, 1.0), ring * 0.85);
  col += vec3(0.4, 0.6, 1.0) * smoothstep(0.3, 0.0, band) * 0.12 * ringT;

  // reveal: alpha hole centred on the mark (the page shows through)
  float r = u_revealR / u_res.y;
  float aa = 2.0 / u_res.y;
  float hole = r <= 0.001 ? 0.0 : 1.0 - smoothstep(r - aa, r + aa, d0);
  float alpha = 1.0 - hole;

  gl_FragColor = vec4(col * alpha, alpha);
}
`;

export interface IntroGL {
  render(t: number, revealRpx: number): void;
  resize(cssW: number, cssH: number, dpr: number): void;
  setTexture(src: TexImageSource): void;
  dispose(): void;
}

/**
 * Create the intro GL context; returns null when WebGL is unavailable.
 * Clears to the page's darkest backdrop tone at alpha = 1 (never transparent
 * black `(0, 0, 0, 0)` and never `#000`).
 */
export function createIntroGL(
  canvas: HTMLCanvasElement,
  clearTone: Rgb01 = DARKEST_BACKDROP_RGB,
): IntroGL | null {
  const gl = canvas.getContext("webgl", {
    premultipliedAlpha: true,
    antialias: false,
    powerPreference: "high-performance",
  });
  if (!gl) return null;

  gl.clearColor(clearTone.r, clearTone.g, clearTone.b, 1.0);
  gl.clear(gl.COLOR_BUFFER_BIT);

  const compile = (type: number, src: string): WebGLShader | null => {
    const sh = gl.createShader(type);
    if (!sh) return null;
    gl.shaderSource(sh, src);
    gl.compileShader(sh);
    if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) {
      // fall back cleanly — the CSS path plays instead
      gl.deleteShader(sh);
      return null;
    }
    return sh;
  };

  const vs = compile(gl.VERTEX_SHADER, VERT);
  const fs = compile(gl.FRAGMENT_SHADER, FRAG);
  if (!vs || !fs) return null;
  const prog = gl.createProgram();
  if (!prog) return null;
  gl.attachShader(prog, vs);
  gl.attachShader(prog, fs);
  gl.linkProgram(prog);
  if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) {
    gl.deleteProgram(prog);
    return null;
  }
  gl.useProgram(prog);

  // fullscreen triangle
  const buf = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, buf);
  gl.bufferData(
    gl.ARRAY_BUFFER,
    new Float32Array([-1, -1, 3, -1, -1, 3]),
    gl.STATIC_DRAW,
  );
  const loc = gl.getAttribLocation(prog, "a_pos");
  gl.enableVertexAttribArray(loc);
  gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);

  const u = {
    res: gl.getUniformLocation(prog, "u_res"),
    time: gl.getUniformLocation(prog, "u_time"),
    revealR: gl.getUniformLocation(prog, "u_revealR"),
    mark: gl.getUniformLocation(prog, "u_mark"),
    base: gl.getUniformLocation(prog, "u_base"),
    tex: gl.getUniformLocation(prog, "u_tex"),
    hasTex: gl.getUniformLocation(prog, "u_hasTex"),
  };

  const tex = gl.createTexture();
  let hasTex = false;
  gl.activeTexture(gl.TEXTURE0);
  gl.bindTexture(gl.TEXTURE_2D, tex);
  gl.uniform1i(u.tex, 0);

  let disposed = false;

  return {
    render(t, revealRpx) {
      if (disposed) return;
      if (revealRpx <= 0) {
        gl.clearColor(clearTone.r, clearTone.g, clearTone.b, 1.0);
        gl.clear(gl.COLOR_BUFFER_BIT);
      }
      gl.uniform2f(u.res, gl.drawingBufferWidth, gl.drawingBufferHeight);
      gl.uniform1f(u.time, t);
      gl.uniform1f(u.revealR, revealRpx);
      gl.uniform2f(u.mark, 0.5, 0.5);
      gl.uniform3f(u.base, clearTone.r, clearTone.g, clearTone.b);
      gl.uniform1f(u.hasTex, hasTex ? 1 : 0);
      gl.drawArrays(gl.TRIANGLES, 0, 3);
    },
    resize(cssW, cssH, dpr) {
      if (disposed) return;
      const w = Math.max(1, Math.floor(cssW * dpr));
      const h = Math.max(1, Math.floor(cssH * dpr));
      if (canvas.width !== w || canvas.height !== h) {
        canvas.width = w;
        canvas.height = h;
      }
      gl.viewport(0, 0, w, h);
    },
    setTexture(src) {
      if (disposed) return;
      gl.bindTexture(gl.TEXTURE_2D, tex);
      try {
        gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, false);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, src);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
        hasTex = true;
      } catch {
        hasTex = false;
      }
    },
    dispose() {
      if (disposed) return;
      disposed = true;
      gl.deleteTexture(tex);
      gl.deleteBuffer(buf);
      gl.deleteProgram(prog);
      gl.deleteShader(vs);
      gl.deleteShader(fs);
      const lose = gl.getExtension("WEBGL_lose_context");
      if (lose) lose.loseContext();
    },
  };
}
