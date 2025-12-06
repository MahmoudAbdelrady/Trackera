import type { WorklogTableActionButtonProps } from "../shared/types";
import type { UserInfo } from "../state/api/user";
import { CalendarCog, CalendarOff, CalendarSync, CalendarX2 } from "lucide-react";
type SyncableItem = {
  status: string;
};

function buildSyncButtonProps<T extends SyncableItem>({
  loggedUserData,
  selectedItems,
  extractIdentifier,
  performSync,
}: {
  loggedUserData: UserInfo | undefined;
  selectedItems: T[];
  extractIdentifier: (item: T) => string;
  performSync: (identifiers: string[], sync: boolean) => void;
}): WorklogTableActionButtonProps[] {
  const hasSynced = selectedItems.some((e) => e.status === "SYNCED");
  const hasNotSynced = selectedItems.some((e) => e.status === "NOT_SYNCED");
  const hasInProgress = selectedItems.some((e) => ["SYNC_IN_PROGRESS", "UNSYNC_IN_PROGRESS"].includes(e.status));

  const syncedItems = selectedItems.filter((e) => e.status === "SYNCED");
  const notSyncedItems = selectedItems.filter((e) => e.status === "NOT_SYNCED");

  const toIds = (arr: T[]) => arr.map(extractIdentifier);

  let actions: WorklogTableActionButtonProps[] = [];

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
          { label: `Sync to Jira (${notSyncedItems.length})`, icon: <CalendarSync />, onClick: () => performSync(toIds(notSyncedItems), true), customClasses: ["sync"] },
          { label: `Unsync from Jira (${syncedItems.length})`, icon: <CalendarX2 />, onClick: () => performSync(toIds(syncedItems), false), customClasses: ["unsync"] },
        ],
      },
    ];
  }

  if (hasNotSynced) {
    return [
      {
        label: `Sync to Jira (${notSyncedItems.length})`,
        icon: <CalendarSync />,
        onClick: () => performSync(toIds(notSyncedItems), true),
      },
    ];
  }

  if (hasSynced) {
    return [
      {
        label: `Unsync from Jira (${syncedItems.length})`,
        icon: <CalendarX2 />,
        onClick: () => performSync(toIds(syncedItems), false),
      },
    ];
  }

  return actions;
}

export default buildSyncButtonProps;
