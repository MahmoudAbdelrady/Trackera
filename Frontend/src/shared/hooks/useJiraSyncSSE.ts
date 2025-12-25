import { useState, useEffect } from "react";
import { useSSE } from "./";
import type { JiraSyncEventProps, SyncPayload } from "../types";
import { showErrorToast } from "../../utils/toast-handler/showToast";
import { workLogApis } from "../../state/api";

type JiraSyncSSEReturn = {
  triggerSync: (payload: SyncPayload) => void;
};

type JiraSyncSSEOptions = {
  hasInProgress: boolean;
  onStatusEvent: (event: JiraSyncEventProps) => void;
};

export const useJiraSyncSSE = ({ hasInProgress, onStatusEvent }: JiraSyncSSEOptions): JiraSyncSSEReturn => {
  const [pendingSyncPayload, setPendingSyncPayload] = useState<SyncPayload | null>(null);
  const [forceSubscribe, setForceSubscribe] = useState(false);
  const [sseReady, setSseReady] = useState(false);
  const [sseAck, setSseAck] = useState(false);

  const sseEnabled = forceSubscribe || hasInProgress;

  // --- Subscribe to SSE ---
  useSSE({
    eventName: "worklog-sync-status",
    enabled: sseEnabled,
    onOpen: () => setSseReady(true),
    onStatusEvent: (event) => {
      setSseAck(true);
      onStatusEvent(event); // pass the event up
    },
  });

  // --- Fire sync after SSE connection is ready ---
  useEffect(() => {
    if (!pendingSyncPayload) return;

    if (sseReady || !sseEnabled) {
      fireSync(pendingSyncPayload);
      setPendingSyncPayload(null);
    }
  }, [sseReady, pendingSyncPayload, sseEnabled]);

  // --- Auto-close connection when no in-progress worklogs and no pending sync ---
  useEffect(() => {
    if (sseAck && !hasInProgress && !pendingSyncPayload) {
      setForceSubscribe(false);
    }
  }, [sseAck, hasInProgress, pendingSyncPayload]);

  // --- Public API ---
  const triggerSync = (payload: SyncPayload) => {
    setPendingSyncPayload(payload);
    setSseAck(false);

    // Only force subscription if there are no in-progress logs
    if (!hasInProgress) {
      setForceSubscribe(true);
      setSseReady(false);
    }
  };

  const fireSync = async (payload: SyncPayload) => {
    try {
      await workLogApis.syncWorkLog(payload);
    } catch (error) {
      showErrorToast(error);
    }
  };

  return { triggerSync };
};
