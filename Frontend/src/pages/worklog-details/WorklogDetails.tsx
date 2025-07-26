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
  type Worklog,
  type WorklogDetails,
  type WorkLogStatusType,
} from "../../utils/types";
import { useState } from "react";
import { Link } from "react-router-dom";
import { getPaddedItem } from "../../utils/helpers";
import worklogModalClasses from "../../components/worklogs/workklog-modal/scss/worklog-modal.module.css";

const WorklogDetails = () => {
  const [editDetailVisible, setEditDetailVisible] = useState(false);
  const [viewDetailVisible, setViewDetailVisible] = useState(false);
  const [deleteDetailVisible, setDeleteDetailVisible] = useState(false);

  const worklogDetails: WorklogDetails[] = [
    {
      detailId: 1,
      taskName: "SAL-1234",
      taskUrl: "https://jira.example.com/browse/SAL-1234",
      totalHours: 5,
      status: "SYNCED",
    },
    {
      detailId: 2,
      taskName: "SAL-5678",
      taskUrl: "https://jira.example.com/browse/SAL-5678",
      totalHours: 7.45,
      status: "SYNCED",
    },
    {
      detailId: 3,
      taskName: "SAL-9012",
      taskUrl: "https://jira.example.com/browse/SAL-9012",
      totalHours: 6.45,
      status: "UNSYNCED",
    },
    {
      detailId: 4,
      taskName: "SAL-5486",
      taskUrl: "https://jira.example.com/browse/SAL-5486",
      totalHours: 2.45,
      status: "UNSYNCED",
    },
  ];

  const tableColumns: TableProps<WorklogDetails>["columns"] = [
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
              onClick={() => setEditDetailVisible(true)}
              className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.edit}`}
            />
          </Tooltip>
          <Tooltip title="View">
            <Eye
              className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.view}`}
              onClick={() => setViewDetailVisible(true)}
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
        title="Task Log Details"
        properties={{
          open: viewDetailVisible,
          centered: true,
          footer: null,
          onCancel: () => setViewDetailVisible(false),
        }}
      >
        <span>To Be Implemented</span>
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
          <WorklogTable
            columns={
              tableColumns as TableProps<WorklogDetails | Worklog>["columns"]
            }
            dataSource={worklogDetails}
            actionButtons={[
              {
                label: "Sync to Jira",
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
