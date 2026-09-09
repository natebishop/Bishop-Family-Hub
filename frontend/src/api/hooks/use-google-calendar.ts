import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ApiException } from "@/api/client";
import { googleCalendarService } from "@/api/services";
import type {
  ApiResponse,
  CalendarEvent,
  GoogleCalendarInfo,
} from "@/lib/types";
import { calendarKeys } from "./use-calendar";

export const googleCalendarKeys = {
  all: ["google-calendar"] as const,
  status: (memberId: string) =>
    [...googleCalendarKeys.all, "status", memberId] as const,
  calendars: (memberId: string) =>
    [...googleCalendarKeys.all, "calendars", memberId] as const,
};

// Queries

export function useGoogleConnectionStatus(memberId: string) {
  return useQuery({
    queryKey: googleCalendarKeys.status(memberId),
    queryFn: () => googleCalendarService.getConnectionStatus(memberId),
    enabled: !!memberId,
    staleTime: 30 * 1000,
  });
}

export function useGoogleCalendars(memberId: string, enabled = true) {
  return useQuery({
    queryKey: googleCalendarKeys.calendars(memberId),
    queryFn: () => googleCalendarService.getCalendars(memberId),
    enabled: !!memberId && enabled,
  });
}

// Mutations

interface GoogleMutationCallbacks<T = void> {
  onSuccess?: (data: T) => void;
  onError?: (error: ApiException) => void;
}

/**
 * Google selection changes are destructive to the visible event set. Clear
 * cached list responses before refetching so a disabled calendar cannot remain
 * on screen while the authoritative response is loading. Detail-event queries
 * share the same prefix, so the predicate deliberately excludes their string
 * ID key.
 */
function clearCalendarEventListCaches(
  queryClient: ReturnType<typeof useQueryClient>,
): void {
  queryClient.setQueriesData<ApiResponse<CalendarEvent[]>>(
    {
      queryKey: calendarKeys.events(),
      predicate: (query) =>
        query.queryKey.length === 3 && typeof query.queryKey[2] !== "string",
    },
    (old) => (old ? { ...old, data: [] } : old),
  );
}

export function useUpdateGoogleCalendars(
  callbacks?: GoogleMutationCallbacks<ApiResponse<GoogleCalendarInfo[]>>,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      memberId,
      calendarIds,
    }: {
      memberId: string;
      calendarIds: string[];
    }) => googleCalendarService.updateCalendars(memberId, calendarIds),
    onSuccess: async (data, variables) => {
      queryClient.invalidateQueries({
        queryKey: googleCalendarKeys.calendars(variables.memberId),
      });
      queryClient.invalidateQueries({
        queryKey: googleCalendarKeys.status(variables.memberId),
      });
      clearCalendarEventListCaches(queryClient);
      await queryClient.invalidateQueries({
        queryKey: calendarKeys.events(),
        refetchType: "active",
      });
      callbacks?.onSuccess?.(data);
    },
    onError: (error: ApiException) => {
      callbacks?.onError?.(error);
    },
  });
}

export function useSyncGoogleCalendar(callbacks?: GoogleMutationCallbacks) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (memberId: string) =>
      googleCalendarService.syncCalendar(memberId),
    onSuccess: (_data, memberId) => {
      queryClient.invalidateQueries({
        queryKey: googleCalendarKeys.status(memberId),
      });
      queryClient.invalidateQueries({
        queryKey: calendarKeys.events(),
      });
      callbacks?.onSuccess?.();
    },
    onError: (error: ApiException) => {
      callbacks?.onError?.(error);
    },
  });
}

export function useDisconnectGoogle(callbacks?: GoogleMutationCallbacks) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (memberId: string) =>
      googleCalendarService.disconnect(memberId),
    onSuccess: (_data, memberId) => {
      queryClient.invalidateQueries({
        queryKey: googleCalendarKeys.status(memberId),
      });
      queryClient.invalidateQueries({
        queryKey: calendarKeys.events(),
      });
      callbacks?.onSuccess?.();
    },
    onError: (error: ApiException) => {
      callbacks?.onError?.(error);
    },
  });
}
