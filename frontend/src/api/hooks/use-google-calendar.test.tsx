import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import type { ReactNode } from "react";
import { afterAll, afterEach, beforeAll } from "vitest";
import { server } from "@/test/mocks/server";
import { calendarKeys, useCalendarEvents } from "./use-calendar";
import {
  googleCalendarKeys,
  useGoogleConnectionStatus,
  useUpdateGoogleCalendars,
  useSyncGoogleCalendar,
} from "./use-google-calendar";

const API_BASE = "http://localhost:3000/api";
const MEMBER_ID = "member-123";

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
  };
}

describe("useGoogleConnectionStatus", () => {
  it("fetches connection status for a member", async () => {
    server.use(
      http.get(`${API_BASE}/google/status/${MEMBER_ID}`, () =>
        HttpResponse.json({
          data: { connected: false, calendars: [] },
          message: null,
        }),
      ),
    );

    const { result } = renderHook(() => useGoogleConnectionStatus(MEMBER_ID), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.data.connected).toBe(false);
  });

  it("does not fetch when memberId is empty", () => {
    const { result } = renderHook(() => useGoogleConnectionStatus(""), {
      wrapper: createWrapper(),
    });

    expect(result.current.isFetching).toBe(false);
  });
});

describe("useSyncGoogleCalendar", () => {
  it("invalidates status and events on success", async () => {
    server.use(
      http.post(
        `${API_BASE}/google/sync/${MEMBER_ID}`,
        () => new HttpResponse(null, { status: 204 }),
      ),
    );

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    // Seed cache
    queryClient.setQueryData(googleCalendarKeys.status(MEMBER_ID), {
      data: { connected: true, calendars: [] },
    });
    queryClient.setQueryData(calendarKeys.events(), { data: [] });

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useSyncGoogleCalendar(), {
      wrapper,
    });

    result.current.mutate(MEMBER_ID);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    // Verify cache was invalidated
    expect(
      queryClient.getQueryState(googleCalendarKeys.status(MEMBER_ID))
        ?.isInvalidated,
    ).toBe(true);
  });
});

describe("useUpdateGoogleCalendars", () => {
  it("clears stale event lists and refetches the active range after saving", async () => {
    const range = { startDate: "2026-06-01", endDate: "2026-06-30" };
    let eventFetches = 0;

    server.use(
      http.put(`${API_BASE}/google/calendars/${MEMBER_ID}`, () =>
        HttpResponse.json({
          data: [
            { id: "primary", name: "Main Calendar", primary: true, enabled: true },
          ],
          message: "Calendar selection updated",
        }),
      ),
      http.get(`${API_BASE}/calendar/events`, () => {
        eventFetches += 1;
        return HttpResponse.json({ data: [], message: null });
      }),
    );

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    queryClient.setQueryData(calendarKeys.eventList(range), {
      data: [
        {
          id: "stale-kamccor-event",
          title: "Stale event from a disabled calendar",
          startTime: "9:00 AM",
          endTime: "10:00 AM",
          date: "2026-06-15",
          memberId: MEMBER_ID,
          isAllDay: false,
        },
      ],
    });

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(
      () => ({
        mutation: useUpdateGoogleCalendars(),
        events: useCalendarEvents(range),
      }),
      { wrapper },
    );

    result.current.mutation.mutate({
      memberId: MEMBER_ID,
      calendarIds: ["primary"],
    });

    await waitFor(() => expect(result.current.mutation.isSuccess).toBe(true));
    await waitFor(() => expect(result.current.events.data?.data).toEqual([]));

    expect(eventFetches).toBeGreaterThan(0);
  });
});
