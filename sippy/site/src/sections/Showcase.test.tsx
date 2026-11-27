import { afterEach, describe, expect, it } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { Showcase } from "./Showcase";

afterEach(cleanup);

describe("Showcase — Three things Sippy does best", () => {
  it("renders heading without subline and keeps backdrop-filter surfaces <= 3 in section", () => {
    const { container } = render(<Showcase />);
    expect(
      screen.getByRole("heading", {
        level: 2,
        name: "Three things Sippy does best.",
      }),
    ).not.toBeNull();
    expect(container.querySelectorAll(".glass").length).toBe(3);
    expect(container.textContent?.toLowerCase()).not.toContain("hydration");
  });

  it("Card 1 logs tapped drinks and updates 'ml logged' total", () => {
    render(<Showcase />);
    expect(screen.getByText("0")).not.toBeNull();
    expect(screen.getByText("ml logged")).not.toBeNull();

    fireEvent.click(screen.getByRole("button", { name: /Water/i }));
    expect(screen.getByText("250")).not.toBeNull();

    fireEvent.click(screen.getByRole("button", { name: /Tea/i }));
    expect(screen.getByText("450")).not.toBeNull();

    fireEvent.click(screen.getByRole("button", { name: /Coffee/i }));
    expect(screen.getByText("600")).not.toBeNull();

    fireEvent.click(screen.getByRole("button", { name: /Juice/i }));
    expect(screen.getByText("800")).not.toBeNull();
  });

  it("Card 2 cycles Dippy, Plant and Cat moods using existing mascot art", () => {
    render(<Showcase />);
    const dippyBtn = screen.getByRole("button", { name: /Dippy: Happy/i });
    const dippyImg = dippyBtn.querySelector("img") as HTMLImageElement;
    expect(dippyImg.getAttribute("src")).toContain("dippy-happy.webp");

    fireEvent.click(dippyBtn);
    expect(
      screen.getByRole("button", { name: /Dippy: Dancing/i }),
    ).not.toBeNull();
    expect(dippyImg.getAttribute("src")).toContain("dippy-dancing.webp");

    const plantBtn = screen.getByRole("button", { name: /Plant: Sprout/i });
    fireEvent.click(plantBtn);
    expect(
      screen.getByRole("button", { name: /Plant: Bloom/i }),
    ).not.toBeNull();

    const catBtn = screen.getByRole("button", { name: /Cat: Playful/i });
    fireEvent.click(catBtn);
    expect(screen.getByRole("button", { name: /Cat: Grown/i })).not.toBeNull();
  });

  it("Card 3 updates goal readout, aria-valuetext and fill bar from 750 to 3000 ml", () => {
    const { container } = render(<Showcase />);
    const slider = screen.getByRole("slider") as HTMLInputElement;
    expect(slider.min).toBe("750");
    expect(slider.max).toBe("3000");

    fireEvent.change(slider, { target: { value: "2500" } });
    expect(slider.getAttribute("aria-valuetext")).toBe(
      "2500 millilitres daily minimum",
    );
    expect(screen.getByText(/2,500/)).not.toBeNull();

    const barFill = container.querySelector(".sc-goal-bar-fill") as HTMLElement;
    expect(barFill.style.transform).toContain("scaleX(");
  });
});
