import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { haptics } from "@/lib/haptics";
import type { ChoreBoardItem } from "@/lib/types";
import { ChoreRow } from "./chore-row";

// Cast: the row only reads templateId/title/cadence/completed.
const baseChore = {
  templateId: "t1",
  title: "Dishes",
  cadence: "DAILY",
  completed: false,
} as ChoreBoardItem;

describe("ChoreRow haptics", () => {
  it("fires success() on the complete path", async () => {
    const success = vi.spyOn(haptics, "success").mockImplementation(() => {});
    render(
      <ChoreRow
        chore={baseChore}
        onComplete={vi.fn()}
        onUncomplete={vi.fn()}
      />,
    );
    await userEvent.click(
      screen.getByRole("button", { name: /mark dishes complete/i }),
    );
    expect(success).toHaveBeenCalledTimes(1);
  });

  it("does NOT fire success() on the uncomplete path", async () => {
    const success = vi.spyOn(haptics, "success").mockImplementation(() => {});
    render(
      <ChoreRow
        chore={{ ...baseChore, completed: true }}
        onComplete={vi.fn()}
        onUncomplete={vi.fn()}
      />,
    );
    await userEvent.click(
      screen.getByRole("button", { name: /mark dishes incomplete/i }),
    );
    expect(success).not.toHaveBeenCalled();
  });
});

it("sizes the checkoff at 44px unconditionally, not only at lg", () => {
  render(<ChoreRow chore={baseChore} onComplete={() => {}} />);
  const checkoff = screen.getByTestId("chore-checkoff");
  expect(checkoff.className).toContain("h-11");
  expect(checkoff.className).toContain("w-11");
  expect(checkoff.className).not.toContain("lg:h-11");
  // The tappable control is the button wrapping it, not the indicator itself.
  expect(screen.getByRole("button", { name: /^Mark / }).className).toContain(
    "min-h-11",
  );
});

it("exposes exactly one completion control, labelled for the current state", () => {
  const { rerender } = render(
    <ChoreRow chore={baseChore} onComplete={() => {}} />,
  );
  expect(screen.getAllByRole("button", { name: /^Mark / })).toHaveLength(1);
  expect(screen.queryByRole("button", { name: /^Complete / })).toBeNull();

  // A static label would still read "Complete Dishes" here while activating it
  // un-completes the chore.
  rerender(
    <ChoreRow
      chore={{ ...baseChore, completed: true }}
      onUncomplete={() => {}}
    />,
  );
  expect(
    screen.getByRole("button", { name: "Mark Dishes incomplete" }),
  ).toBeInTheDocument();
});

it("lets the row body complete the chore", async () => {
  const onComplete = vi.fn();
  render(<ChoreRow chore={baseChore} onComplete={onComplete} />);
  // The title text sits inside the toggle, so tapping it completes the chore.
  await userEvent.click(screen.getByTestId("chore-title"));
  expect(onComplete).toHaveBeenCalledTimes(1);
});

it("keeps Archive separate from the row-body target", async () => {
  const onComplete = vi.fn();
  const onArchive = vi.fn();
  render(
    <ChoreRow
      chore={baseChore}
      onComplete={onComplete}
      onArchive={onArchive}
    />,
  );
  await userEvent.click(screen.getByRole("button", { name: /^Archive / }));
  expect(onArchive).toHaveBeenCalledTimes(1);
  expect(onComplete).not.toHaveBeenCalled();
});

describe("ChoreRow cadence label", () => {
  it("hides the cadence label when the active scope already implies it", () => {
    render(
      <ChoreRow chore={baseChore} activeScope="TODAY" onComplete={() => {}} />,
    );
    expect(screen.queryByText("Daily")).toBeNull();
  });

  it("shows the cadence label when the scope does not imply it", () => {
    render(
      <ChoreRow
        chore={baseChore}
        activeScope="THIS_WEEK"
        onComplete={() => {}}
      />,
    );
    expect(screen.getByText("Daily")).toBeInTheDocument();
  });

  it("shows the cadence label when no scope is supplied", () => {
    render(<ChoreRow chore={baseChore} onComplete={() => {}} />);
    expect(screen.getByText("Daily")).toBeInTheDocument();
  });
});
