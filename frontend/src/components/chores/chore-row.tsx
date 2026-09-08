import { Archive, Check, Circle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { haptics } from "@/lib/haptics";
import type { ChoreBoardItem, ChoreCadence, ChoreScope } from "@/lib/types";
import { cn } from "@/lib/utils";

interface ChoreRowProps {
  chore: ChoreBoardItem;
  /** Active scope tab. When it already implies the chore's cadence the label is
   * redundant (e.g. "Daily" inside the Today column) and is suppressed. */
  activeScope?: ChoreScope;
  onArchive?: () => void;
  onComplete?: () => void;
  onUncomplete?: () => void;
}

function cadenceLabel(cadence: ChoreBoardItem["cadence"]): string {
  if (cadence === "DAILY") return "Daily";
  if (cadence === "WEEKLY") return "Weekly";
  return "Monthly";
}

const IMPLIED_BY: Record<ChoreCadence, ChoreScope> = {
  DAILY: "TODAY",
  WEEKLY: "THIS_WEEK",
  MONTHLY: "THIS_MONTH",
};

export function ChoreRow({
  chore,
  activeScope,
  onArchive,
  onComplete,
  onUncomplete,
}: ChoreRowProps) {
  const showCadence = IMPLIED_BY[chore.cadence] !== activeScope;

  // Shared by the checkoff and the row-body button below, so both stay on the
  // same haptics path (success() on the completing transition only).
  const toggleCompletion = () => {
    if (chore.completed) {
      onUncomplete?.();
    } else {
      haptics.success();
      onComplete?.();
    }
  };

  return (
    <div
      data-testid={`chore-row-${chore.templateId}`}
      className={cn(
        "flex min-h-14 items-center gap-3 rounded-lg border p-3 transition-colors",
        chore.completed
          ? "border-border bg-muted/40"
          : "border-transparent bg-card hover:border-border",
      )}
    >
      {/*
        Keep this a raw <button>, not <Button>/usePressable (the Archive control
        beside it uses <Button>): a pressable would fire haptics.tap() on
        pointerdown, and the shared 40ms throttle would then coalesce away the
        haptics.success() pulse on click. The single completion pulse depends on
        no preceding tap. Guarded by the throttle-coupling test in haptics.test.ts.

        One control, not two: the indicator and the text share this button, the
        way ListItemRow's toggle does. A second sibling button over the text
        would need a distinct accessible name, and any static one ("Complete
        <title>") lies in the completed state, where activating it un-completes.
        Archive stays a sibling — nothing interactive nests here.
      */}
      <button
        type="button"
        aria-label={
          chore.completed
            ? `Mark ${chore.title} incomplete`
            : `Mark ${chore.title} complete`
        }
        onClick={toggleCompletion}
        className="group flex min-h-11 min-w-0 flex-1 items-center gap-3 text-left"
      >
        {/* group-hover, not hover: the indicator is no longer the hovered
            element, so the affordance has to come from the button. */}
        <span
          data-testid="chore-checkoff"
          className={cn(
            "flex h-11 w-11 shrink-0 items-center justify-center rounded-full border-2 transition-colors",
            chore.completed
              ? "border-primary bg-primary text-primary-foreground"
              : "border-border bg-background text-muted-foreground group-hover:border-primary group-hover:text-primary",
          )}
        >
          {chore.completed ? (
            <Check className="h-5 w-5" />
          ) : (
            <Circle className="h-5 w-5" />
          )}
        </span>

        <span className="flex min-w-0 flex-1 flex-col">
          <span
            data-testid="chore-title"
            className={cn(
              "truncate text-sm font-semibold text-foreground",
              chore.completed && "text-muted-foreground line-through",
            )}
          >
            {chore.title}
          </span>
          {showCadence && (
            <span className="mt-1 text-xs font-medium text-muted-foreground">
              {cadenceLabel(chore.cadence)}
            </span>
          )}
        </span>
      </button>

      <Button
        type="button"
        variant="ghost"
        size="icon-lg"
        aria-label={`Archive ${chore.title}`}
        onClick={onArchive}
        className="text-muted-foreground hover:text-foreground"
      >
        <Archive className="h-4 w-4" />
      </Button>
    </div>
  );
}

export type { ChoreRowProps };
