import {
  Calendar,
  CalendarSync,
  CalendarX2,
  Clock,
  ExternalLink,
  Eye,
  RefreshCcw,
  SquarePen,
  Target,
  Trash,
} from "lucide-react";
import { AppLayout, WorklogModal, WorklogTable } from "../../components";
import classes from "./scss/worklog-details.module.css";
import worklogTableClasses from "../../components/worklogs/worklog-table/scss/worklog-table.module.css";
import { Switch, Tooltip, type TableProps } from "antd";
import {
  statusMetadata,
  type WorklogEntry,
  type WorkLogStatusType,
  type WorklogTask,
} from "../../utils/types";
import { useState } from "react";
import { Link } from "react-router-dom";
import { getPaddedItem } from "../../utils/helpers";
import worklogModalClasses from "../../components/worklogs/workklog-modal/scss/worklog-modal.module.css";

const WorklogDetails = () => {
  const [editDetailVisible, setEditDetailVisible] = useState<boolean>(false);
  const [viewDetailTask, setViewDetailTask] = useState<WorklogEntry | null>(
    null
  );
  const [deleteDetailVisible, setDeleteDetailVisible] =
    useState<boolean>(false);

  const [selectedLogDetails, setSelectedLogDetails] = useState<string[]>([]);
  const [selectedWorklogTasks, setSelectedWorklogTasks] = useState<string[]>(
    []
  );

  const worklogDetails: WorklogEntry[] = [
    {
      id: 1,
      taskName: "SAL-1234",
      taskUrl: "https://jira.example.com/browse/SAL-1234",
      totalHours: 5,
      status: "SYNCED",
    },
    {
      id: 2,
      taskName: "SAL-5678",
      taskUrl: "https://jira.example.com/browse/SAL-5678",
      totalHours: 7.45,
      status: "SYNCED",
    },
    {
      id: 3,
      taskName: "SAL-9012",
      taskUrl: "https://jira.example.com/browse/SAL-9012",
      totalHours: 6.45,
      status: "UNSYNCED",
    },
    {
      id: 4,
      taskName: "SAL-5486",
      taskUrl: "https://jira.example.com/browse/SAL-5486",
      totalHours: 2.45,
      status: "UNSYNCED",
    },
  ];

  const worklogTasks: WorklogTask[] = [
    {
      id: 1,
      fromTime: "13:00",
      toTime: "15:00",
      description: "Worked on feature X",
      status: "SYNCED",
    },
    {
      id: 2,
      fromTime: "15:30",
      toTime: "17:00",
      description: "Fixed bug Y",
      status: "SYNCED",
    },
    {
      id: 3,
      fromTime: "09:00",
      toTime: "11:00",
      description: "Reviewed PR Z",
      status: "UNSYNCED",
    },
    {
      id: 4,
      fromTime: "11:30",
      toTime: "12:30",
      description: "Team meeting",
      status: "UNSYNCED",
    },
  ];

  const logDetailsColumns: TableProps<WorklogEntry>["columns"] = [
    {
      title: "Task Name",
      dataIndex: "taskName",
      key: "taskName",
      render: (_, record) => (
        <Link
          to={record.taskUrl}
          className={worklogTableClasses.task_link}
          target="_blank"
        >
          {record.taskName}
          <ExternalLink className={worklogTableClasses.link_icon} />
        </Link>
      ),
      filters: worklogDetails.map((detail) => ({
        text: detail.taskName,
        value: detail.taskName,
      })),
      onFilter: (value, record) => record.taskName.includes(value as string),
    },
    {
      title: "Total Hours",
      dataIndex: "totalHours",
      key: "totalHours",
      sorter: (a, b) => a.totalHours - b.totalHours,
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (_, { status }) => {
        const meta = statusMetadata[status as WorkLogStatusType];
        return getPaddedItem(
          worklogTableClasses,
          "status_item",
          meta.label,
          meta.className
        );
      },
      filters: Object.entries(statusMetadata).map(([key, value]) => ({
        text: value.label,
        value: key,
      })),
      onFilter: (value, record) => record.status === value,
    },
    {
      title: "Actions",
      key: "actions",
      render: (_, record) => (
        <div className={worklogTableClasses.actions_container}>
          {record.status === "SYNCED" ? (
            <Tooltip title="Unsync from Jira">
              <CalendarX2
                onClick={() => {}}
                className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.unsync}`}
              />
            </Tooltip>
          ) : (
            <Tooltip title="Sync to Jira">
              <CalendarSync
                onClick={() => {}}
                className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.sync}`}
              />
            </Tooltip>
          )}
          <Tooltip title="Edit">
            <SquarePen
              onClick={() => setEditDetailVisible(true)}
              className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.edit}`}
            />
          </Tooltip>
          <Tooltip title="View">
            <Eye
              className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.view}`}
              onClick={() => setViewDetailTask(record)}
            />
          </Tooltip>
          <Tooltip title="Delete">
            <Trash
              onClick={() => setDeleteDetailVisible(true)}
              className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.delete}`}
            />
          </Tooltip>
        </div>
      ),
    },
  ];

  const taskLogsColumns: TableProps<WorklogTask>["columns"] = [
    {
      title: "From Time",
      dataIndex: "fromTime",
      key: "fromTime",
    },
    {
      title: "To Time",
      dataIndex: "toTime",
      key: "toTime",
    },
    {
      title: "Description",
      dataIndex: "description",
      key: "description",
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (_, { status }) => {
        const meta = statusMetadata[status as WorkLogStatusType];
        return getPaddedItem(
          worklogTableClasses,
          "status_item",
          meta.label,
          meta.className
        );
      },
      filters: Object.entries(statusMetadata)
        .filter(([key]) => key != "PARTIALLY")
        .map(([key, value]) => ({
          text: value.label,
          value: key,
        })),
      onFilter: (value, record) => record.status === value,
    },
    {
      title: "Actions",
      key: "actions",
      render: (_, record) => (
        <div className={worklogTableClasses.actions_container}>
          {record.status === "SYNCED" ? (
            <Tooltip title="Unsync from Jira">
              <CalendarX2
                onClick={() => {}}
                className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.unsync}`}
              />
            </Tooltip>
          ) : (
            <Tooltip title="Sync to Jira">
              <CalendarSync
                onClick={() => {}}
                className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.sync}`}
              />
            </Tooltip>
          )}
          <Tooltip title="Edit">
            <SquarePen
              onClick={() => {}}
              className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.edit}`}
            />
          </Tooltip>
          <Tooltip title="Delete">
            <Trash
              onClick={() => {}}
              className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.delete}`}
            />
          </Tooltip>
        </div>
      ),
    },
  ];

  const viewTaskModalCloseHandler = () => {
    setViewDetailTask(null);
    setSelectedWorklogTasks([]);
  };

  return (
    <>
      <WorklogModal
        title="Edit Task Log"
        properties={{
          open: editDetailVisible,
          centered: true,
          okText: "Save",
          onOk: () => {},
          onCancel: () => setEditDetailVisible(false),
        }}
      >
        <span>To Be Implemented</span>
      </WorklogModal>
      <WorklogModal
        title={`${viewDetailTask?.taskName} Task Logs`}
        properties={{
          open: !!viewDetailTask,
          centered: true,
          footer: null,
          width: "80%",
          onCancel: viewTaskModalCloseHandler,
        }}
      >
        <WorklogTable<WorklogTask>
          properties={{
            columns: taskLogsColumns,
            dataSource: worklogTasks,
            rowSelection: {
              selectedRowKeys: selectedWorklogTasks,
              onChange: (_, selectedRows: WorklogTask[]) => {
                setSelectedWorklogTasks(
                  selectedRows.map((row) => row.id.toString())
                );
              },
            },
          }}
          actionButtons={[
            {
              label: "Sync to Jira",
              icon: <CalendarSync />,
              onClick: () => {},
              disabled: selectedWorklogTasks.length === 0,
            },
          ]}
        />
      </WorklogModal>
      <WorklogModal
        title="Delete Task Log"
        properties={{
          open: deleteDetailVisible,
          centered: true,
          okText: "Delete",
          onOk: () => {},
          onCancel: () => setDeleteDetailVisible(false),
          okButtonProps: { danger: true },
        }}
      >
        <p className={worklogModalClasses.delete_message}>
          Are you sure you want to delete this task log? This action cannot be
          undone.
        </p>
        <div className={worklogModalClasses.switch_option}>
          <span className={worklogModalClasses.label}>
            Also unsync from Jira:
          </span>
          <Switch />
        </div>
      </WorklogModal>
      <AppLayout>
        <div className={classes.worklog_details_info}>
          <h3 className={classes.title}>Test Worklog</h3>
          <div className={classes.info}>
            <div className={classes.info_box}>
              <Tooltip title="Total Hours">
                <Clock className={classes.info_icon} />
              </Tooltip>
              <span className={classes.info_label}>8.5 hours</span>
            </div>
            <div className={classes.info_box}>
              <Tooltip title="Date">
                <Calendar className={classes.info_icon} />
              </Tooltip>
              <span className={classes.info_label}>2023-10-01</span>
            </div>
            <div className={classes.info_box}>
              <Tooltip title="Evaluation">
                <Target className={classes.info_icon} />
              </Tooltip>
              <span
                className={`${classes.info_label} ${classes.evaluation} ${classes.excellent}`}
              >
                Excellent
              </span>
            </div>
            <div className={classes.info_box}>
              <Tooltip title="Status">
                <RefreshCcw className={classes.info_icon} />
              </Tooltip>
              <span
                className={`${classes.info_label} ${classes.status} ${classes.synced}`}
              >
                Synced
              </span>
            </div>
          </div>
        </div>
        <div className={classes.worklog_details_content}>
          <WorklogTable<WorklogEntry>
            properties={{
              columns: logDetailsColumns,
              dataSource: worklogDetails,
              rowSelection: {
                selectedRowKeys: selectedLogDetails,
                onChange: (_, selectedRows: WorklogEntry[]) => {
                  setSelectedLogDetails(
                    selectedRows.map((row) => row.id.toString())
                  );
                },
              },
            }}
            actionButtons={[
              {
                label: "Sync selected to Jira",
                icon: <CalendarSync />,
                onClick: () => {},
                disabled: selectedLogDetails.length === 0,
                customClasses: ["sync_selected"],
              },
              {
                label: "Sync all to Jira",
                icon: <CalendarSync />,
                onClick: () => {},
              },
            ]}
          />
        </div>
      </AppLayout>
    </>
  );
};

export default WorklogDetails;
