import { useEffect, useMemo, useRef } from "react";
import { authApis } from "../../state/api";

type UseStatusSSEOptions<T> = {
  items: T[] | undefined;
  isInProgress: (item: T) => boolean;
  onStatusEvent: (event: any) => void;
};

export const useWorklogStatusSSE = <T>({ items, isInProgress, onStatusEvent }: UseStatusSSEOptions<T>) => {
  const eventSourceRef = useRef<EventSource | null>(null);

  const hasInProgress = useMemo(() => {
    return items?.some(isInProgress) ?? false;
  }, [items, isInProgress]);

  const subscribe = () => {
    if (eventSourceRef.current) return;

    const es = new EventSource(`${import.meta.env.VITE_TRACKERA_BACKEND_URL}/notifications/subscribe`, { withCredentials: true });

    es.addEventListener("worklog-sync-status", (event: MessageEvent) => {
      onStatusEvent(JSON.parse(event.data));
    });

    es.addEventListener("error", async (event: MessageEvent) => {
      try {
        const errorResponse = JSON.parse(event.data);
        if (errorResponse.status === 401) {
          await authApis.refreshToken();
          closeEventSource();
          subscribe();
          return;
        }
      } catch {
        // non-JSON error, ignore
      }
      closeEventSource();
    });

    eventSourceRef.current = es;
  };

  useEffect(() => {
    if (hasInProgress && !eventSourceRef.current) {
      subscribe();
    }

    if (!hasInProgress && eventSourceRef.current) {
      closeEventSource();
    }
  }, [hasInProgress]);

  useEffect(() => {
    return closeEventSource;
  }, []);

  const closeEventSource = () => {
    if (!eventSourceRef.current) return;
    eventSourceRef.current.close();
    eventSourceRef.current = null;
  };
};
