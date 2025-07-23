import {
  CircleCheckBig,
  Clock,
  Eye,
  Info,
  SquarePen,
  Trash,
  ClipboardPlus,
  CalendarSync,
  CalendarX2,
  Inbox,
} from "lucide-react";
import { AppLayout, SearchFilter, WorklogStatusCard } from "../../components";
import classes from "./scss/home.module.css";
import {
  Button,
  DatePicker,
  Input,
  Modal,
  Switch,
  Table,
  Tooltip,
  type TableProps,
} from "antd";
import { useState } from "react";
import Dragger from "antd/es/upload/Dragger";
import dayjs from "dayjs";
import { Link } from "react-router-dom";

interface Worklog {
  logId: number;
  logName: string;
  totalHours: number;
  date: string;
  evaluation: WorkLogEvaluationType;
  status: WorkLogStatusType;
}

type WorkLogEvaluationType = "EXCELLENT" | "GOOD" | "MODERATE" | "POOR";

type WorkLogStatusType = "SYNCED" | "PARTIALLY" | "UNSYNCED";

interface LogMeta {
  label: string;
  className: string;
}

const evaluationMetadata: Record<WorkLogEvaluationType, LogMeta> = {
  EXCELLENT: { label: "Excellent", className: "excellent" },
  GOOD: { label: "Good", className: "good" },
  MODERATE: { label: "Moderate", className: "moderate" },
  POOR: { label: "Poor", className: "poor" },
};

const statusMetadata: Record<WorkLogStatusType, LogMeta> = {
  SYNCED: { label: "Synced", className: "synced" },
  PARTIALLY: { label: "Partially", className: "partially" },
  UNSYNCED: { label: "Unsynced", className: "unsynced" },
};

