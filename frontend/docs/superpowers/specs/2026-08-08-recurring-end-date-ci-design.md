# Recurring End-Date CI Fix Design

## Problem

The `CI` workflow on `origin/main` failed in GitHub Actions run
`30647242800`. Lint, unit coverage, backend startup, and 204 E2E tests passed;
the only failure was `Recurring Events > creates recurring event with end
date` in Chromium, Firefox, and WebKit, including every retry.

The test ran on July 31, 2026 and requested a recurrence end date two days
later, August 2. The recurrence date picker opened on July 2026. That month's
rendered grid ended on Saturday, August 1, so no button for Sunday, August 2
existed in the DOM. The shared `createEvent` E2E helper assumed the requested
date was already rendered and waited until its click timed out.

The relevant helper and date-picker code did not change between the successful
July 25 `main` run and the failed July 31 run. The passage of time exposed a
latent month-boundary assumption in test infrastructure; this is not an
application regression or a browser-specific flake.

## Goals

- Let the shared E2E helper select any valid recurrence end date in a later
  month by interacting with the date picker's public, accessible controls.
- Add a deterministic regression that always places the requested date beyond
  the initially rendered month grid.
- Preserve the existing application date-picker behavior and recurring-event
  product contract.
- Keep local iteration focused by running the affected E2E scenarios against
  the repository's Docker Compose backend before the full verification suite.

## Non-goals

- Changing the production date picker to render a fixed six-week grid.
- Hiding the bug by choosing an end date that happens to remain visible.
- Refactoring unrelated calendar tests or production date handling.

## Considered Approaches

### 1. Navigate the picker in the E2E helper (selected)

Read the selected day from DayPicker's canonical `data-day` attribute, compare
its calendar month with the requested end date, and click the accessible
previous- or next-month control once per month of difference. Then select the
requested day by its full accessible date name.

This fixes the failing abstraction at its source, works for dates farther than
one month away, and leaves production behavior unchanged.

### 2. Force six visible weeks in the production calendar

This would make August 2 visible in the observed July grid, but it changes the
UI merely to accommodate a test and still does not support dates farther into
the future. It is therefore rejected.

### 3. Constrain the test date to the current visible grid

This would turn the current run green but preserve the helper's invalid
assumption. The failure would recur under another date choice, so this is
rejected.

## Detailed Design

### Shared picker navigation

`e2e/helpers/test-helpers.ts` will gain a focused internal helper for selecting
a date from an already-open DayPicker popover:

1. Locate the selected grid cell and read its `data-day` value.
2. Parse that canonical `yyyy-MM-dd` value with `parseLocalDate`, following the
   repository's local-date rules.
3. Calculate the signed month difference using local year and month fields.
4. Click `Go to the Next Month` or `Go to the Previous Month` for the absolute
   number of month steps.
5. Assert that the target month's grid is visible.
6. Click the requested day using its full accessible label, such as
   `Tuesday, September 8th, 2026`.

The existing `createEvent` helper will delegate recurrence end-date selection
to this function after opening the popover. The signed calculation makes the
helper complete even though valid recurrence end dates normally move forward.

### Deterministic regression

`e2e/calendar-recurring.spec.ts` will add a scenario whose end date is the
eighth day of the next month. A month grid can show at most the first six days
of its following month as outside days, so the target is guaranteed not to be
present until the helper navigates.

The scenario will create the recurring event, open its details, and assert the
formatted `Daily until ...` label. Before the helper fix, it must time out while
looking for the target date. After the fix, it proves both cross-month
navigation and successful persistence of the chosen recurrence end date.

### Error handling

Missing selected-day metadata will fail immediately with an explicit assertion
instead of producing an invalid month calculation. After navigation, the
helper will assert the target grid is visible before attempting the final day
click, keeping failures close to the broken interaction boundary.

## Verification

1. Run the new cross-month E2E scenario before implementation and confirm it
   fails because the target day is absent.
2. Run it after implementation in Chromium, Firefox, and WebKit against the
   Docker Compose backend.
3. Run the complete recurring-event E2E spec.
4. Run lint, all unit tests, the production build, and the CI-equivalent E2E
   suite with Node 22 where available.
