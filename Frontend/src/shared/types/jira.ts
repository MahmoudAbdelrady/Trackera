import type { StatusBadgeProps, TrackeraTableEntity } from "./global";

type JiraTaskEvaluationType = "ON_TIME" | "OVERESTIMATED";

const jiraTaskEvaluationMetadata: Record<JiraTaskEvaluationType, StatusBadgeProps> = {
  ON_TIME: { label: "On Time", type: "success" },
  OVERESTIMATED: { label: "Overestimated", type: "warning" },
};

interface JiraTask extends TrackeraTableEntity {
  taskName: string;
  taskUrl: string;
  project: Record<string, string>;
  status: Record<string, string>;
  timeTracking: Record<string, any>;
}

interface JiraSite {
  id: string;
  url: string;
  name: string;
  avatarUrl: string;
}

export type { JiraTask, JiraTaskEvaluationType, JiraSite };

export { jiraTaskEvaluationMetadata };
