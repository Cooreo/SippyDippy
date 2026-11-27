/**
 * Minimal router — two routes, zero dependencies.
 * Supports the path form `/gallery` (dev server + any host with SPA rewrites)
 * and the hash form `/#/gallery` (works on every static host, no config).
 */
import { useEffect, useState } from "react";

export type Route = "/" | "/gallery";

export function resolveRoute(pathname: string, hash: string): Route {
  const p = pathname.replace(/\/+$/, "") || "/";
  if (p === "/gallery" || hash === "#/gallery") return "/gallery";
  return "/";
}

export function useRoute(): Route {
  const [route, setRoute] = useState<Route>(() =>
    resolveRoute(window.location.pathname, window.location.hash),
  );

  useEffect(() => {
    const update = () =>
      setRoute(resolveRoute(window.location.pathname, window.location.hash));
    window.addEventListener("popstate", update);
    window.addEventListener("hashchange", update);
    return () => {
      window.removeEventListener("popstate", update);
      window.removeEventListener("hashchange", update);
    };
  }, []);

  return route;
}

/**
 * Base-relative public asset URL (GitHub Pages subpath safety).
 * Use for any runtime reference to a file in `public/` — Vite rewrites
 * HTML/CSS/import references, but NOT string literals in JS.
 */
export function asset(path: string): string {
  const p = path.replace(/^\/+/, "");
  const base = import.meta.env.BASE_URL;
  return base.endsWith("/") ? `${base}${p}` : `${base}/${p}`;
}

export function navigate(route: Route): void {
  // Always use the hash form for the gallery: it works in dev AND under any
  // GitHub Pages subpath (path form only works in dev / SPA-rewrite hosts).
  const base = import.meta.env.BASE_URL;
  window.location.href = route === "/gallery" ? `${base}#/gallery` : base;
}
