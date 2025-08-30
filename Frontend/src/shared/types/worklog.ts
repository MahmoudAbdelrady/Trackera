import type { ModalProps, TableProps } from "antd";

type WorkLogEvaluationType = "EXCELLENT" | "GOOD" | "MODERATE" | "POOR";

type WorkLogStatusType = "SYNCED" | "PARTIALLY" | "NOT_SYNCED";

interface Worklog {
  id: number;
  name: string;
  totalHours: string;
  workDate: string;
  evaluation: WorkLogEvaluationType;
  status: WorkLogStatusType;
}

interface WorklogEntry {
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

interface WorklogError {
  row: number;
  error: string;
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
  NOT_SYNCED: { label: "Not Synced", className: "not_synced" },
};

interface WorklogTableActionButtonProps {
  label: string;
  icon: React.ReactNode;
  customClasses?: string[];
  disabled?: boolean;
  onClick: () => void;
}

interface WorklogTableProps<
  T = Worklog | WorklogEntry | WorklogTask | WorklogError
> {
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
  WorklogEntry,
  WorklogTask,
  WorklogError,
  LogMeta,
  WorklogTableActionButtonProps,
  WorklogTableProps,
  WorklogModalProps,
};

export { evaluationMetadata, statusMetadata };
