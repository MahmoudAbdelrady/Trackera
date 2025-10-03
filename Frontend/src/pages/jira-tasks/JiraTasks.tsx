import { Button, Tabs, type TableProps, type TabsProps } from "antd";
import { AppLayout, TrackeraTable, StatusBadge } from "../../components";
import { ExternalLink, RefreshCw } from "lucide-react";
import { jiraTaskEvaluationMetadata, type JiraTask, type JiraTaskEvaluationType } from "../../shared/types";
import { Link } from "react-router-dom";
import trackeraTableClasses from "../../components/trackera-table/scss/trackera-table.module.css";
import classes from "./scss/jira-tasks.module.css";

const JiraTasks = () => {
  const jiraTasksColumns: TableProps<JiraTask>["columns"] = [
    {
      title: "Task Name",
      dataIndex: "taskName",
      key: "taskName",
      render: (_, record) => {
        return record.taskUrl ? (
          <Link to={record.taskUrl} className={trackeraTableClasses.task_link} target="_blank">
            {record.taskName}
            <ExternalLink className={trackeraTableClasses.link_icon} />
          </Link>
        ) : (
          record.taskName
        );
      },
    },
    {
      title: "Story Points",
      dataIndex: "storyPoints",
      key: "storyPoints",
    },
    {
      title: "Original Estimate",
      dataIndex: "originalEstimate",
      key: "originalEstimate",
    },
    {
      title: "Logged Hours",
      dataIndex: "loggedHours",
      key: "loggedHours",
    },
    {
      title: "Remaining Hours",
      dataIndex: "remainingHours",
      key: "remainingHours",
    },
    {
      title: "Evaluation",
      dataIndex: "evaluation",
      key: "evaluation",
      render: (_, { evaluation }) => {
        return <StatusBadge badgeProps={jiraTaskEvaluationMetadata[evaluation as JiraTaskEvaluationType]} />;
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
    },
  ];

  const currentTasks: JiraTask[] = [
    {
      id: 1,
      taskName: "Implement authentication module",
      storyPoints: 20,
      originalEstimate: 20,
      loggedHours: 12,
      remainingHours: 8,
      evaluation: "ON_TIME",
      status: "Backend Implementation",
    },
    {
      id: 2,
      taskName: "Design database schema",
      storyPoints: 15,
      originalEstimate: 18,
      loggedHours: 16,
      remainingHours: 0,
      evaluation: "OVERESTIMATED",
      status: "Pending Testzone",
      notes: "Overestimated by 2 hours",
    },
  ];

  const overestimatedTasks: JiraTask[] = [
    {
      id: 3,
      taskName: "Create user profile page",
      storyPoints: 10,
      originalEstimate: 15,
      loggedHours: 20,
      remainingHours: 0,
      status: "Done",
      notes: "Overestimated by 5 hours",
    },
    {
      id: 4,
      taskName: "Set up CI/CD pipeline",
      storyPoints: 25,
      originalEstimate: 21,
      loggedHours: 18,
      remainingHours: 2,
      status: "Pending Staging",
      notes: "Overestimated by 3 hours",
    },
  ];

  const items: TabsProps["items"] = [
    {
      key: "1",
      label: "Current Tasks (5)",
      children: (
        <TrackeraTable<JiraTask>
          properties={{
            columns: jiraTasksColumns,
            dataSource: currentTasks,
            pagination: { style: { marginRight: "16px" } },
          }}
        />
      ),
    },
    {
      key: "2",
      label: "Overestimated Tasks (3)",
      children: (
        <TrackeraTable<JiraTask>
          properties={{
            columns: jiraTasksColumns.filter((col) => col.key !== "evaluation"),
            dataSource: overestimatedTasks,
            pagination: { style: { marginRight: "16px" } },
          }}
        />
      ),
    },
  ];

  return (
    <AppLayout>
      <div className={classes.refresh_container}>
        <Button icon={<RefreshCw />}>Refresh</Button>
        <div className={classes.last_refresh}>Last Updated: Just now</div>
      </div>
      <div className={classes.jira_tasks_container}>
        <Tabs defaultActiveKey="1" items={items} />
      </div>
    </AppLayout>
  );
};

export default JiraTasks;
