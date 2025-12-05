import type { ModalProps, TableProps } from "antd";
import type { StatusBadgeProps, TrackeraTableEntity } from "./global";

type WorkLogEvaluationType = "EXCELLENT" | "GOOD" | "MODERATE" | "POOR";

type WorkLogStatusType = "SYNCED" | "PARTIALLY" | "NOT_SYNCED" | "SYNC_IN_PROGRESS" | "UNSYNC_IN_PROGRESS";

interface Worklog extends TrackeraTableEntity {
  name: string;
  totalTime: string;
  workDate: string;
  evaluation: WorkLogEvaluationType;
  status: WorkLogStatusType;
}

interface WorklogTask extends TrackeraTableEntity {
  taskName: string;
  taskUrl: string;
  totalHours: string;
  totalMinutes: number;
  status: WorkLogStatusType;
}

interface WorklogEntry extends TrackeraTableEntity {
  fromTime: string;
  toTime: string;
  duration: string;
  description: string;
  status: WorkLogStatusType;
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
  setFetchWorkLog: (fetch: boolean) => void;
  setFetchSummary: (fetch: boolean) => void;
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
  filters: Record<string, any>;
  setFilters: (filters: Record<string, any>) => void;
  setFetchWorkLog: (fetch: boolean) => void;
  jiraLinked: boolean;
}

export type {
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
};

export { worklogEvaluationMetadata, statusMetadata };
