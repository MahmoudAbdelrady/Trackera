import type { StatusBadgeProps, TrackeraTableEntity } from "./global";

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

interface WorkLogSearchFilter {
  operator: string | undefined | null;
  value: any | undefined | null;
  secondValue?: any | undefined | null;
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
  WorkLogSearchFilter,
  JiraSyncEventType,
  JiraSyncEventProps,
};

export { WorkLogStatus, WorkLogEvaluation, worklogEvaluationMetadata, statusMetadata, JiraSyncEvent };
