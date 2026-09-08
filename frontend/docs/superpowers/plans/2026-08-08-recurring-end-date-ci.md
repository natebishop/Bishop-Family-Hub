# Recurring End-Date CI Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the recurring-event E2E helper navigate across date-picker months so `main` CI remains green at month boundaries.

**Architecture:** Keep production date-picker behavior unchanged and fix the shared Playwright abstraction that selects recurrence end dates. The helper will derive the picker's selected month from DayPicker's canonical `data-day`, navigate with accessible month controls, and then select the requested accessible date; a deterministic next-month regression will exercise that path.

**Tech Stack:** TypeScript, Playwright, React DayPicker accessibility semantics, date-fns, Docker Compose, Vitest, Biome, Vite

---

## File Map

- Modify `e2e/calendar-recurring.spec.ts`: add a deterministic cross-month
  recurring-end-date regression.
- Modify `e2e/helpers/test-helpers.ts`: navigate the open recurrence date picker
  to the requested month before selecting the day.
- Existing `docker-compose.e2e.yml`: provide the published backend used by the
  targeted and full Playwright runs.

### Task 1: Add the deterministic cross-month regression

**Files:**
- Modify: `e2e/calendar-recurring.spec.ts:1-13`
- Modify: `e2e/calendar-recurring.spec.ts:278-299`
- Test: `e2e/calendar-recurring.spec.ts`

- [ ] **Step 1: Import deterministic date helpers**

Add this import above the local E2E helper imports:

```typescript
import { addDays, addMonths, format, startOfMonth } from "date-fns";
```

- [ ] **Step 2: Add a regression whose target cannot be an outside day**

Append this test inside `test.describe("Recurring Events", ...)`:

```typescript
test("creates a recurring event with an end date outside the visible picker month", async ({
  page,
}) => {
  const endDate = addDays(startOfMonth(addMonths(new Date(), 1)), 7);

  await createEvent(page, {
    title: "Cross-month Workshop",
    recurrence: { frequency: "daily", endDate },
  });

  const dialog = await openEventDetail(page, "Cross-month Workshop");
  await expect(
    dialog.getByText(`Daily until ${format(endDate, "MMM d, yyyy")}`),
  ).toBeVisible();
});
```

The eighth of the following month cannot appear among the current month's
outside days, which are limited to at most the first six dates.

- [ ] **Step 3: Start the published backend locally**

Run:

```bash
BE_IMAGE_TAG=1.9.0 docker compose -f docker-compose.e2e.yml up -d --wait
```

Expected: the `family-hub-api` service reaches its healthy state.

- [ ] **Step 4: Run the regression and verify RED**

Run:

```bash
npx playwright test e2e/calendar-recurring.spec.ts --project=chromium --grep "outside the visible picker month"
```

Expected: FAIL in `createEvent` while waiting for the target date button,
because the picker remains on the initial month.

### Task 2: Navigate the recurrence date picker to its target month

**Files:**
- Modify: `e2e/helpers/test-helpers.ts:1-8`
- Modify: `e2e/helpers/test-helpers.ts:414-509`
- Test: `e2e/calendar-recurring.spec.ts`

- [ ] **Step 1: Import the repository local-date utilities**

Add this import with the existing `src` imports:

```typescript
import { parseLocalDate } from "../../src/lib/time-utils";
```

- [ ] **Step 2: Add the focused date-picker selection helper**

Add this internal helper immediately before `createEvent`:

