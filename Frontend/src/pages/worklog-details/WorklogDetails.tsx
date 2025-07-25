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
import { AppLayout } from "../../components";
import classes from "./scss/worklog-details.module.css";
import { Button, Modal, Switch, Table, Tooltip, type TableProps } from "antd";
import type {
  LogMeta,
  WorklogDetails,
  WorkLogStatusType,
} from "../../utils/types";
import { useState } from "react";
import { Link } from "react-router-dom";

const statusMetadata: Record<WorkLogStatusType, LogMeta> = {
  SYNCED: { label: "Synced", className: "synced" },
  PARTIALLY: { label: "Partially", className: "partially" },
  UNSYNCED: { label: "Unsynced", className: "unsynced" },
};

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
        <Link to={record.taskUrl} className={classes.task_link} target="_blank">
          {record.taskName}
          <ExternalLink className={classes.link_icon} />
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
        return getPaddedItem("status_item", meta.label, meta.className);
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
        <div className={classes.actions_container}>
          {record.status === "SYNCED" ? (
            <Tooltip title="Unsync from Jira">
              <CalendarX2
                onClick={() => {}}
                className={`${classes.log_action_btn} ${classes.unsync}`}
              />
            </Tooltip>
          ) : (
            <Tooltip title="Sync to Jira">
              <CalendarSync
                onClick={() => {}}
                className={`${classes.log_action_btn} ${classes.sync}`}
              />
            </Tooltip>
          )}
          <Tooltip title="Edit">
            <SquarePen
              onClick={() => setEditDetailVisible(true)}
              className={`${classes.log_action_btn} ${classes.edit}`}
            />
          </Tooltip>
          <Tooltip title="View">
            <Eye
              className={`${classes.log_action_btn} ${classes.view}`}
              onClick={() => setViewDetailVisible(true)}
            />
          </Tooltip>
          <Tooltip title="Delete">
            <Trash
              onClick={() => setDeleteDetailVisible(true)}
              className={`${classes.log_action_btn} ${classes.delete}`}
            />
          </Tooltip>
        </div>
      ),
    },
  ];

  const getPaddedItem = (
    className: string,
    itemLabel: string,
    metaClassName: string
  ) => {
    return (
      <div className={`${classes[className]} ${classes[metaClassName]}`}>
        {itemLabel}
      </div>
    );
  };

  return (
    <>
      <Modal
        open={editDetailVisible}
        centered
        okText="Delete"
        onOk={() => {}}
        onCancel={() => setEditDetailVisible(false)}
        okButtonProps={{ danger: true }}
        className={classes.worklog_modal}
      >
        <h2 className={classes.header}>Edit Task Log</h2>
      </Modal>
      <Modal
        open={viewDetailVisible}
        centered
        okText="Delete"
        onOk={() => {}}
        onCancel={() => setViewDetailVisible(false)}
        okButtonProps={{ danger: true }}
        className={classes.worklog_modal}
      >
        <h2 className={classes.header}>Task Log Details</h2>
      </Modal>
      <Modal
        open={deleteDetailVisible}
        centered
        okText="Delete"
        onOk={() => {}}
        onCancel={() => setDeleteDetailVisible(false)}
        okButtonProps={{ danger: true }}
        className={classes.worklog_modal}
      >
        <h2 className={classes.header}>Delete Task Log</h2>
        <p className={classes.delete_message}>
          Are you sure you want to delete this task log? This action cannot be
          undone.
        </p>
        <div className={classes.switch_option}>
          <span className={classes.label}>Also unsync from Jira:</span>
          <Switch />
        </div>
      </Modal>
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
          <div className={classes.details_actions}>
            <Button
              icon={<CalendarSync />}
              className={`${classes.log_button} ${classes.sync}`}
              onClick={() => {}}
            >
              Sync to Jira
            </Button>
          </div>
          <div className={classes.details_data}>
            <Table
              columns={tableColumns}
              dataSource={worklogDetails}
              scroll={{ x: 768 }}
              className={classes.worklogs_table}
            />
          </div>
        </div>
      </AppLayout>
    </>
  );
};

export default WorklogDetails;
