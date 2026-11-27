/**
 * Component smoke tests (happy-dom): the glass library renders, the tab bar
 * reports changes, and the toggle flips. Fluidkit's spring animation is
 * inert in the test environment — that's expected and covered visually via
 * the live preview (no headless browser in the build sandbox).
 */
import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "../../lib/theme";
import { GlassCard } from "./GlassCard";
import { GlassTabBar } from "./GlassTabBar";
import { GlassToggle } from "./GlassToggle";
import { GlassTextField } from "./GlassTextField";
import { GlassIconButton } from "./GlassIconButton";

afterEach(cleanup);

describe("glass components", () => {
  it("GlassCard renders the .glass surface with a backdrop filter", () => {
    const { container } = render(
      <ThemeProvider>
        <GlassCard data-testid="card">
          <p>hi</p>
        </GlassCard>
      </ThemeProvider>,
    );
    const card = container.querySelector(".glass");
    expect(card).not.toBeNull();
    const style = window.getComputedStyle(
      card as Element,
    ) as CSSStyleDeclaration & { webkitBackdropFilter?: string };
    expect(style.backdropFilter || style.webkitBackdropFilter).toContain(
      "blur",
    );
  });

  it("GlassTabBar calls onChange with the selected tab id", () => {
    const onChange = vi.fn();
    render(
      <ThemeProvider>
        <GlassTabBar
          value="home"
          onChange={onChange}
          items={[
            { id: "home", label: "Home" },
            { id: "drinks", label: "Drinks" },
            { id: "friends", label: "Friends" },
          ]}
        />
      </ThemeProvider>,
    );
    fireEvent.click(screen.getByText("Drinks"));
    expect(onChange).toHaveBeenCalledWith("drinks");
  });

  it("GlassToggle reports the next state", () => {
    const onCheckedChange = vi.fn();
    render(
      <ThemeProvider>
        <GlassToggle
          checked={false}
          onCheckedChange={onCheckedChange}
          label="Reminders"
        />
      </ThemeProvider>,
    );
    // fluidkit renders a real hidden input with role="switch"
    fireEvent.click(screen.getByRole("switch"));
    expect(onCheckedChange).toHaveBeenCalledWith(true);
  });

  it("GlassTextField wires its label to the input", () => {
    render(
      <ThemeProvider>
        <GlassTextField label="Add a drink" placeholder="water" />
      </ThemeProvider>,
    );
    const input = screen.getByPlaceholderText("water");
    expect(input).not.toBeNull();
    const label = screen.getByText("Add a drink");
    expect(label.getAttribute("for")).toBe(input.getAttribute("id"));
  });

  it("GlassIconButton exposes its accessible label", () => {
    render(
      <ThemeProvider>
        <GlassIconButton label="Add drink">
          <span aria-hidden>•</span>
        </GlassIconButton>
      </ThemeProvider>,
    );
    expect(screen.getByRole("button", { name: "Add drink" })).not.toBeNull();
  });
});
