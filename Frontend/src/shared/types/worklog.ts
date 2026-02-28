const WORKLOG_EVALUATION = {
  EXCELLENT: "EXCELLENT",
  GOOD: "GOOD",
  MODERATE: "MODERATE",
  POOR: "POOR",
} as const;

const WORKLOG_STATUS = {
  SYNCED: "SYNCED",
  PARTIALLY: "PARTIALLY",
  NOT_SYNCED: "NOT_SYNCED",
  IN_QUEUE: "IN_QUEUE",
  SYNC_IN_PROGRESS: "SYNC_IN_PROGRESS",
  UNSYNC_IN_PROGRESS: "UNSYNC_IN_PROGRESS",
} as const;

const JIRA_SYNC_EVENT = {
  ALL: "ALL",
  WORKLOG: "WORKLOG",
  TASK: "TASK",
  ENTRY: "ENTRY",
} as const;

type WorklogStatusType = (typeof WORKLOG_STATUS)[keyof typeof WORKLOG_STATUS];

type WorklogEvaluationType = (typeof WORKLOG_EVALUATION)[keyof typeof WORKLOG_EVALUATION];

type JiraSyncEventType = (typeof JIRA_SYNC_EVENT)[keyof typeof JIRA_SYNC_EVENT];

interface SyncPayload {
  worklogId?: string;
  taskNames?: string[];
  entryIds?: string[];
  sync: boolean;
}

interface Worklog {
  id: string;
  name: string;
  totalTime: string;
  workDate: string;
  evaluation: WorklogEvaluationType;
  status: WorklogStatusType;
  hasError: boolean;
}

interface WorklogTask {
  taskName: string;
  taskUrl: string;
  totalHours: string;
  totalMinutes: number;
  status: WorklogStatusType;
  hasError: boolean;
}

interface WorklogEntry {
  id: string;
  fromTime: string;
  toTime: string;
  duration: string;
  description: string;
  status: WorklogStatusType;
  syncError?: string;
}

interface WorklogDetailNewDataPayload {
  name: string;
  startTime?: string;
  endTime?: string;
  duration?: string;
  description?: string;
}

interface UpdateWorkLogDetailPayloadDTO {
  taskName?: string;
  entryId?: string;
  isTask: boolean;
  syncToJira: boolean;
  newData: WorklogDetailNewDataPayload;
}

interface WorklogSummaryCard {
  label: string;
  subLabel: string;
  code: string;
  value: string;
}

interface WorklogSummaryResponse {
  currentMonthSummary: WorklogSummaryCard[];
  previousMonthLoggedHours: string;
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
  status: WorklogStatusType;
  syncError?: string;
};

interface UpdateWorklogDetailResponse {
  message: string;
  worklogInfo: Worklog;
  task: WorklogTask;
  entry?: WorklogEntry;
}

export type {
  SyncPayload,
  WorklogEvaluationType,
  WorklogStatusType,
  Worklog,
  WorklogTask,
  WorklogEntry,
  UpdateWorkLogDetailPayloadDTO,
  UpdateWorklogDetailResponse,
  WorklogSelection,
  WorklogError,
  WorkLogSearchFilter,
  JiraSyncEventType,
  JiraSyncEventProps,
  WorklogSummaryCard,
  WorklogSummaryResponse,
};

export { WORKLOG_STATUS, WORKLOG_EVALUATION, JIRA_SYNC_EVENT };
