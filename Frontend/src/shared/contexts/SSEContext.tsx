import { createContext, useEffect, useRef, useState, type ReactNode } from "react";
import { authApis } from "../../state/api";

interface SSEContextValue {
  isConnected: boolean;
  subscribe: (eventName: string, handler: (data: any) => void) => () => void;
  forceConnect: () => void;
  allowDisconnect: () => void;
}

const SSEContext = createContext<SSEContextValue | null>(null);

export const SSEContextProvider = ({ children }: { children: ReactNode }) => {
  const eventSourceRef = useRef<EventSource | null>(null);
  const listenersRef = useRef<Map<string, Set<(data: any) => void>>>(new Map());
  const [isConnected, setIsConnected] = useState(false);
  const forceConnectionRef = useRef(false);

  const hasActiveListeners = () => {
    return Array.from(listenersRef.current.values()).some((set) => set.size > 0);
  };

  const connect = () => {
    if (eventSourceRef.current) return;

    const es = new EventSource(`${import.meta.env.VITE_TRACKERA_BACKEND_URL}/notifications/subscribe`, {
      withCredentials: true,
    });

    es.addEventListener("connected", () => {
      setIsConnected(true);
    });

    es.addEventListener("worklog-sync-status", (event: MessageEvent) => {
      const data = JSON.parse(event.data);
      const handlers = listenersRef.current.get("worklog-sync-status");
      handlers?.forEach((handler) => handler(data));
    });

    es.addEventListener("auth-error", async () => {
      try {
        await authApis.refreshToken();
        disconnect();
        connect();
      } catch {
        disconnect();
      }
    });

    es.onerror = (error) => {
      console.log("SSE error:", error);
      disconnect();
    };

    eventSourceRef.current = es;
  };

  const disconnect = () => {
    if (!eventSourceRef.current) return;
    eventSourceRef.current.close();
    eventSourceRef.current = null;
    setIsConnected(false);
  };

  const subscribe = (eventName: string, handler: (data: any) => void) => {
    // Add handler to listeners
    if (!listenersRef.current.has(eventName)) {
      listenersRef.current.set(eventName, new Set());
    }
    listenersRef.current.get(eventName)!.add(handler);

    // Connect if needed
    if (!eventSourceRef.current) {
      connect();
    }

    // Return unsubscribe function
    return () => {
      const handlers = listenersRef.current.get(eventName);
      handlers?.delete(handler);

      // Disconnect if no more listeners and not forced
      if (!hasActiveListeners() && !forceConnectionRef.current) {
        disconnect();
      }
    };
  };

  const forceConnect = () => {
    forceConnectionRef.current = true;
    if (!eventSourceRef.current) {
      connect();
    }
  };

  const allowDisconnect = () => {
    forceConnectionRef.current = false;
    if (!hasActiveListeners()) {
      disconnect();
    }
  };

  useEffect(() => {
    return () => disconnect();
  }, []);

  return (
    <SSEContext.Provider value={{ isConnected, subscribe, forceConnect, allowDisconnect }}>
      {children}
    </SSEContext.Provider>
  );
};

export default SSEContext;
