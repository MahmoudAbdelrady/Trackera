import { useState, useEffect, useRef } from "react";
import type { JiraSyncEventProps, SyncPayload } from "../types";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import { worklogApis } from "../../state/api";
import { useSSEContext } from "./useSSEContext";

interface JiraSyncSSEReturn {
  triggerSync: (payload: SyncPayload) => void;
  isRequestingSync: boolean;
}

interface JiraSyncSSEOptions {
  hasInProgress: boolean;
  onStatusEvent: (event: JiraSyncEventProps) => void;
}

export const useJiraSyncSSE = ({ hasInProgress, onStatusEvent }: JiraSyncSSEOptions): JiraSyncSSEReturn => {
  const [pendingSyncPayload, setPendingSyncPayload] = useState<SyncPayload | null>(null);
  const [sseAck, setSseAck] = useState<boolean>(false);
  const [wantsSSE, setWantsSSE] = useState<boolean>(false);
  const [syncRequested, setSyncRequested] = useState<boolean>(false);

  const { isConnected, isConnecting, subscribe, forceConnect, allowDisconnect } = useSSEContext();
  const unsubscribeRef = useRef<null | (() => void)>(null);

  const shouldSubscribe = wantsSSE || hasInProgress;

  // ---- Listen to SSE events ----
  useEffect(() => {
    if (!shouldSubscribe) return;

    const unsubscribe = subscribe("worklog-sync-status", (event) => {
      setSseAck(true);
      onStatusEvent(event);
    });

    unsubscribeRef.current = unsubscribe;

    return () => {
      unsubscribe();
      unsubscribeRef.current = null;
    };
  }, [shouldSubscribe, onStatusEvent]);

  useEffect(() => {
    if (hasInProgress) {
      forceConnect();
    }
  }, [hasInProgress]);

  // --- Fire sync after SSE connection is ready ---
  useEffect(() => {
    if (!pendingSyncPayload || !isConnected) return;

    fireSync(pendingSyncPayload);
    setPendingSyncPayload(null);
  }, [isConnected, pendingSyncPayload]);

  // --- Auto-close connection when no in-progress worklogs and no pending sync ---
  useEffect(() => {
    if (!shouldSubscribe) return;

    if (sseAck && !hasInProgress && !pendingSyncPayload) {
      unsubscribeRef.current?.();
      unsubscribeRef.current = null;
      setWantsSSE(false);
      allowDisconnect();
    }
  }, [sseAck, hasInProgress, pendingSyncPayload, shouldSubscribe]);

  // --- Public API ---
  const triggerSync = (payload: SyncPayload) => {
    setPendingSyncPayload(payload);
    setSseAck(false);
    setWantsSSE(true);
    forceConnect();
  };

  const fireSync = async (payload: SyncPayload) => {
    setSyncRequested(true);
    try {
      const response = await worklogApis.syncWorklog(payload);
      showSuccessToast(response);
    } catch (error) {
      showErrorToast(error);
    }
    setSyncRequested(false);
  };

  return { triggerSync, isRequestingSync: isConnecting || syncRequested };
};
