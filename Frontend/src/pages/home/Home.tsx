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
import {
  AppLayout,
  SearchFilter,
  WorklogModal,
  WorklogStatusCard,
  WorklogTable,
} from "../../components";
import classes from "./scss/home.module.css";
import { DatePicker, Input, Switch, Tooltip, type TableProps } from "antd";
import { useState } from "react";
import Dragger from "antd/es/upload/Dragger";
import dayjs from "dayjs";
import { Link } from "react-router-dom";
import {
  evaluationMetadata,
  statusMetadata,
  type Worklog,
  type WorklogDetails,
  type WorkLogEvaluationType,
  type WorkLogStatusType,
} from "../../utils/types";
import { getPaddedItem } from "../../utils/helpers";
import worklogTableClasses from "../../components/worklogs/worklog-table/scss/worklog-table.module.css";
import worklogModalClasses from "../../components/worklogs/workklog-modal/scss/worklog-modal.module.css";

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
        return getPaddedItem(
          worklogTableClasses,
          "evaluation_item",
          meta.label,
          meta.className
        );
      },
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
              onClick={() => setEditWorkLogVisible(true)}
              className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.edit}`}
            />
          </Tooltip>
          <Tooltip title="View">
            <Link
              to={`/worklog-details/${record.logId}`}
              className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.view}`}
            >
              <Eye />
            </Link>
          </Tooltip>
          <Tooltip title="Delete">
            <Trash
              onClick={() => setDeleteWorkLogVisible(true)}
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
        title="Add Worklog"
        properties={{
          open: addWorkLogVisible,
          centered: true,
          okText: "Add",
          onOk: () => setAddWorkLogVisible(false),
          onCancel: () => setAddWorkLogVisible(false),
        }}
      >
        <form className={worklogModalClasses.worklog_form}>
          <div className={worklogModalClasses.form_group}>
            <span className={worklogModalClasses.label}>Log Name:</span>
            <Input
              placeholder="Enter log name"
              style={{ width: "70%", marginRight: "10px" }}
            />
            <Tooltip title="If not provided, the log name will be auto-generated based on the upload date and weekday.">
              <Info size={22} cursor={"pointer"} />
            </Tooltip>
          </div>
          <div className={worklogModalClasses.form_group}>
            <span className={worklogModalClasses.label}>Log date:</span>
            <DatePicker placeholder="Select log date" defaultValue={dayjs()} />
          </div>
          <div
            className={`${worklogModalClasses.form_group} ${worklogModalClasses.upload_group}`}
          >
            <span className={worklogModalClasses.label}>Upload log file:</span>
            <Dragger className={worklogModalClasses.upload_box}>
              <div className={worklogModalClasses.upload_icon_box}>
                <Inbox className={worklogModalClasses.upload_icon} />
              </div>
              <span className={worklogModalClasses.upload_title}>
                Click or drag worklog file to this area to upload
              </span>
              <p className={worklogModalClasses.upload_subtitle}>
                Supported formats: .xlsx
              </p>
            </Dragger>
          </div>
          <div className={worklogModalClasses.form_group}>
            <span className={worklogModalClasses.label}>
              Sync to Jira after upload:
            </span>
            <Switch />
          </div>
        </form>
      </WorklogModal>
      <WorklogModal
        title="Edit Worklog"
        properties={{
          open: editWorkLogVisible,
          centered: true,
          okText: "Save",
          onOk: () => {},
          onCancel: () => setEditWorkLogVisible(false),
        }}
      >
        <span>To Be Implemented</span>
      </WorklogModal>
      <WorklogModal
        title="Delete Worklog"
        properties={{
          open: deleteWorkLogVisible,
          centered: true,
          okText: "Delete",
          okButtonProps: { danger: true },
          onOk: () => {},
          onCancel: () => setDeleteWorkLogVisible(false),
        }}
      >
        <p className={worklogModalClasses.delete_message}>
          Are you sure you want to delete this worklog? This action cannot be
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
            <WorklogTable
              columns={
                tableColumns as TableProps<Worklog | WorklogDetails>["columns"]
              }
              dataSource={worklogs}
              actionButtons={[
                {
                  label: "Add Worklog",
                  icon: <ClipboardPlus />,
                  onClick: () => setAddWorkLogVisible(true),
                },
              ]}
            />
          </div>
        </div>
      </AppLayout>
    </>
  );
};

export default Home;
