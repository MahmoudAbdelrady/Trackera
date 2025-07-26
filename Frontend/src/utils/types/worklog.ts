import type { ModalProps, TableProps } from "antd";

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
  onClick: () => void;
}

interface WorklogTableProps {
  columns: TableProps<Worklog | WorklogDetails>["columns"];
  dataSource: Worklog[] | WorklogDetails[];
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
  LogMeta,
  WorklogTableActionButtonProps,
  WorklogTableProps,
  WorklogModalProps,
};

export { evaluationMetadata, statusMetadata };
