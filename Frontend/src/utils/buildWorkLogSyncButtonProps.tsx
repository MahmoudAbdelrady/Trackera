import type { TableActionButtonProps } from "../components";
import { WorkLogStatus, type SyncPayload } from "../shared/types";
import type { UserInfo } from "../shared/types/auth";
import { CalendarCog, CalendarOff, CalendarSync, CalendarX2 } from "lucide-react";

type SyncableItem = {
  status: string;
};

interface BuildSyncButtonPropsOptions<T extends SyncableItem> {
  loggedUserData: UserInfo | undefined;
  selectedItems: T[];
  extractIdentifier: (item: T) => string;
  triggerSync: (payload: SyncPayload) => void;
  isEntry?: boolean;
}

function buildSyncButtonProps<T extends SyncableItem>({
  loggedUserData,
  selectedItems,
  extractIdentifier,
  triggerSync,
  isEntry = false,
}: BuildSyncButtonPropsOptions<T>): TableActionButtonProps[] {
  const hasSynced = selectedItems.some((e) => e.status === WorkLogStatus.SYNCED);
  const hasNotSynced = selectedItems.some((e) => e.status === WorkLogStatus.NOT_SYNCED);
  const hasInProgress = selectedItems.some(
    (e) => e.status === WorkLogStatus.SYNC_IN_PROGRESS || e.status === WorkLogStatus.UNSYNC_IN_PROGRESS
  );

  const syncedItems = selectedItems.filter((e) => e.status === WorkLogStatus.SYNCED);
  const notSyncedItems = selectedItems.filter((e) => e.status === WorkLogStatus.NOT_SYNCED);

  const toIds = (arr: T[]) => arr.map(extractIdentifier);

  const createPayload = (items: T[], sync: boolean): SyncPayload =>
    isEntry ? { entryIds: toIds(items), sync } : { taskNames: toIds(items), sync };

  let actions: TableActionButtonProps[] = [];

  if (!loggedUserData?.jiraLinked) {
    return [{ label: "Sync to Jira (Jira not linked)", icon: <CalendarOff />, disabled: true }];
  }

  if (selectedItems.length === 0) {
    return [{ label: "Sync to Jira", icon: <CalendarSync />, disabled: true }];
  }

  if (hasInProgress) {
    return [{ label: "Actions Unavailable", icon: <CalendarOff />, disabled: true }];
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
            onClick: () => triggerSync(createPayload(notSyncedItems, true)),
            customClasses: ["sync"],
          },
          {
            label: `Unsync from Jira (${syncedItems.length})`,
            icon: <CalendarX2 />,
            onClick: () => triggerSync(createPayload(syncedItems, false)),
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
        onClick: () => triggerSync(createPayload(notSyncedItems, true)),
      },
    ];
  }

  if (hasSynced) {
    return [
      {
        label: `Unsync from Jira (${syncedItems.length})`,
        icon: <CalendarX2 />,
        onClick: () => triggerSync(createPayload(syncedItems, false)),
      },
    ];
  }

  return actions;
}

export default buildSyncButtonProps;
