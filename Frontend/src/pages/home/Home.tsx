import { Eye, SquarePen, Trash, ClipboardPlus, CalendarSync, CalendarX2, CalendarOff, CircleAlert } from "lucide-react";
import { ManageWorkLogModal, AppLayout, SearchFilter, WorklogModal, WorklogStatusCard, TrackeraTable, StatusBadge } from "../../components";
import classes from "./scss/home.module.css";
import { Alert, Button, Tooltip, type TableProps } from "antd";
import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import {
  worklogEvaluationMetadata,
  statusMetadata,
  type WorkLogSummaryCard,
  type PaginatedResponse,
  type Worklog,
  type WorkLogEvaluationType,
  type WorkLogStatusType,
  type WorklogSelection,
} from "../../shared/types";
import { createPaginationConfig } from "../../utils";
import trackeraTableClasses from "../../components/trackera-table/scss/trackera-table.module.css";
import worklogModalClasses from "../../components/worklogs/modals/worklog-modal/scss/worklog-modal.module.css";
import requestInstance from "../../shared/axios/request-instance";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import { userQueries } from "../../state/queries";
import { authApis } from "../../state/api";

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
  const eventSourceRef = useRef<EventSource | null>(null);

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

  useEffect(() => {
    const hasInProgressLogs = workLogsResponse?.content.some((worklog) => worklog.status === "SYNC_IN_PROGRESS" || worklog.status === "UNSYNC_IN_PROGRESS");
    if (hasInProgressLogs && !eventSourceRef.current) {
      subscribeForStatusUpdates();
    }

    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
    };
  }, [workLogsResponse]);

  const subscribeForStatusUpdates = () => {
    if (eventSourceRef.current) return;

    const es = new EventSource(`${import.meta.env.VITE_TRACKERA_BACKEND_URL}/notifications/subscribe`, {
      withCredentials: true,
    });
    es.addEventListener("worklog-sync-status", (event: MessageEvent) => {
      const workLogStatusEventMessage = JSON.parse(event.data);
      setWorkLogsResponse((prev) => {
        if (!prev) return prev;
        const updatedContent = prev.content.map((worklog) =>
          worklog.id === workLogStatusEventMessage.logId && workLogStatusEventMessage.type === "WORKLOG"
            ? { ...worklog, status: workLogStatusEventMessage.status, hasError: !!workLogStatusEventMessage.syncError }
            : worklog
        );
        return { ...prev, content: updatedContent };
      });
    });
    es.addEventListener("error", async (event: MessageEvent) => {
      const errorResponse = JSON.parse(event.data);
      if (errorResponse.status === 401) {
        await authApis.refreshToken();
        closeEventSource(es);
        subscribeForStatusUpdates();
        return;
      }
      closeEventSource(es);
    });

    eventSourceRef.current = es;
  };

  const closeEventSource = (event: EventSource) => {
    event.close();
    eventSourceRef.current = null;
  };

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
      await handleWorkLogDeletion(workLogId, null);
      setFetchWorkLog(true);
      setFetchSummary(true);
      setDeleteWorkLogVisible(false);
      setSelectedWorkLog(undefined);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsDeletingWorkLog(false);
  };

  const handleWorkLogDeletion = async (worklogId: string | number, selection: WorklogSelection | null) => {
    const response = await requestInstance.delete(`/worklog/${worklogId}`, { data: { ...selection } });
    showSuccessToast(response.data.message);
    return response.data;
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
      render: (_, { status, hasError }) => {
        return loggedUserData?.jiraLinked ? <StatusBadge badgeProps={{ ...statusMetadata[status as WorkLogStatusType], icon: hasError ? <CircleAlert /> : undefined }} /> : "-";
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
            <Button
              type="text"
              icon={<Trash />}
              onClick={() => {
                setSelectedWorkLog(record);
                setDeleteWorkLogVisible(true);
              }}
              className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.delete} ${
                (record.status === "SYNC_IN_PROGRESS" || record.status === "UNSYNC_IN_PROGRESS") && trackeraTableClasses.disabled
              }`}
              disabled={record.status === "SYNC_IN_PROGRESS" || record.status === "UNSYNC_IN_PROGRESS"}
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