```typescript
async function selectDateFromPopover(
  popover: Locator,
  targetDate: Date,
): Promise<void> {
  const selectedDateValue = await popover
    .locator('[data-selected="true"]')
    .getAttribute("data-day");

  expect(
    selectedDateValue,
    "Date picker should expose its selected date",
  ).not.toBeNull();

  const selectedDate = parseLocalDate(selectedDateValue ?? "");
  const selectedMonthIndex =
    selectedDate.getFullYear() * 12 + selectedDate.getMonth();
  const targetMonthIndex =
    targetDate.getFullYear() * 12 + targetDate.getMonth();
  const monthOffset = targetMonthIndex - selectedMonthIndex;
  const navigationName =
    monthOffset > 0 ? "Go to the Next Month" : "Go to the Previous Month";

  for (let step = 0; step < Math.abs(monthOffset); step += 1) {
    await popover.getByRole("button", { name: navigationName }).click();
  }

  await expect(
    popover.getByRole("grid", {
      name: format(targetDate, "MMMM yyyy"),
      exact: true,
    }),
  ).toBeVisible();

  await popover
    .getByRole("button", {
      name: format(targetDate, "EEEE, MMMM do, yyyy"),
    })
    .click();
}
```

- [ ] **Step 3: Delegate recurrence end-date selection to the helper**

Replace the existing inline accessible-label selection after the popover
becomes visible:

```typescript
await selectDateFromPopover(popover, options.recurrence.endDate);
```

- [ ] **Step 4: Run the deterministic regression and verify GREEN**

Run:

```bash
npx playwright test e2e/calendar-recurring.spec.ts --project=chromium --grep "outside the visible picker month"
```

Expected: PASS.

- [ ] **Step 5: Run the affected scenario across all failed CI browsers**

Run:

```bash
npx playwright test e2e/calendar-recurring.spec.ts --project=chromium --project=firefox --project=webkit --grep "outside the visible picker month"
```

Expected: 3 passed.

- [ ] **Step 6: Run the complete recurring-event spec**

Run:

```bash
npx playwright test e2e/calendar-recurring.spec.ts --project=chromium --project=firefox --project=webkit
```

Expected: all desktop recurring-event scenarios pass.

- [ ] **Step 7: Commit the tested fix atomically**

Run:

```bash
git add e2e/calendar-recurring.spec.ts e2e/helpers/test-helpers.ts
git commit -m "fix(e2e): navigate recurring end-date months"
```

Expected: one commit containing the regression and its minimal helper fix.

### Task 3: Verify the branch and prepare the pull request

**Files:**
- Verify: all tracked files

- [ ] **Step 1: Run the complete CI E2E suite locally**

Run under Node 22 while the Docker Compose backend is healthy:

```bash
CI=true npm run test:e2e
```

Expected: the full Playwright matrix passes, including production-build offline
coverage.

- [ ] **Step 2: Stop the local backend after E2E execution**

Run:

```bash
BE_IMAGE_TAG=1.9.0 docker compose -f docker-compose.e2e.yml down
```

Expected: the E2E backend container and network stop cleanly.

- [ ] **Step 3: Run static checks with the CI Node major**

Run under Node 22:

```bash
npm run lint
npm run format:check
npm run build
```

Expected: all commands exit 0.

- [ ] **Step 4: Run all unit tests**

Run under Node 22:

```bash
npm test -- --run
```

Expected: 177 test files and 1,769 existing tests pass; the E2E-only change
does not alter the Vitest count.

- [ ] **Step 5: Review branch scope and commit structure**

Run:

```bash
git diff --check origin/main...HEAD
git diff --stat origin/main...HEAD
git log --oneline origin/main..HEAD
git status --short --branch
```

Expected: only the design, plan, regression, and helper fix are present; no
working-tree changes remain.

- [ ] **Step 6: Push and open a ready pull request**

Run:

```bash
git push -u origin codex/fix-main-recurring-end-date-ci
gh pr create --base main --head codex/fix-main-recurring-end-date-ci --title "fix(e2e): navigate recurring end-date months" --body $'## Summary\n\n- navigate the recurring end-date picker to dates in other months\n- add a deterministic cross-month Playwright regression\n- document the failed main CI root cause and implementation plan\n\n## Testing\n\n- targeted Playwright regression in Chromium, Firefox, and WebKit\n- complete recurring-event Playwright spec\n- full CI Playwright matrix\n- npm run lint\n- npm run format:check\n- npm test -- --run\n- npm run build'
```

Expected: branch push succeeds and GitHub returns a pull-request URL.
