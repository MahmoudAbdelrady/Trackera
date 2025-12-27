import type { ModalProps, TableProps } from "antd";
import type { StatusBadgeProps, TrackeraTableEntity } from "./global";
import type { UserInfo } from "./auth";

const WorkLogEvaluation = {
  EXCELLENT: "EXCELLENT",
  GOOD: "GOOD",
  MODERATE: "MODERATE",
  POOR: "POOR",
} as const;

const WorkLogStatus = {
  SYNCED: "SYNCED",
  PARTIALLY: "PARTIALLY",
  NOT_SYNCED: "NOT_SYNCED",
  SYNC_IN_PROGRESS: "SYNC_IN_PROGRESS",
  UNSYNC_IN_PROGRESS: "UNSYNC_IN_PROGRESS",
} as const;

// type JiraSyncEventType = "ALL" | "WORKLOG" | "TASK" | "ENTRY";

const JiraSyncEvent = {
  ALL: "ALL",
  WORKLOG: "WORKLOG",
  TASK: "TASK",
  ENTRY: "ENTRY",
} as const;

type WorkLogStatusType = (typeof WorkLogStatus)[keyof typeof WorkLogStatus];

type WorkLogEvaluationType = (typeof WorkLogEvaluation)[keyof typeof WorkLogEvaluation];

type JiraSyncEventType = (typeof JiraSyncEvent)[keyof typeof JiraSyncEvent];

interface SyncPayload {
  workLogId?: string;
  taskNames?: string[];
  entryIds?: string[];
  sync: boolean;
}

interface Worklog extends TrackeraTableEntity {
  name: string;
  totalTime: string;
  workDate: string;
  evaluation: WorkLogEvaluationType;
  status: WorkLogStatusType;
  hasError: boolean;
}

interface WorklogTask extends TrackeraTableEntity {
  taskName: string;
  taskUrl: string;
  totalHours: string;
  totalMinutes: number;
  status: WorkLogStatusType;
  hasError: boolean;
}

interface WorklogEntry extends TrackeraTableEntity {
  fromTime: string;
  toTime: string;
  duration: string;
  description: string;
  status: WorkLogStatusType;
  syncError?: string;
}

interface WorklogSelection {
  taskNames?: string[];
  entryIds?: string[];
}

interface WorklogError extends TrackeraTableEntity {
  row: number;
  error: string;
}

const worklogEvaluationMetadata: Record<WorkLogEvaluationType, StatusBadgeProps> = {
  EXCELLENT: { label: "Excellent", type: "main" },
  GOOD: { label: "Good", type: "success" },
  MODERATE: { label: "Moderate", type: "warning" },
  POOR: { label: "Poor", type: "danger" },
};

const statusMetadata: Record<WorkLogStatusType, StatusBadgeProps> = {
  SYNCED: { label: "Synced", type: "success" },
  PARTIALLY: { label: "Partially", type: "warning" },
  NOT_SYNCED: { label: "Not Synced", type: "danger" },
  SYNC_IN_PROGRESS: { label: "Sync in Progress", type: "warning" },
  UNSYNC_IN_PROGRESS: { label: "Unsync in Progress", type: "warning" },
};

interface WorklogTableActionButtonOptions {
  label: string;
  onClick: () => void;
  disabled?: boolean;
  icon?: React.ReactNode;
  customClasses?: string[];
}

interface WorklogTableActionButtonProps {
  label: string;
  icon?: React.ReactNode;
  customClasses?: string[];
  disabled?: boolean;
  onClick?: () => void;
  options?: WorklogTableActionButtonOptions[];
}

interface WorklogTableProps<T = TrackeraTableEntity> {
  properties: TableProps<T>;
  actionButtons?: WorklogTableActionButtonProps[];
}

interface WorklogModalProps {
  title: string;
  properties: ModalProps;
  children: React.ReactNode;
}

interface ManageWorkLogModalProps {
  setIsOpen: (isOpen: boolean) => void;
  refreshWorkLogData: () => void;
  selectedWorkLog?: Worklog;
  setSelectedWorkLog?: (worklog: Worklog | undefined) => void;
  jiraLinked: boolean;
}

interface WorkLogSummaryCard {
  label: string;
  subLabel: string;
  code: string;
  value: string;
}

interface WorkLogSearchFilter {
  operator: string | undefined | null;
  value: any | undefined | null;
  secondValue?: any | undefined | null;
}

interface WorkLogsFilterProps {
  setFilters: (filters: Record<string, any>) => void;
  jiraLinked: boolean;
}

interface ActionButtonsProps {
  record: Worklog | WorklogTask | WorklogEntry;
  jiraLinked: boolean;
  viewLink?: string;
  syncParams: SyncPayload;
  onSync: (params: SyncPayload) => void;
  onEdit?: () => void;
  onView?: () => void;
  onDelete: () => void;
}

interface WorklogTaskEntriesProps {
  loggedUserData: UserInfo;
  worklogId: string;
  selectedTask: WorklogTask;
  worklogEntries: WorklogEntry[];
  setWorklogEntries: (entries: WorklogEntry[]) => void;
  refetchData: () => void;
  triggerSync: (params: SyncPayload) => void;
  onCloseHandler: () => void;
}

type JiraSyncEventProps = {
  type: JiraSyncEventType;
  logId: string;
  taskNames?: string[];
  entryIds?: string[];
  status: WorkLogStatusType;
  syncError?: string;
};

export type {
  SyncPayload,
  WorkLogEvaluationType,
  WorkLogStatusType,
  Worklog,
  WorklogTask,
  WorklogEntry,
  WorklogSelection,
  WorklogError,
  StatusBadgeProps,
  WorklogTableActionButtonProps,
  WorklogTableProps,
  WorklogModalProps,
  ManageWorkLogModalProps,
  WorkLogSummaryCard,
  WorkLogSearchFilter,
  WorkLogsFilterProps,
  ActionButtonsProps,
  WorklogTaskEntriesProps,
  JiraSyncEventType,
  JiraSyncEventProps,
};

export { WorkLogStatus, WorkLogEvaluation, worklogEvaluationMetadata, statusMetadata, JiraSyncEvent };
