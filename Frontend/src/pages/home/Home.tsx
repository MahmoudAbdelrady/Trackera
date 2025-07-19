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

interface Worklog {
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
      logName: "Frontend Development",
      totalHours: 10,
      date: "2023-10-01",
      evaluation: "EXCELLENT",
      status: "SYNCED",
    },
    {
      logName: "Backend Development",
      totalHours: 7.45,
      date: "2023-10-01",
      evaluation: "GOOD",
      status: "PARTIALLY",
    },
    {
      logName: "Bug Fixing",
      totalHours: 6.45,
      date: "2023-10-01",
      evaluation: "MODERATE",
      status: "UNSYNCED",
    },
    {
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
              onClick={() => {}}
              className={`${classes.log_action_btn} ${classes.edit}`}
            />
          </Tooltip>
          <Tooltip title="View">
            <Eye
              onClick={() => {}}
              className={`${classes.log_action_btn} ${classes.view}`}
            />
          </Tooltip>
          <Tooltip title="Delete">
            <Trash
              onClick={() => {}}
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
        className={classes.add_worklog_modal}
      >
        <h2 className={classes.header}>Add Worklog</h2>
        <form className={classes.add_worklog_form}>
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
