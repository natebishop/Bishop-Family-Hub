import { Check, MoreHorizontal, Pencil, Trash2 } from "lucide-react";
import { haptics } from "@/lib/haptics";
import type { ListItem } from "@/lib/types";
import { cn } from "@/lib/utils";
import { Button } from "../ui/button";

interface ListItemRowProps {
  item: ListItem;
  onToggle: (completed: boolean) => void;
  onEdit: () => void;
  onDelete: () => void;
  /** Passed from the parent rather than read via hook so the row stays
   * presentational and both branches are testable without a matchMedia shim. */
  isLargeScreen?: boolean;
  onOpenActions?: () => void;
}

export function ListItemRow({
  item,
  onToggle,
  onEdit,
  onDelete,
  isLargeScreen,
  onOpenActions,
}: ListItemRowProps) {
  return (
    <div className="flex min-h-14 items-center gap-2 rounded-lg border border-border bg-card p-2 shadow-sm">
      {/*
        Keep this a raw <button>, not <Button>/usePressable: a pressable would
        fire haptics.tap() on pointerdown, and the shared 40ms throttle would
        then coalesce away the haptics.success() pulse on click. The single
        completion pulse depends on no preceding tap on the same gesture.
        Guarded by the throttle-coupling test in haptics.test.ts.
      */}
      <button
        type="button"
        aria-pressed={item.completed}
        onClick={() => {
          if (!item.completed) haptics.success(); // completing transition only
          onToggle(!item.completed);
        }}
        className="flex min-h-11 min-w-0 flex-1 items-center gap-3 rounded-md px-2 py-2 text-left transition-colors hover:bg-muted/70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      >
        <span
          className={cn(
            "flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-2 transition-colors",
            item.completed
              ? "border-primary bg-primary text-primary-foreground"
              : "border-border bg-background",
          )}
        >
          {item.completed && <Check className="h-4 w-4" />}
        </span>
        <span
          className={cn(
            "min-w-0 flex-1 break-words text-[15px] font-medium leading-5",
            item.completed
              ? "text-muted-foreground line-through"
              : "text-foreground",
          )}
        >
          {item.text}
        </span>
      </button>
      {/* Two 44px icons would eat ~96px of a 390px row and leave a destructive
          delete one thumb-slip from the toggle, so below lg they collapse into
          a single overflow control. Its gate must match the actions sheet's
          gate in list-detail-view exactly, or the 769-1023px band gets a
          button that opens nothing. */}
      {isLargeScreen ? (
        <>
          <Button type="button" variant="ghost" size="icon-lg" onClick={onEdit}>
            <Pencil className="h-4 w-4" />
            <span className="sr-only">Edit</span>
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="icon-lg"
            onClick={onDelete}
          >
            <Trash2 className="h-4 w-4" />
            <span className="sr-only">Delete</span>
          </Button>
        </>
      ) : (
        <Button
          type="button"
          variant="ghost"
          size="icon-lg"
          aria-label={`More actions for ${item.text}`}
          onClick={onOpenActions}
        >
          <MoreHorizontal className="h-4 w-4" />
        </Button>
      )}
    </div>
  );
}