const Home = () => {
  const [addWorkLogVisible, setAddWorkLogVisible] = useState(false);
  const [editWorkLogVisible, setEditWorkLogVisible] = useState(false);
  const [deleteWorkLogVisible, setDeleteWorkLogVisible] = useState(false);
  const workLogStatusCards = [
    {
      cardLabel: "Total Logged Hours",
      cardValue: "34.7",
      cardIcon: <Clock />,
      cardColorTheme: "main",
    },
    {
      cardLabel: "Target Hours",
      cardValue: "40",
      cardIcon: <CircleCheckBig />,
      cardColorTheme: "success",
    },
    {
      cardLabel: "Hours Left",
      cardValue: "5.3",
      cardIcon: <Info />,
      cardColorTheme: "info",
      cardSubLabel: "Approx. 0.7 days",
    },
  ];

  const worklogs: Worklog[] = [
    {
      logId: 1,
      logName: "Frontend Development",
      totalHours: 10,
      date: "2023-10-01",
      evaluation: "EXCELLENT",
      status: "SYNCED",
    },
    {
      logId: 2,
      logName: "Backend Development",
      totalHours: 7.45,
      date: "2023-10-01",
      evaluation: "GOOD",
      status: "PARTIALLY",
    },
    {
      logId: 3,
      logName: "Bug Fixing",
      totalHours: 6.45,
      date: "2023-10-01",
      evaluation: "MODERATE",
      status: "UNSYNCED",
    },
    {
      logId: 4,
      logName: "Reviewing",
      totalHours: 2.45,
      date: "2023-10-01",
      evaluation: "POOR",
      status: "UNSYNCED",
    },
  ];

  const tableColumns: TableProps<Worklog>["columns"] = [
    {
      title: "Log Name",
      dataIndex: "logName",
      key: "logName",
    },
    {
      title: "Total Hours",
      dataIndex: "totalHours",
      key: "totalHours",
    },
    {
      title: "Date",
      dataIndex: "date",
      key: "date",
    },
    {
      title: "Evaluation",
      dataIndex: "evaluation",
      key: "evaluation",
      render: (_, { evaluation }) => {
        const meta = evaluationMetadata[evaluation as WorkLogEvaluationType];
        return getPaddedItem("evaluation_item", meta.label, meta.className);
      },
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (_, { status }) => {
        const meta = statusMetadata[status as WorkLogStatusType];
        return getPaddedItem("status_item", meta.label, meta.className);
      },
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
              onClick={() => setEditWorkLogVisible(true)}
              className={`${classes.log_action_btn} ${classes.edit}`}
            />
          </Tooltip>
          <Tooltip title="View">
            <Link
              to={`/worklog-details/${record.logId}`}
              className={`${classes.log_action_btn} ${classes.view}`}
            >
              <Eye />
            </Link>
          </Tooltip>
          <Tooltip title="Delete">
            <Trash
              onClick={() => setDeleteWorkLogVisible(true)}
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
        open={addWorkLogVisible}
        centered
        okText="Add"
        onOk={() => setAddWorkLogVisible(false)}
        onCancel={() => setAddWorkLogVisible(false)}
        className={classes.worklog_modal}
      >
        <h2 className={classes.header}>Add Worklog</h2>
        <form className={classes.worklog_form}>
          <div className={classes.form_group}>
            <span className={classes.label}>Log Name:</span>
            <Input
              placeholder="Enter log name"
              style={{ width: "70%", marginRight: "10px" }}
            />
            <Tooltip title="If not provided, the log name will be auto-generated based on the upload date and weekday.">
              <Info size={22} cursor={"pointer"} />
            </Tooltip>
          </div>
          <div className={classes.form_group}>
            <span className={classes.label}>Log date:</span>
            <DatePicker placeholder="Select log date" defaultValue={dayjs()} />
          </div>
          <div className={`${classes.form_group} ${classes.upload_group}`}>
            <span className={classes.label}>Upload log file:</span>
            <Dragger className={classes.upload_box}>
              <div className={classes.upload_icon_box}>
                <Inbox className={classes.upload_icon} />
              </div>
              <span className={classes.upload_title}>
                Click or drag worklog file to this area to upload
              </span>
              <p className={classes.upload_subtitle}>
                Supported formats: .xlsx
              </p>
            </Dragger>
          </div>
          <div className={classes.form_group}>
            <span className={classes.label}>Sync to Jira after upload:</span>
            <Switch />
          </div>
        </form>
      </Modal>
      <Modal
        open={editWorkLogVisible}
        centered
        okText="Save"
        onOk={() => {}}
        onCancel={() => setEditWorkLogVisible(false)}
        className={classes.worklog_modal}
      >
        <h2 className={classes.header}>Edit Worklog</h2>
      </Modal>
      <Modal
        open={deleteWorkLogVisible}
        centered
        okText="Delete"
        onOk={() => {}}
        onCancel={() => setDeleteWorkLogVisible(false)}
        okButtonProps={{ danger: true }}
        className={classes.worklog_modal}
      >
        <h2 className={classes.header}>Delete Worklog</h2>
        <p className={classes.delete_message}>
          Are you sure you want to delete this worklog? This action cannot be
          undone.
        </p>
        <div className={classes.switch_option}>
          <span className={classes.label}>Also unsync from Jira:</span>
          <Switch />
        </div>
      </Modal>
      <AppLayout>
        <div className={classes.worklog_status_cards_container}>
          {workLogStatusCards.map((card, index) => (
            <WorklogStatusCard
              key={index}
              cardLabel={card.cardLabel}
              cardValue={card.cardValue}
              cardIcon={card.cardIcon}
              cardColorTheme={card.cardColorTheme}
              cardSubLabel={card.cardSubLabel}
            />
          ))}
        </div>
        <div className={classes.worklogs_content}>
          <SearchFilter />
          <div className={classes.worklogs_container}>
            <div className={classes.worklogs_actions}>
              <Button
                icon={<ClipboardPlus />}
                className={`${classes.log_button} ${classes.add}`}
                onClick={() => setAddWorkLogVisible(true)}
              >
                Add Worklog
              </Button>
            </div>
            <div className={classes.worklogs_data}>
              <Table
                columns={tableColumns}
                dataSource={worklogs}
                scroll={{ x: 768 }}
                className={classes.worklogs_table}
              />
            </div>
          </div>
        </div>
      </AppLayout>
    </>
  );
};

export default Home;
