import type { ModalProps, TableProps } from "antd";

type WorkLogEvaluationType = "EXCELLENT" | "GOOD" | "MODERATE" | "POOR";

type WorkLogStatusType = "SYNCED" | "PARTIALLY" | "UNSYNCED";

interface Worklog {
  id: number;
  logName: string;
  totalHours: number;
  date: string;
  evaluation: WorkLogEvaluationType;
  status: WorkLogStatusType;
}

interface WorklogDetails {
  id: number;
  taskName: string;
  taskUrl: string;
  totalHours: number;
  status: WorkLogStatusType;
}

interface WorklogTask {
  id: number;
  fromTime: string;
  toTime: string;
  description: string;
  status: WorkLogStatusType;
}

interface LogMeta {
  label: string;
  className: string;
}

const evaluationMetadata: Record<WorkLogEvaluationType, LogMeta> = {
  EXCELLENT: { label: "Excellent", className: "excellent" },
  GOOD: { label: "Good", className: "good" },
  MODERATE: { label: "Moderate", className: "moderate" },
  POOR: { label: "Poor", className: "poor" },
};

const statusMetadata: Record<WorkLogStatusType, LogMeta> = {
  SYNCED: { label: "Synced", className: "synced" },
  PARTIALLY: { label: "Partially", className: "partially" },
  UNSYNCED: { label: "Unsynced", className: "unsynced" },
};

interface WorklogTableActionButtonProps {
  label: string;
  icon: React.ReactNode;
  customClasses?: string[];
  disabled?: boolean;
  onClick: () => void;
}

interface WorklogTableProps<T = Worklog | WorklogDetails | WorklogTask> {
  properties: TableProps<T>;
  actionButtons: WorklogTableActionButtonProps[];
}

interface WorklogModalProps {
  title: string;
  properties: ModalProps;
  children: React.ReactNode;
}

export type {
  WorkLogEvaluationType,
  WorkLogStatusType,
  Worklog,
  WorklogDetails,
  WorklogTask,
  LogMeta,
  WorklogTableActionButtonProps,
  WorklogTableProps,
  WorklogModalProps,
};

export { evaluationMetadata, statusMetadata };
