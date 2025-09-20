import type { StatusBadgeProps, TrackeraTableEntity } from "./global";

type JiraTaskEvaluationType = "ON_TIME" | "OVERESTIMATED";

const jiraTaskEvaluationMetadata: Record<JiraTaskEvaluationType, StatusBadgeProps> = {
  ON_TIME: { label: "On Time", type: "success" },
  OVERESTIMATED: { label: "Overestimated", type: "warning" },
};

interface JiraTask extends TrackeraTableEntity {
  taskName: string;
  taskUrl?: string;
  loggedHours: number;
  remainingHours: number;
  evaluation?: JiraTaskEvaluationType;
  status: string;
  notes?: string;
}

export type { JiraTask, JiraTaskEvaluationType };

export { jiraTaskEvaluationMetadata };
