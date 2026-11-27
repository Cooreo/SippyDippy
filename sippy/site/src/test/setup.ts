/**
 * Vitest setup (happy-dom): stubs for APIs the libraries probe at runtime
 * that happy-dom doesn't implement.
 */

class ObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
  takeRecords() {
    return [];
  }
}

if (!("ResizeObserver" in globalThis)) {
  Object.defineProperty(globalThis, "ResizeObserver", {
    value: ObserverStub,
    writable: true,
  });
}
if (!("IntersectionObserver" in globalThis)) {
  Object.defineProperty(globalThis, "IntersectionObserver", {
    value: ObserverStub,
    writable: true,
  });
}

// fluidkit/lenis/motion probe matchMedia; happy-dom's may lack addEventListener.
const probe =
  typeof window !== "undefined" && typeof window.matchMedia === "function"
    ? window.matchMedia("(prefers-color-scheme: dark)")
    : null;
if (
  typeof window !== "undefined" &&
  (!probe || typeof probe.addEventListener !== "function")
) {
  Object.defineProperty(window, "matchMedia", {
    value: (media: string) => ({
      matches: false,
      media,
      onchange: null,
      addEventListener: () => {},
      removeEventListener: () => {},
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    }),
    writable: true,
  });
}
