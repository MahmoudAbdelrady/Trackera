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

interface Worklog {
  id: string;
  name: string;
  totalTime: string;
  workDate: string;
  evaluation: WorkLogEvaluationType;
  status: WorkLogStatusType;
  hasError: boolean;
}

interface WorklogTask {
  taskName: string;
  taskUrl: string;
  totalHours: string;
  totalMinutes: number;
  status: WorkLogStatusType;
  hasError: boolean;
}

interface WorklogEntry {
  id: string;
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

interface WorklogError {
  row: number;
  error: string;
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
  WorkLogSearchFilter,
  JiraSyncEventType,
  JiraSyncEventProps,
};

export { WorkLogStatus, WorkLogEvaluation, JiraSyncEvent };
