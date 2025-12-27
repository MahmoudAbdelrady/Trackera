import { ClipboardPlus } from "lucide-react";
import {
  ManageWorkLogModal,
  AppLayout,
  SearchFilter,
  WorklogModal,
  WorklogStatusCard,
  TrackeraTable,
  WorkLogColumns,
} from "../../components";
import classes from "./scss/home.module.css";
import { Alert } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { type PaginatedResponse, type Worklog, WorkLogStatus, JiraSyncEvent } from "../../shared/types";
import { createPaginationConfig } from "../../utils";
import worklogModalClasses from "../../components/worklogs/modals/worklog-modal/scss/worklog-modal.module.css";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import { userQueries } from "../../state/queries";
import { useJiraSyncSSE } from "../../shared/hooks";
import { workLogApis } from "../../state/api";
import type { WorkLogSummaryCard } from "../../components/worklogs/worklog-status-card/WorklogStatusCard";

const Home = () => {
  const { data: loggedUserData } = userQueries.useMeQuery();
  const [manageWorkLogVisible, setManageWorkLogVisible] = useState<boolean>(false);
  const [deleteWorkLogVisible, setDeleteWorkLogVisible] = useState<boolean>(false);
  const [isFetchingWorkLogs, setIsFetchingWorkLogs] = useState<boolean>(true);
  const [isDeletingWorkLog, setIsDeletingWorkLog] = useState<boolean>(false);
  const [workLogsResponse, setWorkLogsResponse] = useState<PaginatedResponse<Worklog> | null>(null);
  const [workLogSummary, setWorkLogSummary] = useState<WorkLogSummaryCard[]>([]);
  const [selectedWorkLog, setSelectedWorkLog] = useState<Worklog | undefined>(undefined);
  const [searchFilters, setSearchFilters] = useState<Record<string, any>>({});
  const hasInProgress = useMemo(
    () =>
      workLogsResponse?.content.some(
        (worklog) =>
          worklog.status === WorkLogStatus.SYNC_IN_PROGRESS || worklog.status === WorkLogStatus.UNSYNC_IN_PROGRESS
      ) ?? false,
    [workLogsResponse?.content]
  );

  useEffect(() => {
    fetchWorkLogs();
  }, [searchFilters]);

  useEffect(() => {
    fetchWorkLogSummary();
  }, []);

  const { triggerSync } = useJiraSyncSSE({
    hasInProgress,
    onStatusEvent: (event) => {
      setWorkLogsResponse((prev) => {
        if (!prev) return prev;
        const updatedContent = prev.content.map((worklog) =>
          worklog.id === event.logId && (event.type === JiraSyncEvent.WORKLOG || event.type === JiraSyncEvent.ALL)
            ? { ...worklog, status: event.status, hasError: !!event.syncError }
            : worklog
        );
        return { ...prev, content: updatedContent };
      });
    },
  });

  const fetchWorkLogs = useCallback(
    async (pageNum: number = 0, pageSize: number = 10) => {
      setIsFetchingWorkLogs(true);
      try {
        setWorkLogsResponse(await workLogApis.getWorkLogs(pageNum, pageSize, searchFilters));
      } catch (error) {
        showErrorToast(error);
      }
      setIsFetchingWorkLogs(false);
    },
    [searchFilters]
  );

  const fetchWorkLogSummary = useCallback(async () => {
    try {
      setWorkLogSummary(await workLogApis.getWorkLogSummary());
    } catch (error: any) {
      showErrorToast(error);
    }
  }, []);

  const deleteWorkLog = async (workLogId: string) => {
    setIsDeletingWorkLog(true);
    try {
      const result = await workLogApis.deleteWorkLog(workLogId);
      showSuccessToast(result.message);
      fetchWorkLogs();
      fetchWorkLogSummary();
      setDeleteWorkLogVisible(false);
      setSelectedWorkLog(undefined);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsDeletingWorkLog(false);
  };

  const tableColumns = useMemo(
    () =>
      WorkLogColumns({
        jiraLinked: loggedUserData?.jiraLinked || false,
        onSync: triggerSync,
        onEdit: (record) => {
          setSelectedWorkLog(record);
          setManageWorkLogVisible(true);
        },
        onDelete: (record) => {
          setSelectedWorkLog(record);
          setDeleteWorkLogVisible(true);
        },
      }),
    [loggedUserData?.jiraLinked, triggerSync]
  );

  return (
    <>
      {manageWorkLogVisible && (
        <ManageWorkLogModal
          setIsOpen={setManageWorkLogVisible}
          refreshWorkLogData={() => {
            fetchWorkLogs();
            fetchWorkLogSummary();
          }}
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
        <p className={worklogModalClasses.delete_message}>
          Are you sure you want to delete this worklog? This action cannot be undone.
        </p>
        {(selectedWorkLog?.status === WorkLogStatus.SYNCED || selectedWorkLog?.status === WorkLogStatus.PARTIALLY) && (
          <Alert
            message="This worklog has synced data with Jira and will be unsynced upon deletion."
            type="warning"
            showIcon
            className={worklogModalClasses.alert_message}
          />
        )}
      </WorklogModal>
      <AppLayout>
        <div className={classes.worklog_status_cards_container}>
          {workLogSummary.map((card) => (
            <WorklogStatusCard
              key={card.code}
              label={card.label}
              subLabel={card.subLabel}
              code={card.code}
              value={card.value}
            />
          ))}
        </div>
        <div className={classes.worklogs_content}>
          <SearchFilter setFilters={setSearchFilters} jiraLinked={loggedUserData?.jiraLinked || false} />
          <div className={classes.worklogs_container}>
            <TrackeraTable<Worklog>
              properties={{
                columns: tableColumns,
                dataSource: workLogsResponse?.content || [],
                loading: isFetchingWorkLogs,
                locale: {
                  emptyText: isFetchingWorkLogs ? "Loading..." : "No worklogs found",
                },
                pagination: createPaginationConfig(workLogsResponse, fetchWorkLogs, "worklogs"),
              }}
              rowKey={(record) => record.id}
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
