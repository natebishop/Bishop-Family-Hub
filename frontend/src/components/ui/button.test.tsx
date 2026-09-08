import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { Button, buttonSizes, buttonVariants } from "./button";

describe("Button press feedback", () => {
  it("carries the pressable class", () => {
    render(<Button>Save</Button>);
    expect(screen.getByRole("button").className).toContain(
      "active:scale-[0.97]",
    );
  });
  it("fires the pressable seam and the caller's onPointerDown", () => {
    const onPointerDown = vi.fn();
    render(<Button onPointerDown={onPointerDown}>Save</Button>);
    screen
      .getByRole("button")
      .dispatchEvent(new Event("pointerdown", { bubbles: true }));
    expect(onPointerDown).toHaveBeenCalledTimes(1);
  });
});

describe("Button touch targets", () => {
  // Enumerated from the exported variant map, not a hand-kept list, so adding a
  // size without a 44px height fails here instead of shipping. The h-9 `sm`
  // variant is what this catches.
  const sizes = Object.keys(buttonSizes) as (keyof typeof buttonSizes)[];

  it("enumerates every declared size", () => {
    // Guards the line above: if buttonSizes were ever emptied or renamed, the
    // loop below would pass vacuously.
    expect(sizes).toEqual(
      expect.arrayContaining(["default", "sm", "lg", "icon"]),
    );
  });

  it.each(sizes)("sizes %s at the 44px minimum", (size) => {
    // h-11 / min-h-11 / size-11 are the 44px spellings; anything else (h-9,
    // h-10, size-9) leaves a control short of the PRD minimum.
    expect(buttonVariants({ size })).toMatch(/\b(?:min-h-11|h-11|size-11)\b/);
  });

  it("has no compact icon variant left to reach for", () => {
    expect(sizes).not.toContain("icon-sm");
  });
});
