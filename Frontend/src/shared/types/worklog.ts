import type { ModalProps, TableProps } from "antd";

type WorkLogEvaluationType = "EXCELLENT" | "GOOD" | "MODERATE" | "POOR";

type WorkLogStatusType = "SYNCED" | "PARTIALLY" | "NOT_SYNCED";

interface Worklog {
  logId: string;
  name: string;
  totalHours: string;
  workDate: string;
  evaluation: WorkLogEvaluationType;
  status: WorkLogStatusType;
}

interface WorklogTask {
  id: number;
  taskName: string;
  taskUrl: string;
  totalHours: number;
  status: WorkLogStatusType;
}

interface WorklogEntry {
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

interface WorklogTableProps<T = Worklog | WorklogTask | WorklogEntry | WorklogError> {
  properties: TableProps<T>;
  actionButtons: WorklogTableActionButtonProps[];
}

interface WorklogModalProps {
  title: string;
  properties: ModalProps;
  children: React.ReactNode;
}

interface ManageWorkLogModalProps {
  setIsOpen: (isOpen: boolean) => void;
  setFetchWorkLog: (fetch: boolean) => void;
  setFetchSummary: (fetch: boolean) => void;
  selectedWorkLog?: Worklog;
  setSelectedWorkLog?: (worklog: Worklog | undefined) => void;
}

interface WorkLogSummaryCard {
  label: string;
  subLabel: string;
  code: string;
  value: string;
}

interface WorkLogSearchFilter {
  fieldName: string;
  operator?: string | undefined | null;
  value: any | undefined | null;
  extraValue?: any | undefined | null;
}

interface WorkLogsFilterProps {
  filters: WorkLogSearchFilter[];
  setFilters: (filters: WorkLogSearchFilter[]) => void;
  setFetchWorkLog: (fetch: boolean) => void;
}

export type {
  WorkLogEvaluationType,
  WorkLogStatusType,
  Worklog,
  WorklogTask,
  WorklogEntry,
  WorklogError,
  LogMeta,
  WorklogTableActionButtonProps,
  WorklogTableProps,
  WorklogModalProps,
  ManageWorkLogModalProps,
  WorkLogSummaryCard,
  WorkLogSearchFilter,
  WorkLogsFilterProps,
};

export { evaluationMetadata, statusMetadata };
