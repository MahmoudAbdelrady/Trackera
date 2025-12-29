import type { WorklogEvaluationType, WorklogStatusType } from "../../shared/types";
import type { StatusBadgeProps } from "../";

const worklogEvaluationMetadata: Record<WorklogEvaluationType, StatusBadgeProps> = {
  EXCELLENT: { label: "Excellent", type: "main" },
  GOOD: { label: "Good", type: "success" },
  MODERATE: { label: "Moderate", type: "warning" },
  POOR: { label: "Poor", type: "danger" },
};

const statusMetadata: Record<WorklogStatusType, StatusBadgeProps> = {
  SYNCED: { label: "Synced", type: "success" },
  PARTIALLY: { label: "Partially", type: "warning" },
  NOT_SYNCED: { label: "Not Synced", type: "danger" },
  SYNC_IN_PROGRESS: { label: "Sync in Progress", type: "warning" },
  UNSYNC_IN_PROGRESS: { label: "Unsync in Progress", type: "warning" },
};

export { worklogEvaluationMetadata, statusMetadata };
