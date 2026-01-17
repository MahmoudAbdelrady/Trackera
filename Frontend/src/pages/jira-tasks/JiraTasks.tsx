import { Button, Tabs, type TableProps, type TabsProps } from "antd";
import { TrackeraTable, StatusBadge, AccessDenied, type StatusBadgeProps, AvatarWithFallback } from "../../components";
import { ExternalLink, RefreshCw } from "lucide-react";
import { type JiraTask } from "../../shared/types";
import { Link } from "react-router-dom";
import classes from "./scss/jira-tasks.module.css";
import { showErrorToast } from "../../utils/toast-handler/showToast";
import { useCallback, useEffect, useMemo, useState } from "react";
import { userQueries } from "../../state/queries";
import { jiraApis } from "../../state/api";
import { AppLayout } from "../../layouts";

type JiraTaskEvaluationType = "ON_TIME" | "OVERESTIMATED";

const jiraTaskEvaluationMetadata: Record<JiraTaskEvaluationType, StatusBadgeProps> = {
  ON_TIME: { label: "On Time", type: "success" },
  OVERESTIMATED: { label: "Overestimated", type: "warning" },
};

const JiraTasks = () => {
  const { data: loggedUserData } = userQueries.useMeQuery();

  const [isLoading, setIsLoading] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<string | null>(null);
  const [jiraTasks, setJiraTasks] = useState<Record<string, any> | null>(null);

  const jiraTasksColumns: TableProps<JiraTask>["columns"] = [
    {
      title: "Task Name",
      dataIndex: "taskName",
      key: "taskName",
      render: (_, record) => {
        return (
          <Link to={record.taskUrl} className={classes.task_link} target="_blank">
            {record.taskName}
            <ExternalLink className={classes.link_icon} />
          </Link>
        );
      },
    },
    {
      title: "Project",
      dataIndex: "project",
      key: "project",
      render: (_, { project }) => {
        return (
          <div className={classes.project_cell}>
            {project ? (
              <>
                <AvatarWithFallback
                  src={project.icon}
                  alt={project.name}
                  fallbackText={project.name}
                  width="28px"
                  height="28px"
                />
                <span>{project.name}</span>
              </>
            ) : (
              "-"
            )}
          </div>
        );
      },
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (_, { status }) => {
        return <StatusBadge label={status.name} type={getStatusType(status.category)} />;
      },
    },
    {
      title: "Original Estimate",
      dataIndex: "originalEstimate",
      key: "originalEstimate",
      render: (_, { timeTracking }) => {
        return timeTracking.originalEstimate || "-";
      },
    },
    {
      title: "Logged Time",
      dataIndex: "loggedTime",
      key: "loggedTime",
      render: (_, { timeTracking }) => {
        return timeTracking.loggedTime || "-";
      },
    },
    {
      title: "Remaining Time",
      dataIndex: "remainingTime",
      key: "remainingTime",
      render: (_, { timeTracking }) => {
        return timeTracking.remainingTime || "-";
      },
    },
    {
      title: "Evaluation",
      dataIndex: "evaluation",
      key: "evaluation",
      render: (_, { timeTracking }) => {
        return timeTracking.evaluation ? (
          <StatusBadge {...jiraTaskEvaluationMetadata[timeTracking.evaluation as JiraTaskEvaluationType]} />
        ) : (
          "-"
        );
      },
    },
    {
      title: "Notes",
      dataIndex: "notes",
      key: "notes",
      render: (_, { timeTracking }) => {
        return timeTracking.notes || "-";
      },
    },
  ];

  const getStatusType = (statusKey: string) => {
    switch (statusKey) {
      case "new":
        return "default";
      case "indeterminate":
        return "main";
      case "done":
        return "success";
      default:
        return "default";
    }
  };

  const items: TabsProps["items"] = useMemo(
    () => [
      {
        key: "1",
        label: `Current Tasks (${jiraTasks?.currentTasks?.total || 0})`,
        children: (
          <TrackeraTable<JiraTask>
            properties={{
              columns: jiraTasksColumns,
              dataSource: jiraTasks?.currentTasks?.data || [],
              pagination: { style: { marginRight: "16px" } },
              loading: isLoading,
            }}
            rowKey={(record) => record.taskName}
          />
        ),
      },
      {
        key: "2",
        label: `Overestimated Tasks (${jiraTasks?.overestimatedTasks?.total || 0})`,
        children: (
          <TrackeraTable<JiraTask>
            properties={{
              columns: jiraTasksColumns.filter((col) => col.key !== "evaluation"),
              dataSource: jiraTasks?.overestimatedTasks?.data || [],
              pagination: { style: { marginRight: "16px" } },
              loading: isLoading,
            }}
            rowKey={(record) => record.taskName}
          />
        ),
      },
    ],
    [jiraTasksColumns, jiraTasks, isLoading],
  );

  const fetchJiraTasks = useCallback(async (forceUpdate: boolean = false) => {
    setIsLoading(true);
    try {
      const result = await jiraApis.getJiraTasks(forceUpdate);
      setLastUpdated(result.lastUpdated);
      setJiraTasks(result.tasks);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsLoading(false);
  }, []);

  useEffect(() => {
    if (loggedUserData?.jiraLinked) {
      fetchJiraTasks();
    }
  }, [loggedUserData?.jiraLinked, fetchJiraTasks]);

  return (
    <AppLayout>
      {loggedUserData?.jiraLinked ? (
        <>
          <div className={classes.refresh_container}>
            <Button icon={<RefreshCw />} onClick={() => fetchJiraTasks(true)}>
              Refresh
            </Button>
            {lastUpdated && <div className={classes.last_refresh}>Last Updated: {lastUpdated}</div>}
          </div>
          <div className={classes.jira_tasks_container}>
            <Tabs defaultActiveKey="1" items={items} />
          </div>
        </>
      ) : (
        <AccessDenied message="Jira is not linked to your account. Please link Jira to access this page." fitParent />
      )}
    </AppLayout>
  );
};

export default JiraTasks;
