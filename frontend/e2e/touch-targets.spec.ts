import {
  type APIRequestContext,
  expect,
  type Page,
  test,
} from "@playwright/test";
import {
  createChoreTemplate,
  createList,
  createListItem,
  getChoresBoard,
  registerFamily,
  seedBrowserAuth,
} from "./helpers/api-helpers";
import { clearStorage, waitForHydration } from "./helpers/test-helpers";

/** PRD prd.md:815/827 — the iOS HIG / platform touch minimum. */
const MIN = 44;

/**
 * jsdom resolves no Tailwind, so unit tests can only assert class strings —
 * exactly how `lg:h-11` passed while mobile sat at 36px. This measures the real
 * laid-out box in a browser instead.
 */
async function assertMinimumTargets(
  page: Page,
  names: readonly (string | RegExp)[],
) {
  const undersized: string[] = [];
  for (const name of names) {
    const controls = page.getByRole("button", { name });
    // Wait for at least one to render before counting; a name that matches
    // nothing would pass vacuously, so the count assertion below is the guard.
    await expect(controls.first()).toBeVisible();
    const count = await controls.count();
    expect(count, `no control matched ${String(name)}`).toBeGreaterThan(0);
    for (let i = 0; i < count; i++) {
      const box = await controls.nth(i).boundingBox();
      if (!box) continue;
      if (box.width < MIN || box.height < MIN) {
        undersized.push(
          `${String(name)}#${i}: ${Math.round(box.width)}x${Math.round(box.height)}`,
        );
      }
    }
  }
  expect(undersized, `controls below ${MIN}px`).toEqual([]);
}

/**
 * Sets the viewport *before* the first paint so the breakpoint hooks
 * (`useIsMobile` ≤768, `useIsLargeScreen` ≥1024) settle on the band under test
 * rather than reacting to a resize mid-run.
 */
async function openAppAt(
  page: Page,
  request: APIRequestContext,
  width: number,
) {
  await page.setViewportSize({ width, height: 900 });
  await page.goto("/");
  await clearStorage(page);

  // Two members: the 390px calendar toolbar is the tightest row in the app and
  // its width budget is only genuinely under pressure with more than one dot.
  const registration = await registerFamily(request, {
    familyName: "Touch Target Family",
    members: [
      { name: "Alice", color: "coral" },
      { name: "Bob", color: "teal" },
    ],
  });

  // Seed real content — a freshly registered family has no chores and no lists,
  // so the chore/list selectors would match nothing and pass vacuously.
  //
  // activeFrom must come from the backend's own clock, not this process's:
  // the board scopes "today" to the family clock, so a runner-derived date is
  // one day ahead whenever the two disagree, the template is not yet active,
  // and the board renders its empty state. That window is only a few hours
  // wide, which is why this passed locally and on a mid-morning CI run before
  // failing on an 03:26 UTC one.
  const board = await getChoresBoard(request, registration.token);
  await createChoreTemplate(request, registration.token, {
    title: "Dishes",
    assignedToMemberId: registration.family.members[0].id,
    cadence: "DAILY",
    activeFrom: board.today.periodStartDate,
  });
  const list = await createList(request, registration.token, {
    name: "Groceries",
    kind: "grocery",
  });
  await createListItem(request, registration.token, list.id, { text: "Milk" });

  await seedBrowserAuth(page, registration);
  await page.reload();
  await waitForHydration(page);

  return page.getByRole("navigation", { name: /primary/i });
}

test.describe("touch targets meet the 44px minimum", () => {
  test("calendar at 390px", async ({ page, request }) => {
    const nav = await openAppAt(page, request, 390);
    await nav.getByRole("button", { name: "Calendar" }).click();

    // Below 769px the calendar renders MobileToolbar, whose pills are labelled
    // "Daily view"… (mobile-toolbar.tsx:12-15).
    await assertMinimumTargets(page, [
      "Previous",
      "Next",
      "Daily view",
      "Weekly view",
      "Monthly view",
      "Schedule view",
      "Today",
      /filter$/,
    ]);

    // The toolbar row must pan, not push the page wide: its member-dot group is
    // the elastic one (min-w-0 + overflow-x-auto) precisely because 44px pills,
    // prev/next and two dots cannot all fit 390px.
    const scrollW = await page.evaluate(
      () => document.documentElement.scrollWidth,
    );
    expect(scrollW).toBeLessThanOrEqual(390);
  });

  test("calendar at 900px (desktop switcher, no lg: rules)", async ({
    page,
    request,
  }) => {
    const nav = await openAppAt(page, request, 900);
    await nav.getByRole("button", { name: "Calendar" }).click();

    // At 900px the calendar renders the *desktop* switcher, labelled
    // "Day"/"Week"/"Month"/"Schedule" (calendar-view-switcher.tsx:9-16).
    await assertMinimumTargets(page, [
      "Previous",
      "Next",
      "Day",
      "Week",
      "Month",
      "Schedule",
      "Today",
    ]);
  });

  for (const width of [390, 900]) {
    test(`chores at ${width}px`, async ({ page, request }) => {
      const nav = await openAppAt(page, request, width);
      await nav.getByRole("button", { name: "Chores" }).click();
      await assertMinimumTargets(page, [
        // One control per row: the indicator and the title share this button,
        // so /^Mark / measures the whole tappable row body.
        /^Mark /,
        /^Archive /,
        "Add recurring chore",
        // ChoreScopeSwitcher is isMobile-gated (chores-view.tsx:144), so its
        // Day/Week/Month pills exist below 769px only — at 900px the board
        // renders scope columns instead. Asserting them at both widths would
        // fail on a control that legitimately is not there.
        ...(width < 769 ? ["Day", "Week", "Month"] : []),
      ]);
    });

    test(`lists at ${width}px`, async ({ page, request }) => {
      const nav = await openAppAt(page, request, width);
      await nav.getByRole("button", { name: "Lists" }).click();
      await page
        .getByRole("button", { name: /groceries/i })
        .first()
        .click();
      // Both widths are below lg, so both get the single overflow control —
      // that is the point: one gate for the button and its sheet.
      // Anchored: Playwright's string `name` is a case-insensitive *substring*
      // match, so a bare "Milk" also matches "More actions for Milk" — the same
      // trap the unit tests were switched to `exact: true` to avoid.
      await assertMinimumTargets(page, [
        /^More actions/,
        /^Milk$/,
        "Back to Lists",
      ]);
    });
  }
});
