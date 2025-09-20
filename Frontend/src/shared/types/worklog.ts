import type { ModalProps, TableProps } from "antd";
import type { StatusBadgeProps, TrackeraTableEntity } from "./global";

type WorkLogEvaluationType = "EXCELLENT" | "GOOD" | "MODERATE" | "POOR";

type WorkLogStatusType = "SYNCED" | "PARTIALLY" | "NOT_SYNCED";

interface Worklog extends TrackeraTableEntity {
  name: string;
  totalHours: string;
  workDate: string;
  evaluation: WorkLogEvaluationType;
  status: WorkLogStatusType;
}

interface WorklogTask extends TrackeraTableEntity {
  taskName: string;
  taskUrl: string;
  totalHours: string;
  totalTime: number;
  status: WorkLogStatusType;
}

interface WorklogEntry extends TrackeraTableEntity {
  fromTime: string;
  toTime: string;
  duration: string;
  description: string;
  status: WorkLogStatusType;
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
};

interface WorklogTableActionButtonProps {
  label: string;
  icon: React.ReactNode;
  customClasses?: string[];
  disabled?: boolean;
  onClick: () => void;
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
  StatusBadgeProps,
  WorklogTableActionButtonProps,
  WorklogTableProps,
  WorklogModalProps,
  ManageWorkLogModalProps,
  WorkLogSummaryCard,
  WorkLogSearchFilter,
  WorkLogsFilterProps,
};

export { worklogEvaluationMetadata, statusMetadata };
