import { createContext, useEffect, useRef, useState, type ReactNode } from "react";
import { authApis } from "../../state/api";
import { API_BASE_URL } from "../constants";

interface SSEContextValue {
  isConnected: boolean;
  isConnecting: boolean;
  subscribe: (eventName: string, handler: (data: any) => void) => () => void;
  forceConnect: () => void;
  allowDisconnect: () => void;
}

const SSEContext = createContext<SSEContextValue | null>(null);

export const SSEContextProvider = ({ children }: { children: ReactNode }) => {
  const eventSourceRef = useRef<EventSource | null>(null);
  const listenersRef = useRef<Map<string, Set<(data: any) => void>>>(new Map());
  const [isConnected, setIsConnected] = useState(false);
  const [isConnecting, setIsConnecting] = useState(false);
  const forceConnectionRef = useRef(false);

  const hasActiveListeners = () => {
    return Array.from(listenersRef.current.values()).some((set) => set.size > 0);
  };

  const connect = () => {
    if (eventSourceRef.current) return;

    setIsConnecting(true);
    const es = new EventSource(`${API_BASE_URL}/notifications/subscribe`, {
      withCredentials: true,
    });

    es.addEventListener("connected", () => {
      setIsConnected(true);
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

    es.onerror = (_error) => {
      disconnect();
    };

    eventSourceRef.current = es;
    setIsConnecting(false);
  };

  const disconnect = () => {
    if (!eventSourceRef.current) return;
    eventSourceRef.current.close();
    eventSourceRef.current = null;
    setIsConnected(false);
  };

  const attachEventListener = (eventName: string) => {
    if (!eventSourceRef.current) return;

    eventSourceRef.current.addEventListener(eventName, (event: MessageEvent) => {
      const data = JSON.parse(event.data);
      const handlers = listenersRef.current.get(eventName);
      handlers?.forEach((handler) => handler(data));
    });
  };

  const subscribe = (eventName: string, handler: (data: any) => void) => {
    // Add handler to listeners
    if (!listenersRef.current.has(eventName)) {
      listenersRef.current.set(eventName, new Set());
    }
    const handlers = listenersRef.current.get(eventName)!;
    handlers.add(handler);

    // Connect if needed
    if (!eventSourceRef.current) {
      connect();
    }

    // Attach event listener if first handler for this event
    if (handlers.size === 1) {
      attachEventListener(eventName);
    }

    // Return unsubscribe function
    return () => {
      handlers.delete(handler);

      if (handlers.size === 0) {
        listenersRef.current.delete(eventName);
      }

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
    <SSEContext.Provider value={{ isConnected, isConnecting, subscribe, forceConnect, allowDisconnect }}>
      {children}
    </SSEContext.Provider>
  );
};

export default SSEContext;
