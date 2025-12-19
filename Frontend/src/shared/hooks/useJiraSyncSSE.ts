import { useState, useEffect } from "react";
import { useSSE } from "./";
import requestInstance from "../axios/request-instance";
import type { SyncPayload } from "../types";

type JiraSyncSSEReturn = {
  triggerSync: (payload: SyncPayload) => void;
};

type JiraSyncSSEOptions = {
  hasInProgress: boolean;
  onStatusEvent: (event: any) => void;
};

export const useJiraSyncSSE = ({ hasInProgress, onStatusEvent }: JiraSyncSSEOptions): JiraSyncSSEReturn => {
  const [pendingSyncPayload, setPendingSyncPayload] = useState<SyncPayload | null>(null);
  const [forceSubscribe, setForceSubscribe] = useState(false);
  const [sseReady, setSseReady] = useState(false);
  const [sseAck, setSseAck] = useState(false);

  const enabled = forceSubscribe || hasInProgress;

  // --- Subscribe to SSE ---
  useSSE({
    eventName: "worklog-sync-status",
    enabled,
    onOpen: () => setSseReady(true),
    onStatusEvent: (event) => {
      setSseAck(true);
      onStatusEvent(event); // pass the event up
    },
  });

  // --- Fire sync after SSE connection is ready ---
  useEffect(() => {
    if (!sseReady || !pendingSyncPayload) return;

    fireSync(pendingSyncPayload);
    setPendingSyncPayload(null);
  }, [sseReady, pendingSyncPayload]);

  // --- Auto-close connection when no in-progress worklogs and no pending sync ---
  useEffect(() => {
    if (sseAck && !hasInProgress && !pendingSyncPayload) {
      setForceSubscribe(false);
    }
  }, [sseAck, hasInProgress, pendingSyncPayload]);

  // --- Public API ---
  const triggerSync = (payload: SyncPayload) => {
    setPendingSyncPayload(payload);
    setForceSubscribe(true);
    setSseReady(false);
    setSseAck(false);
  };

  const fireSync = async (payload: SyncPayload) => {
    const { workLogId, taskNames, entryIds, sync } = payload;

    await requestInstance.post(`/worklog/${workLogId}/sync${sync ? "" : "?sync=false"}`, { taskNames, entryIds });
  };

  return { triggerSync };
};
