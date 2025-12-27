interface JiraTask {
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

export type { JiraTask, JiraSite };
