import type { TableActionButtonProps } from "../components";
import type { JiraSyncSSEReturn } from "../shared/hooks/useJiraSyncSSE";
import { WORKLOG_STATUS, type SyncPayload } from "../shared/types";
import type { UserInfo } from "../shared/types";
import { CalendarCog, CalendarOff, CalendarSync, CalendarX2 } from "lucide-react";

type SyncableItem = {
  status: string;
};

interface BuildSyncButtonPropsOptions<T extends SyncableItem> {
  loggedUserData: UserInfo | undefined;
  selectedItems: T[];
  worklogId: string;
  extractIdentifier: (item: T) => string;
  jiraSyncSSE: JiraSyncSSEReturn;
  isEntry?: boolean;
}

function buildSyncButtonProps<T extends SyncableItem>({
  loggedUserData,
  selectedItems,
  worklogId,
  extractIdentifier,
  jiraSyncSSE,
  isEntry = false,
}: BuildSyncButtonPropsOptions<T>): TableActionButtonProps[] {
  const hasSynced = selectedItems.some((e) => e.status === WORKLOG_STATUS.SYNCED);
  const hasNotSynced = selectedItems.some((e) => e.status === WORKLOG_STATUS.NOT_SYNCED);
  const hasInProgress = selectedItems.some(
    (e) =>
      e.status === WORKLOG_STATUS.IN_QUEUE ||
      e.status === WORKLOG_STATUS.SYNC_IN_PROGRESS ||
      e.status === WORKLOG_STATUS.UNSYNC_IN_PROGRESS,
  );

  const syncedItems = selectedItems.filter((e) => e.status === WORKLOG_STATUS.SYNCED);
  const notSyncedItems = selectedItems.filter((e) => e.status === WORKLOG_STATUS.NOT_SYNCED);
  const toIds = (arr: T[]) => arr.map(extractIdentifier);

  const createPayload = (items: T[], sync: boolean): SyncPayload =>
    isEntry
      ? { worklogId: worklogId!, entryIds: toIds(items), sync }
      : { worklogId: worklogId!, taskNames: toIds(items), sync };

  let actions: TableActionButtonProps[] = [];

  if (!loggedUserData?.jiraLinked) {
    return [{ label: "Sync to Jira (Jira not linked)", icon: <CalendarOff />, disabled: true }];
  }

  if (selectedItems.length === 0) {
    return [{ label: "Sync to Jira", icon: <CalendarSync />, disabled: true }];
  }

  if (hasInProgress || jiraSyncSSE.isRequestingSync) {
    return [
      { label: "Actions Unavailable", icon: <CalendarOff />, disabled: true, loading: jiraSyncSSE.isRequestingSync },
    ];
  }

  if (hasSynced && hasNotSynced) {
    return [
      {
        label: "Bulk Actions",
        icon: <CalendarCog />,
        options: [
          {
            label: `Sync to Jira (${notSyncedItems.length})`,
            icon: <CalendarSync />,
            onClick: () => jiraSyncSSE.triggerSync(createPayload(notSyncedItems, true)),
            customClasses: ["sync"],
          },
          {
            label: `Unsync from Jira (${syncedItems.length})`,
            icon: <CalendarX2 />,
            onClick: () => jiraSyncSSE.triggerSync(createPayload(syncedItems, false)),
            customClasses: ["unsync"],
          },
        ],
      },
    ];
  }

  if (hasNotSynced) {
    return [
      {
        label: `Sync to Jira (${notSyncedItems.length})`,
        icon: <CalendarSync />,
        onClick: () => jiraSyncSSE.triggerSync(createPayload(notSyncedItems, true)),
      },
    ];
  }

  if (hasSynced) {
    return [
      {
        label: `Unsync from Jira (${syncedItems.length})`,
        icon: <CalendarX2 />,
        onClick: () => jiraSyncSSE.triggerSync(createPayload(syncedItems, false)),
      },
    ];
  }

  return actions;
}

export default buildSyncButtonProps;
