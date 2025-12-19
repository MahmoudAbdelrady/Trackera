import { useEffect, useRef } from "react";
import { authApis } from "../../state/api";

type UseStatusSSEOptions = {
  enabled: boolean;
  onStatusEvent: (event: any) => void;
  onOpen?: () => void;
};

export const useWorklogStatusSSE = ({ enabled, onStatusEvent, onOpen }: UseStatusSSEOptions) => {
  const eventSourceRef = useRef<EventSource | null>(null);

  const subscribe = () => {
    if (eventSourceRef.current) return;

    const es = new EventSource(`${import.meta.env.VITE_TRACKERA_BACKEND_URL}/notifications/subscribe`, { withCredentials: true });

    es.addEventListener("connected", (_) => {
      onOpen?.();
    });

    es.addEventListener("worklog-sync-status", (event: MessageEvent) => {
      onStatusEvent(JSON.parse(event.data));
    });

    es.addEventListener("auth-error", async (_) => {
      try {
        await authApis.refreshToken();
        closeEventSource();
        subscribe();
        return;
      } catch {
        // non-JSON error, ignore
      }
      closeEventSource();
    });

    es.onerror = (error: any) => {
      console.log("SSE error:", error);
      closeEventSource();
    };

    eventSourceRef.current = es;
  };

  useEffect(() => {
    if (enabled && !eventSourceRef.current) {
      subscribe();
    }

    if (!enabled && eventSourceRef.current) {
      closeEventSource();
    }
  }, [enabled]);

  useEffect(() => {
    return closeEventSource;
  }, []);

  const closeEventSource = () => {
    if (!eventSourceRef.current) return;
    eventSourceRef.current.close();
    eventSourceRef.current = null;
  };
};
