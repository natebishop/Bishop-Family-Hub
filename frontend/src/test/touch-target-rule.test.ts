import { readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";

/** Conditional 44px sizing is the defect this rule exists to prevent: a control
 * sized correctly at lg: while staying 32-40px on the phones the family uses.
 * If a genuine large-screen enhancement needs one of these, it must sit on top of
 * a base that already meets 44px — add the file to ALLOWED with a reason.
 *
 * Every breakpoint prefix counts, not just `lg:` — `md:h-11` leaves phones just
 * as short — and `size-11` is the same defect written the shorthand way. Both
 * were reachable holes in the first version of this rule. */
const BANNED = /\b(?:sm|md|lg|xl|2xl):(?:min-)?(?:size|[hw])-11\b/;
const ALLOWED = new Set<string>([]);

describe("touch-target rule", () => {
  it("has no breakpoint-only 44px sizing left in components", () => {
    const files = readdirSync(join(process.cwd(), "src"), {
      encoding: "utf8",
      recursive: true,
    })
      .filter((file) => /\.tsx?$/.test(file) && !file.includes(".test."))
      .map((file) => join("src", file));
    const offenders = files.filter((f) => {
      if (ALLOWED.has(f)) return false;
      return BANNED.test(readFileSync(join(process.cwd(), f), "utf8"));
    });
    expect(
      offenders,
      "files sizing touch targets only above a breakpoint",
    ).toEqual([]);
  });
});
