import { describe, expect, it } from "vitest";
import { asset, resolveRoute } from "./router";

describe("resolveRoute", () => {
  it("maps /gallery (and trailing slash) to the gallery", () => {
    expect(resolveRoute("/gallery", "")).toBe("/gallery");
    expect(resolveRoute("/gallery/", "")).toBe("/gallery");
  });
  it("supports the hash form for static hosts without SPA rewrites", () => {
    expect(resolveRoute("/", "#/gallery")).toBe("/gallery");
    expect(resolveRoute("/features", "#/gallery")).toBe("/gallery");
    expect(resolveRoute("/", "#/assets")).toBe("/assets");
  });
  it("everything else is the home route", () => {
    expect(resolveRoute("/", "")).toBe("/");
    expect(resolveRoute("/features", "")).toBe("/");
    expect(resolveRoute("", "")).toBe("/");
    expect(resolveRoute("/", "#gallery")).toBe("/");
  });
});

describe("asset", () => {
  it("joins the base URL to public/ assets", () => {
    expect(asset("backdrops/blobs-dark.jpg")).toBe("/backdrops/blobs-dark.jpg");
  });
  it("strips leading slashes from the path", () => {
    expect(asset("/backdrops/blobs-dark.jpg")).toBe("/backdrops/blobs-dark.jpg");
  });
});
