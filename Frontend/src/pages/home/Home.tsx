import { CircleCheckBig, Clock, Eye, Info, SquarePen, Trash, ClipboardPlus, CalendarSync, CalendarX2 } from "lucide-react";
import { ManageWorkLogModal, AppLayout, SearchFilter, WorklogModal, WorklogStatusCard, WorklogTable } from "../../components";
import classes from "./scss/home.module.css";
import { Switch, Tooltip, type TableProps } from "antd";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { evaluationMetadata, statusMetadata, type PaginatedResponse, type Worklog, type WorkLogEvaluationType, type WorkLogStatusType } from "../../shared/types";
import { createPaginationConfig, getPaddedItem } from "../../utils";
import worklogTableClasses from "../../components/worklogs/worklog-table/scss/worklog-table.module.css";
import worklogModalClasses from "../../components/worklogs/modals/worklog-modal/scss/worklog-modal.module.css";
import requestInstance from "../../shared/axios/request-instance";
import { showErrorToast } from "../../utils/toast-handler/showToast";

const Home = () => {
  const [manageWorkLogVisible, setManageWorkLogVisible] = useState<boolean>(false);
  const [deleteWorkLogVisible, setDeleteWorkLogVisible] = useState<boolean>(false);
  const [isFetchingWorkLogs, setIsFetchingWorkLogs] = useState<boolean>(true);
  const [fetchWorkLog, setFetchWorkLog] = useState<boolean>(true);
  const [workLogsResponse, setWorkLogsResponse] = useState<PaginatedResponse<Worklog> | null>(null);
  const [selectedWorkLog, setSelectedWorkLog] = useState<Worklog | undefined>(undefined);

  useEffect(() => {
    if (fetchWorkLog) {
      fetchWorkLogs();
      setFetchWorkLog(false);
    }
  }, [fetchWorkLog]);

  const fetchWorkLogs = async (pageNum: number = 0, pageSize: number = 10) => {
    setIsFetchingWorkLogs(true);
    try {
      const response = await requestInstance.get(`/worklog?page=${pageNum}&size=${pageSize}`);
      setWorkLogsResponse(response.data);
    } catch (error) {
      showErrorToast(error);
    }
    setIsFetchingWorkLogs(false);
  };
  //   {
  //     id: 1,
  //     logName: "Frontend Development",
  //     totalHours: 10,
  //     date: "2023-10-01",
  //     evaluation: "EXCELLENT",
  //     status: "SYNCED",
  //   },
  //   {
  //     id: 2,
  //     logName: "Backend Development",
  //     totalHours: 7.45,
  //     date: "2023-10-01",
  //     evaluation: "GOOD",
  //     status: "PARTIALLY",
  //   },
  //   {
  //     id: 3,
  //     logName: "Bug Fixing",
  //     totalHours: 6.45,
  //     date: "2023-10-01",
  //     evaluation: "MODERATE",
  //     status: "UNSYNCED",
  //   },
  //   {
  //     id: 4,
  //     logName: "Reviewing",
  //     totalHours: 2.45,
  //     date: "2023-10-01",
  //     evaluation: "POOR",
  //     status: "UNSYNCED",
  //   },
  // ];

  const tableColumns: TableProps<Worklog>["columns"] = [
    {
      title: "Log Name",
      dataIndex: "name",
      key: "name",
    },
    {
      title: "Total Hours",
      dataIndex: "totalHours",
      key: "totalHours",
    },
    {
      title: "Date",
      dataIndex: "workDate",
      key: "workDate",
    },
    {
      title: "Evaluation",
      dataIndex: "evaluation",
      key: "evaluation",
      render: (_, { evaluation }) => {
        const meta = evaluationMetadata[evaluation as WorkLogEvaluationType];
        return getPaddedItem(worklogTableClasses, "evaluation_item", meta.label, meta.className);
      },
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (_, { status }) => {
        const meta = statusMetadata[status as WorkLogStatusType];
        return getPaddedItem(worklogTableClasses, "status_item", meta.label, meta.className);
      },
    },
    {
      title: "Actions",
      key: "actions",
      render: (_, record) => (
        <div className={worklogTableClasses.actions_container}>
          {record.status === "SYNCED" ? (
            <Tooltip title="Unsync from Jira">
              <CalendarX2 onClick={() => {}} className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.unsync}`} />
            </Tooltip>
          ) : (
            <Tooltip title="Sync to Jira">
              <CalendarSync onClick={() => {}} className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.sync}`} />
            </Tooltip>
          )}
          <Tooltip title="Edit">
            <SquarePen
              onClick={() => {
                setSelectedWorkLog(record);
                setManageWorkLogVisible(true);
              }}
              className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.edit}`}
            />
          </Tooltip>
          <Tooltip title="View">
            <Link to={`/worklog-details/${record.id}`} className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.view}`}>
              <Eye />
            </Link>
          </Tooltip>
          <Tooltip title="Delete">
            <Trash onClick={() => setDeleteWorkLogVisible(true)} className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.delete}`} />
          </Tooltip>
        </div>
      ),
    },
  ];

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

  return (
    <>
      {manageWorkLogVisible && <ManageWorkLogModal setIsOpen={setManageWorkLogVisible} setFetchWorkLog={setFetchWorkLog} selectedWorkLog={selectedWorkLog} setSelectedWorkLog={setSelectedWorkLog} />}
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
        <p className={worklogModalClasses.delete_message}>Are you sure you want to delete this worklog? This action cannot be undone.</p>
        <div className={worklogModalClasses.switch_option}>
          <span className={worklogModalClasses.label}>Also unsync from Jira:</span>
          <Switch />
        </div>
      </WorklogModal>
      <AppLayout>
        <div className={classes.worklog_status_cards_container}>
          {workLogStatusCards.map((card, index) => (
            <WorklogStatusCard key={index} cardLabel={card.cardLabel} cardValue={card.cardValue} cardIcon={card.cardIcon} cardColorTheme={card.cardColorTheme} cardSubLabel={card.cardSubLabel} />
          ))}
        </div>
        <div className={classes.worklogs_content}>
          <SearchFilter />
          <div className={classes.worklogs_container}>
            <WorklogTable<Worklog>
              properties={{
                columns: tableColumns,
                dataSource: workLogsResponse?.content || [],
                loading: isFetchingWorkLogs,
                locale: {
                  emptyText: isFetchingWorkLogs ? "Loading..." : "No worklogs found",
                },
                pagination: createPaginationConfig(workLogsResponse, fetchWorkLogs, "worklogs"),
              }}
              actionButtons={[
                {
                  label: "Add Worklog",
                  icon: <ClipboardPlus />,
                  onClick: () => setManageWorkLogVisible(true),
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
