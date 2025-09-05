import { Eye, SquarePen, Trash, ClipboardPlus, CalendarSync, CalendarX2 } from "lucide-react";
import { ManageWorkLogModal, AppLayout, SearchFilter, WorklogModal, WorklogStatusCard, WorklogTable } from "../../components";
import classes from "./scss/home.module.css";
import { Switch, Tooltip, type TableProps } from "antd";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { evaluationMetadata, statusMetadata, type WorkLogSummaryCard, type PaginatedResponse, type Worklog, type WorkLogEvaluationType, type WorkLogStatusType } from "../../shared/types";
import { createPaginationConfig, getPaddedItem } from "../../utils";
import worklogTableClasses from "../../components/worklogs/worklog-table/scss/worklog-table.module.css";
import worklogModalClasses from "../../components/worklogs/modals/worklog-modal/scss/worklog-modal.module.css";
import requestInstance from "../../shared/axios/request-instance";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";

const Home = () => {
  const [manageWorkLogVisible, setManageWorkLogVisible] = useState<boolean>(false);
  const [deleteWorkLogVisible, setDeleteWorkLogVisible] = useState<boolean>(false);
  const [isFetchingWorkLogs, setIsFetchingWorkLogs] = useState<boolean>(true);
  const [isDeletingWorkLog, setIsDeletingWorkLog] = useState<boolean>(false);
  const [fetchWorkLog, setFetchWorkLog] = useState<boolean>(true);
  const [workLogsResponse, setWorkLogsResponse] = useState<PaginatedResponse<Worklog> | null>(null);
  const [workLogSummary, setWorkLogSummary] = useState<WorkLogSummaryCard[]>([]);
  const [selectedWorkLog, setSelectedWorkLog] = useState<Worklog | undefined>(undefined);

  useEffect(() => {
    if (fetchWorkLog) {
      fetchWorkLogs();
      fetchWorkLogSummary();
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

  const fetchWorkLogSummary = async () => {
    try {
      const response = await requestInstance.get("/worklog/summary");
      setWorkLogSummary(response.data);
    } catch (error: any) {
      showErrorToast(error);
    }
  };

  const deleteWorkLog = async (workLogId: number) => {
    setIsDeletingWorkLog(true);
    try {
      const response = await requestInstance.delete(`/worklog/${workLogId}`);
      showSuccessToast(response.data);
      setFetchWorkLog(true);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsDeletingWorkLog(false);
    setDeleteWorkLogVisible(false);
    setSelectedWorkLog(undefined);
  };

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
            <Trash
              onClick={() => {
                setSelectedWorkLog(record);
                setDeleteWorkLogVisible(true);
              }}
              className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.delete}`}
            />
          </Tooltip>
        </div>
      ),
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
          closable: !isDeletingWorkLog,
          keyboard: !isDeletingWorkLog,
          maskClosable: !isDeletingWorkLog,
          okButtonProps: { danger: true, loading: isDeletingWorkLog, disabled: isDeletingWorkLog },
          cancelButtonProps: { disabled: isDeletingWorkLog },
          onOk: () => deleteWorkLog(selectedWorkLog?.id!),
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
          {workLogSummary.map((card, index) => (
            <WorklogStatusCard key={index} label={card.label} subLabel={card.subLabel} code={card.code} value={card.value} />
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
