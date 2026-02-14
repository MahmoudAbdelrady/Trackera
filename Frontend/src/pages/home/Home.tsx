import { ClipboardPlus } from "lucide-react";
import {
  ManageWorklogModal,
  SearchFilter,
  WorklogModal,
  WorklogStatusCard,
  TrackeraTable,
  createWorklogColumns,
  DeleteWarning,
} from "../../components";
import classes from "./scss/home.module.css";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  type PaginatedResponse,
  type Worklog,
  WORKLOG_STATUS,
  JIRA_SYNC_EVENT,
  type WorklogSummaryResponse,
} from "../../shared/types";
import { createPaginationConfig } from "../../utils";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import { userQueries } from "../../state/queries";
import { useJiraSyncSSE } from "../../shared/hooks";
import { worklogApis } from "../../state/api";
import { AppLayout } from "../../layouts";
import { Badge } from "antd";
import type { JiraSyncSSEReturn } from "../../shared/hooks/useJiraSyncSSE";

const Home = () => {
  const { data: loggedUserData } = userQueries.useMeQuery();
  const [manageWorklogVisible, setManageWorklogVisible] = useState<boolean>(false);
  const [deleteWorklogVisible, setDeleteWorklogVisible] = useState<boolean>(false);
  const [isFetchingWorklogs, setIsFetchingWorklogs] = useState<boolean>(true);
  const [isDeletingWorklog, setIsDeletingWorklog] = useState<boolean>(false);
  const [worklogsResponse, setWorklogsResponse] = useState<PaginatedResponse<Worklog> | null>(null);
  const [worklogSummaryResponse, setWorklogSummaryResponse] = useState<WorklogSummaryResponse | null>(null);
  const [selectedWorklog, setSelectedWorklog] = useState<Worklog | undefined>(undefined);
  const [searchFilters, setSearchFilters] = useState<Record<string, any>>({});
  const hasInProgress = useMemo(
    () =>
      worklogsResponse?.content.some(
        (worklog) =>
          worklog.status === WORKLOG_STATUS.IN_QUEUE ||
          WORKLOG_STATUS.SYNC_IN_PROGRESS ||
          worklog.status === WORKLOG_STATUS.UNSYNC_IN_PROGRESS,
      ) ?? false,
    [worklogsResponse?.content],
  );

  useEffect(() => {
    fetchWorklogs();
  }, [searchFilters]);

  useEffect(() => {
    fetchWorklogSummary();
  }, []);

  const jiraSyncSSE: JiraSyncSSEReturn = useJiraSyncSSE({
    hasInProgress,
    onStatusEvent: (event) => {
      setWorklogsResponse((prev) => {
        if (!prev) return prev;
        const updatedContent = prev.content.map((worklog) =>
          worklog.id === event.logId && (event.type === JIRA_SYNC_EVENT.WORKLOG || event.type === JIRA_SYNC_EVENT.ALL)
            ? { ...worklog, status: event.status, hasError: !!event.syncError }
            : worklog,
        );
        return { ...prev, content: updatedContent };
      });
    },
  });

  const fetchWorklogs = useCallback(
    async (pageNum: number = 0, pageSize: number = 10) => {
      setIsFetchingWorklogs(true);
      try {
        setWorklogsResponse(await worklogApis.getWorklogs(pageNum, pageSize, searchFilters));
      } catch (error) {
        showErrorToast(error);
      }
      setIsFetchingWorklogs(false);
    },
    [searchFilters],
  );

  const fetchWorklogSummary = useCallback(async () => {
    try {
      setWorklogSummaryResponse(await worklogApis.getWorklogSummary());
    } catch (error: any) {
      showErrorToast(error);
    }
  }, []);

  const deleteWorklog = async (worklogId: string) => {
    setIsDeletingWorklog(true);
    try {
      const result = await worklogApis.deleteWorklog(worklogId);
      showSuccessToast(result.message);
      fetchWorklogs();
      fetchWorklogSummary();
      setDeleteWorklogVisible(false);
      setSelectedWorklog(undefined);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsDeletingWorklog(false);
  };

  const tableColumns = useMemo(
    () =>
      createWorklogColumns({
        jiraLinked: loggedUserData?.jiraLinked || false,
        jiraSyncSSE: jiraSyncSSE,
        onEdit: (record) => {
          setSelectedWorklog(record);
          setManageWorklogVisible(true);
        },
        onDelete: (record) => {
          setSelectedWorklog(record);
          setDeleteWorklogVisible(true);
        },
      }),
    [loggedUserData?.jiraLinked, jiraSyncSSE],
  );

  return (
    <>
      {manageWorklogVisible && (
        <ManageWorklogModal
          setIsOpen={setManageWorklogVisible}
          refreshWorklogData={() => {
            fetchWorklogs();
            fetchWorklogSummary();
          }}
          selectedWorklog={selectedWorklog}
          setSelectedWorklog={setSelectedWorklog}
          jiraLinked={loggedUserData?.jiraLinked || false}
        />
      )}
      <WorklogModal
        title="Delete Worklog"
        properties={{
          open: deleteWorklogVisible,
          centered: true,
          okText: "Delete",
          closable: !isDeletingWorklog,
          keyboard: !isDeletingWorklog,
          maskClosable: !isDeletingWorklog,
          okButtonProps: { danger: true, loading: isDeletingWorklog, disabled: isDeletingWorklog },
          cancelButtonProps: { disabled: isDeletingWorklog },
          onOk: () => deleteWorklog(selectedWorklog?.id!),
          onCancel: () => {
            setDeleteWorklogVisible(false);
            setSelectedWorklog(undefined);
          },
        }}
      >
        <DeleteWarning
          message="Are you sure you want to delete this worklog? This action cannot be undone."
          showAlert={
            selectedWorklog?.status === WORKLOG_STATUS.SYNCED || selectedWorklog?.status === WORKLOG_STATUS.PARTIALLY
          }
          alertMessage="This worklog has synced data with Jira and will be unsynced upon deletion."
        />
      </WorklogModal>
      <AppLayout>
        <div className={classes.worklog_status_cards_container}>
          {worklogSummaryResponse?.currentMonthSummary.map((card) => (
            <WorklogStatusCard
              key={card.code}
              label={card.label}
              subLabel={card.subLabel}
              code={card.code}
              value={card.value}
            />
          ))}
        </div>
        {worklogSummaryResponse?.previousMonthLoggedHours &&
          worklogSummaryResponse?.previousMonthLoggedHours !== "0" && (
            <div className={classes.prev_month_worklog}>
              <Badge.Ribbon text="Last Month" color="#9bb9f8" styles={{ content: { color: "#000" } }}>
                <div className={classes.prev_data}>
                  Logged Hours: {worklogSummaryResponse?.previousMonthLoggedHours}
                </div>
              </Badge.Ribbon>
            </div>
          )}
        <div className={classes.worklogs_content}>
          <SearchFilter setFilters={setSearchFilters} jiraLinked={loggedUserData?.jiraLinked || false} />
          <div className={classes.worklogs_container}>
            <TrackeraTable<Worklog>
              properties={{
                columns: tableColumns,
                dataSource: worklogsResponse?.content || [],
                loading: isFetchingWorklogs,
                locale: {
                  emptyText: isFetchingWorklogs ? "Loading..." : "No worklogs found",
                },
                pagination: createPaginationConfig(worklogsResponse, fetchWorklogs, "worklogs"),
              }}
              rowKey={(record) => record.id}
              actionButtons={[
                {
                  label: "Add Worklog",
                  icon: <ClipboardPlus />,
                  onClick: () => setManageWorklogVisible(true),
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
