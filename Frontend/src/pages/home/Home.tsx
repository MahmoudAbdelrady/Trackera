import { Eye, SquarePen, Trash, ClipboardPlus, CalendarSync, CalendarX2, CalendarOff } from "lucide-react";
import { ManageWorkLogModal, AppLayout, SearchFilter, WorklogModal, WorklogStatusCard, TrackeraTable, StatusBadge } from "../../components";
import classes from "./scss/home.module.css";
import { Alert, Button, Tooltip, type TableProps } from "antd";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { worklogEvaluationMetadata, statusMetadata, type WorkLogSummaryCard, type PaginatedResponse, type Worklog, type WorkLogEvaluationType, type WorkLogStatusType } from "../../shared/types";
import { createPaginationConfig } from "../../utils";
import trackeraTableClasses from "../../components/trackera-table/scss/trackera-table.module.css";
import worklogModalClasses from "../../components/worklogs/modals/worklog-modal/scss/worklog-modal.module.css";
import requestInstance from "../../shared/axios/request-instance";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import { userQueries } from "../../state/queries";

const Home = () => {
  const { data: loggedUserData } = userQueries.useMeQuery();
  const [manageWorkLogVisible, setManageWorkLogVisible] = useState<boolean>(false);
  const [deleteWorkLogVisible, setDeleteWorkLogVisible] = useState<boolean>(false);
  const [isFetchingWorkLogs, setIsFetchingWorkLogs] = useState<boolean>(true);
  const [isDeletingWorkLog, setIsDeletingWorkLog] = useState<boolean>(false);
  const [isSyncingJira, setIsSyncingJira] = useState<boolean>(false);
  const [fetchWorkLog, setFetchWorkLog] = useState<boolean>(true);
  const [fetchSummary, setFetchSummary] = useState<boolean>(true);
  const [workLogsResponse, setWorkLogsResponse] = useState<PaginatedResponse<Worklog> | null>(null);
  const [workLogSummary, setWorkLogSummary] = useState<WorkLogSummaryCard[]>([]);
  const [selectedWorkLog, setSelectedWorkLog] = useState<Worklog | undefined>(undefined);
  const [searchFilters, setSearchFilters] = useState<Record<string, any>>({});

  useEffect(() => {
    if (fetchWorkLog) {
      fetchWorkLogs();
      setFetchWorkLog(false);
    }
  }, [fetchWorkLog]);

  useEffect(() => {
    if (fetchSummary) {
      fetchWorkLogSummary();
      setFetchSummary(false);
    }
  }, [fetchSummary]);

  const fetchWorkLogs = async (pageNum: number = 0, pageSize: number = 10) => {
    setIsFetchingWorkLogs(true);
    try {
      const response = await requestInstance.post(`/worklog/search?page=${pageNum}&size=${pageSize}`, searchFilters);
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

  const deleteWorkLog = async (workLogId: string | number) => {
    setIsDeletingWorkLog(true);
    try {
      const response = await requestInstance.delete(`/worklog/${workLogId}`);
      showSuccessToast(response.data);
      setFetchWorkLog(true);
      setFetchSummary(true);
      setDeleteWorkLogVisible(false);
      setSelectedWorkLog(undefined);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsDeletingWorkLog(false);
  };

  const performJiraSync = async (workLogId: string | number, sync: boolean) => {
    setIsSyncingJira(true);
    try {
      const response = await requestInstance.post(`/worklog/${workLogId}/sync${sync ? "" : "?sync=false"}`);
      showSuccessToast(response.data);
      setFetchWorkLog(true);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsSyncingJira(false);
  };

  const tableColumns: TableProps<Worklog>["columns"] = [
    {
      title: "Log Name",
      dataIndex: "name",
      key: "name",
    },
    {
      title: "Total Time",
      dataIndex: "totalTime",
      key: "totalTime",
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
        return <StatusBadge badgeProps={worklogEvaluationMetadata[evaluation as WorkLogEvaluationType]} />;
      },
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (_, { status }) => {
        return loggedUserData?.jiraLinked ? <StatusBadge badgeProps={statusMetadata[status as WorkLogStatusType]} /> : "-";
      },
    },
    {
      title: "Actions",
      key: "actions",
      render: (_, record) => (
        <div className={trackeraTableClasses.actions_container}>
          {record.status === "SYNCED" || record.status === "UNSYNC_IN_PROGRESS" ? (
            <Tooltip title={`${loggedUserData?.jiraLinked || record.status === "UNSYNC_IN_PROGRESS" ? "Unsync from Jira" : "Link your Jira account in settings to enable this option."}`}>
              <Button
                type="text"
                icon={!loggedUserData?.jiraLinked ? <CalendarOff /> : <CalendarX2 />}
                onClick={() => performJiraSync(record.id, false)}
                className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.unsync} ${
                  (!loggedUserData?.jiraLinked || record.status === "UNSYNC_IN_PROGRESS") && trackeraTableClasses.disabled
                }`}
                disabled={!loggedUserData?.jiraLinked || record.status === "UNSYNC_IN_PROGRESS"}
              />
            </Tooltip>
          ) : (
            <Tooltip title={`${loggedUserData?.jiraLinked || record.status === "SYNC_IN_PROGRESS" ? "Sync to Jira" : "Link your Jira account in settings to enable this option."}`}>
              <Button
                type="text"
                icon={!loggedUserData?.jiraLinked ? <CalendarOff /> : <CalendarSync />}
                onClick={() => performJiraSync(record.id, true)}
                className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.sync} ${
                  (!loggedUserData?.jiraLinked || record.status === "SYNC_IN_PROGRESS") && trackeraTableClasses.disabled
                }`}
                disabled={!loggedUserData?.jiraLinked || record.status === "SYNC_IN_PROGRESS"}
              />
            </Tooltip>
          )}
          <Tooltip title="Edit">
            <SquarePen
              onClick={() => {
                setSelectedWorkLog(record);
                setManageWorkLogVisible(true);
              }}
              className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.edit}`}
            />
          </Tooltip>
          <Tooltip title="View">
            <Link to={`/worklog-details/${record.id}`} className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.view}`}>
              <Eye />
            </Link>
          </Tooltip>
          <Tooltip title="Delete">
            <Trash
              onClick={() => {
                setSelectedWorkLog(record);
                setDeleteWorkLogVisible(true);
              }}
              className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.delete}`}
            />
          </Tooltip>
        </div>
      ),
    },
  ];

  return (
    <>
      {manageWorkLogVisible && (
        <ManageWorkLogModal
          setIsOpen={setManageWorkLogVisible}
          setFetchWorkLog={setFetchWorkLog}
          setFetchSummary={setFetchSummary}
          selectedWorkLog={selectedWorkLog}
          setSelectedWorkLog={setSelectedWorkLog}
          jiraLinked={loggedUserData?.jiraLinked || false}
        />
      )}
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
          onCancel: () => {
            setDeleteWorkLogVisible(false);
            setSelectedWorkLog(undefined);
          },
        }}
      >
        <p className={worklogModalClasses.delete_message}>Are you sure you want to delete this worklog? This action cannot be undone.</p>
        {(selectedWorkLog?.status === "SYNCED" || selectedWorkLog?.status === "PARTIALLY") && (
          <Alert message="This worklog has synced data with Jira and will be unsynced upon deletion." type="warning" showIcon className={worklogModalClasses.alert_message} />
        )}
      </WorklogModal>
      <AppLayout>
        <div className={classes.worklog_status_cards_container}>
          {workLogSummary.map((card, index) => (
            <WorklogStatusCard key={index} label={card.label} subLabel={card.subLabel} code={card.code} value={card.value} />
          ))}
        </div>
        <div className={classes.worklogs_content}>
          <SearchFilter filters={searchFilters} setFilters={setSearchFilters} setFetchWorkLog={setFetchWorkLog} jiraLinked={loggedUserData?.jiraLinked || false} />
          <div className={classes.worklogs_container}>
            <TrackeraTable<Worklog>
              properties={{
                columns: tableColumns,
                dataSource: workLogsResponse?.content || [],
                loading: isFetchingWorkLogs || isSyncingJira,
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
