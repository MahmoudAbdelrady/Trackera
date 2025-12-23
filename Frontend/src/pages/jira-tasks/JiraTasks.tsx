import { Button, Tabs, type TableProps, type TabsProps } from "antd";
import { AppLayout, TrackeraTable, StatusBadge, AccessDenied } from "../../components";
import { ExternalLink, RefreshCw } from "lucide-react";
import { jiraTaskEvaluationMetadata, type JiraTask, type JiraTaskEvaluationType } from "../../shared/types";
import { Link } from "react-router-dom";
import trackeraTableClasses from "../../components/trackera-table/scss/trackera-table.module.css";
import classes from "./scss/jira-tasks.module.css";
import { showErrorToast } from "../../utils/toast-handler/showToast";
import { useEffect, useState } from "react";
import { userQueries } from "../../state/queries";
import { jiraApis } from "../../state/api";

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
          <Link to={record.taskUrl} className={trackeraTableClasses.task_link} target="_blank">
            {record.taskName}
            <ExternalLink className={trackeraTableClasses.link_icon} />
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
                <img src={project.icon} alt={project.name} />
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
        return <StatusBadge badgeProps={{ label: status.name, type: getStatusType(status.category) }} />;
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
          <StatusBadge badgeProps={jiraTaskEvaluationMetadata[timeTracking.evaluation as JiraTaskEvaluationType]} />
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

  const items: TabsProps["items"] = [
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
        />
      ),
    },
  ];

  useEffect(() => {
    if (loggedUserData?.jiraLinked) {
      fetchJiraTasks();
    }
  }, []);

  const fetchJiraTasks = async (forceUpdate: boolean = false) => {
    setIsLoading(true);
    try {
      const result = await jiraApis.getJiraTasks(forceUpdate);
      setLastUpdated(result.lastUpdated);
      setJiraTasks(result.tasks);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsLoading(false);
  };

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
        <AccessDenied />
      )}
    </AppLayout>
  );
};

export default JiraTasks;
