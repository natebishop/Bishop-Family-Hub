import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import type * as React from "react";

import { usePressable } from "@/hooks";
import { cn } from "@/lib/utils";

/** Exported so button.test.tsx can enumerate the real variants rather than a
 * hand-kept copy that drifts the moment someone adds a size. */
const buttonSizes = {
  default: "h-11 px-4 py-2 has-[>svg]:px-3",
  sm: "min-h-11 rounded-lg gap-1.5 px-3 text-sm has-[>svg]:px-2.5",
  lg: "h-11 rounded-xl px-6 text-base has-[>svg]:px-4",
  icon: "size-11",
  "icon-lg": "size-11",
} as const;

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg text-[15px] leading-none font-semibold transition-all disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg:not([class*='size-'])]:size-4 shrink-0 [&_svg]:shrink-0 outline-none focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px] aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground hover:bg-primary/90",
        destructive:
          "bg-destructive text-white hover:bg-destructive/90 focus-visible:ring-destructive/20 dark:focus-visible:ring-destructive/40 dark:bg-destructive/60",
        outline:
          "border bg-background shadow-xs hover:bg-accent hover:text-accent-foreground dark:bg-input/30 dark:border-input dark:hover:bg-input/50",
        secondary:
          "bg-secondary text-secondary-foreground hover:bg-secondary/80",
        ghost:
          "hover:bg-accent hover:text-accent-foreground dark:hover:bg-accent/50",
        link: "text-primary underline-offset-4 hover:underline",
      },
      // Touch-target rule (PRD prd.md:815/827): any control reachable on a touch
      // device must be >= 44x44px at EVERY width, not only at lg:. EVERY size
      // variant meets it, so there is no wrong choice here and no rule to
      // remember.
      //
      // `sm` used to be h-9 with the rule "add your own min-h-11 below lg".
      // That rule held at 6 of 35 call sites — the other 29 were sheet and
      // modal chrome that only ever renders on a phone, and neither guard could
      // see them. A variant nobody may safely use is not a variant; `sm` now
      // means lighter type and tighter padding, not a shorter target. Same
      // reasoning that moved `default`/`icon`/Input off h-10 rather than
      // patching their call sites one at a time.
      //
      // `icon-sm` (size-9) is gone: it had no remaining uses.
      //
      // Width is still per-site — these set height, and a short label can leave
      // a button under 44px wide. Add `min-w-11` (see recipe-filter-bar.tsx).
      //
      // Enforced by button.test.tsx (every variant >= 44px),
      // src/test/touch-target-rule.test.ts (bans breakpoint-only sizing) and
      // e2e/touch-targets.spec.ts (measures real boxes at 390px and 900px).
      size: buttonSizes,
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  },
);

function Button({
  className,
  variant,
  size,
  asChild = false,
  onPointerDown,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean;
  }) {
  const Comp = asChild ? Slot : "button";
  const pressable = usePressable();

  return (
    <Comp
      data-slot="button"
      className={cn(
        buttonVariants({ variant, size }),
        pressable.className,
        className,
      )}
      onPointerDown={(event) => {
        pressable.onPointerDown(event);
        onPointerDown?.(event);
      }}
      {...props}
    />
  );
}

export { Button, buttonSizes, buttonVariants };
