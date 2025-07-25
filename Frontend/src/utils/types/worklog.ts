type WorkLogEvaluationType = "EXCELLENT" | "GOOD" | "MODERATE" | "POOR";

type WorkLogStatusType = "SYNCED" | "PARTIALLY" | "UNSYNCED";

interface Worklog {
  logId: number;
  logName: string;
  totalHours: number;
  date: string;
  evaluation: WorkLogEvaluationType;
  status: WorkLogStatusType;
}

interface WorklogDetails {
  detailId: number;
  taskName: string;
  taskUrl: string;
  totalHours: number;
  status: WorkLogStatusType;
}

interface LogMeta {
  label: string;
  className: string;
}

export type {
  WorkLogEvaluationType,
  WorkLogStatusType,
  Worklog,
  WorklogDetails,
  LogMeta,
};
