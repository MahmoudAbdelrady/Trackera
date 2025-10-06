import { Button, Tabs, type TableProps, type TabsProps } from "antd";
import { AppLayout, TrackeraTable, StatusBadge } from "../../components";
import { ExternalLink, RefreshCw } from "lucide-react";
import { jiraTaskEvaluationMetadata, type JiraTask, type JiraTaskEvaluationType } from "../../shared/types";
import { Link } from "react-router-dom";
import trackeraTableClasses from "../../components/trackera-table/scss/trackera-table.module.css";
import classes from "./scss/jira-tasks.module.css";
import { showErrorToast } from "../../utils/toast-handler/showToast";
import requestInstance from "../../shared/axios/request-instance";
import { useEffect, useState } from "react";

const JiraTasks = () => {
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
      title: "Original Estimate",
      dataIndex: "originalEstimate",
      key: "originalEstimate",
      render: (_, { timeTracking }) => {
        return timeTracking.originalEstimate || "-";
      },
    },
    {
      title: "Logged Hours",
      dataIndex: "loggedHours",
      key: "loggedHours",
      render: (_, { timeTracking }) => {
        return timeTracking.loggedHours || "-";
      },
    },
    {
      title: "Remaining Hours",
      dataIndex: "remainingHours",
      key: "remainingHours",
      render: (_, { timeTracking }) => {
        return timeTracking.remainingHours || "-";
      },
    },
    {
      title: "Evaluation",
      dataIndex: "evaluation",
      key: "evaluation",
      render: (_, { timeTracking }) => {
        return timeTracking.evaluation ? <StatusBadge badgeProps={jiraTaskEvaluationMetadata[timeTracking.evaluation as JiraTaskEvaluationType]} /> : "-";
      },
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (_, { status }) => {
        return <StatusBadge badgeProps={{ label: status, type: "main" }} />;
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

  // const currentTasks: JiraTask[] = [
  //   {
  //     id: 1,
  //     taskName: "Implement authentication module",
  //     storyPoints: 20,
  //     originalEstimate: 20,
  //     loggedHours: 12,
  //     remainingHours: 8,
  //     evaluation: "ON_TIME",
  //     status: "Backend Implementation",
  //   },
  //   {
  //     id: 2,
  //     taskName: "Design database schema",
  //     storyPoints: 15,
  //     originalEstimate: 18,
  //     loggedHours: 16,
  //     remainingHours: 0,
  //     evaluation: "OVERESTIMATED",
  //     status: "Pending Testzone",
  //     notes: "Overestimated by 2 hours",
  //   },
  // ];

  // const overestimatedTasks: JiraTask[] = [
  //   {
  //     id: 3,
  //     taskName: "Create user profile page",
  //     storyPoints: 10,
  //     originalEstimate: 15,
  //     loggedHours: 20,
  //     remainingHours: 0,
  //     status: "Done",
  //     notes: "Overestimated by 5 hours",
  //   },
  //   {
  //     id: 4,
  //     taskName: "Set up CI/CD pipeline",
  //     storyPoints: 25,
  //     originalEstimate: 21,
  //     loggedHours: 18,
  //     remainingHours: 2,
  //     status: "Pending Staging",
  //     notes: "Overestimated by 3 hours",
  //   },
  // ];

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
    fetchJiraTasks();
  }, []);

  const fetchJiraTasks = async (forceUpdate: boolean = false) => {
    setIsLoading(true);
    try {
      const response = await requestInstance.get(`/jira/tasks${forceUpdate ? "?forceUpdate=true" : ""}`);
      const fetchedData = response.data;
      setLastUpdated(fetchedData.lastUpdated);
      setJiraTasks(fetchedData.tasks);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsLoading(false);
  };

  return (
    <AppLayout>
      <div className={classes.refresh_container}>
        <Button icon={<RefreshCw />} onClick={() => fetchJiraTasks(true)}>
          Refresh
        </Button>
        {lastUpdated && <div className={classes.last_refresh}>Last Updated: {lastUpdated}</div>}
      </div>
      <div className={classes.jira_tasks_container}>
        <Tabs defaultActiveKey="1" items={items} />
      </div>
    </AppLayout>
  );
};

export default JiraTasks;
