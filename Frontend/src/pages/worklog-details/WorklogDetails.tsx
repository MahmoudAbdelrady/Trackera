import {
  AppLayout,
  WorklogInfo,
  WorklogModal,
  TrackeraTable,
  WorkLogTaskColumns,
  WorklogTaskEntries,
} from "../../components";
import classes from "./scss/worklog-details.module.css";
import { Alert, Empty, Skeleton } from "antd";
import {
  type Worklog,
  type WorklogEntry,
  type WorklogSelection,
  type WorkLogStatusType,
  type WorklogTask,
  JiraSyncEvent,
  WorkLogStatus,
} from "../../shared/types";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import worklogModalClasses from "../../components/worklogs/modals/worklog-modal/scss/worklog-modal.module.css";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import { userQueries } from "../../state/queries";
import buildSyncButtonProps from "../../utils/buildWorkLogSyncButtonProps";
import { useJiraSyncSSE } from "../../shared/hooks";
import { workLogApis } from "../../state/api";

const WorklogDetails = () => {
  const { worklogId } = useParams();
  const navigate = useNavigate();
  const { data: loggedUserData } = userQueries.useMeQuery();

  // worklog info
  const [worklogInfo, setWorklogInfo] = useState<Worklog | null>(null);
  const [isFetchingLogInfo, setIsFetchingLogInfo] = useState<boolean>(true);
  const [showNotFound, setShowNotFound] = useState<boolean>(false);

  // worklog tasks
  const [worklogTasks, setWorklogTasks] = useState<WorklogTask[]>([]);
  const [selectedTask, setSelectedTask] = useState<WorklogTask | null>(null);
  const [selectedTaskNames, setSelectedTaskNames] = useState<string[]>([]);
  const [taskModalState, setTaskModalState] = useState<{ type: "view" | "delete" | null; task: WorklogTask | null }>({
    type: null,
    task: null,
  });
  const [isFetchingTasks, setIsFetchingTasks] = useState<boolean>(false);
  const [isDeletingTask, setIsDeletingTask] = useState<boolean>(false);
  const selectedWorklogTasks = useMemo(
    () => worklogTasks.filter((task) => selectedTaskNames.includes(task.taskName)),
    [worklogTasks, selectedTaskNames]
  );

  // worklog entries
  const [worklogEntries, setWorklogEntries] = useState<WorklogEntry[]>([]);

  // sse subscription
  const hasInProgress = useMemo(() => {
    const inProgressStatuses: WorkLogStatusType[] = [WorkLogStatus.SYNC_IN_PROGRESS, WorkLogStatus.UNSYNC_IN_PROGRESS];
    return (
      worklogTasks.some((task) => inProgressStatuses.includes(task.status)) ||
      worklogEntries.some((entry) => inProgressStatuses.includes(entry.status)) ||
      !!(worklogInfo?.status && inProgressStatuses.includes(worklogInfo?.status))
    );
  }, [worklogTasks, worklogEntries, worklogInfo]);

  const fetchWorklogInfo = useCallback(async () => {
    if (!worklogId) return;

    setIsFetchingLogInfo(true);
    try {
      const result = await workLogApis.getWorkLogInfo(worklogId!);
      setWorklogInfo(result);
    } catch (error: any) {
      if (error.response?.status === 404) {
        setShowNotFound(true);
      } else {
        showErrorToast(error);
      }
    }
    setIsFetchingLogInfo(false);
  }, [worklogId]);

  const fetchWorklogTasks = useCallback(async () => {
    if (!worklogInfo?.id) return;

    setIsFetchingTasks(true);
    try {
      const result = await workLogApis.getWorkLogTasks(worklogId!);
      setWorklogTasks(result.map((task: WorklogTask) => ({ ...task, id: task.taskName })));
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsFetchingTasks(false);
  }, [worklogInfo?.id]);

  useEffect(() => {
    fetchWorklogInfo();
  }, [fetchWorklogInfo]);

  useEffect(() => {
    fetchWorklogTasks();
  }, [fetchWorklogTasks]);

  const handleDeleteWorkLogTask = async () => {
    setIsDeletingTask(true);

    try {
      const result = await handleWorkLogDeletion({ taskNames: [selectedTask?.taskName ?? ""] });
      if (result.isLast) {
        navigate("/");
      } else {
        fetchWorklogTasks();
        fetchWorklogInfo();
        setSelectedTaskNames([]);
      }
    } catch (error: any) {
      showErrorToast(error);
    }

    setIsDeletingTask(false);
    clearDeleteTaskModalFields();
  };

  const handleWorkLogDeletion = async (selection: WorklogSelection) => {
    const result = await workLogApis.deleteWorkLog(worklogId!, selection);
    showSuccessToast(result.message);
    return result;
  };

  const { triggerSync } = useJiraSyncSSE({
    hasInProgress,
    onStatusEvent: (event) => {
      if (event.type === JiraSyncEvent.TASK || event.type === JiraSyncEvent.ALL) {
        setWorklogTasks((prevTasks) => {
          const updatedTasks = [...prevTasks];
          updatedTasks.forEach((task, index) => {
            if (event.taskNames?.includes(task.taskName) && event.logId === worklogId) {
              updatedTasks[index] = { ...task, status: event.status, hasError: !!event.syncError };
            }
          });
          return updatedTasks;
        });
      }

      if (event.type === JiraSyncEvent.ENTRY || event.type === JiraSyncEvent.ALL) {
        setWorklogEntries((prevEntries) =>
          prevEntries.map((entry) =>
            event.entryIds?.includes(entry.id) && event.logId === worklogId
              ? { ...entry, status: event.status, syncError: event.syncError }
              : entry
          )
        );
      }

      if (event.type === JiraSyncEvent.WORKLOG || event.type === JiraSyncEvent.ALL) {
        setWorklogInfo((prev) => {
          if (!prev || event.logId !== worklogId) return prev;
          return { ...prev, status: event.status, hasError: !!event.syncError };
        });
      }
    },
  });

  const worklogTaskColumns = useMemo(
    () =>
      WorkLogTaskColumns({
        worklogId: worklogId!,
        worklogTasks: worklogTasks,
        jiraLinked: loggedUserData?.jiraLinked || false,
        onSync: triggerSync,
        onView: (record) => {
          setSelectedTask(record);
          setTaskModalState({ type: "view", task: record });
        },
        onDelete: (record) => {
          setTaskModalState({ type: "delete", task: record });
          setSelectedTask(record);
        },
      }),
    [loggedUserData?.jiraLinked, worklogId, worklogTasks, triggerSync]
  );

  const clearDeleteTaskModalFields = () => {
    setTaskModalState({ type: null, task: null });
    setSelectedTask(null);
  };

  return (
    <>
      {taskModalState.type === "view" && (
        <WorklogTaskEntries
          loggedUserData={loggedUserData!}
          worklogId={worklogId!}
          selectedTask={selectedTask!}
          worklogEntries={worklogEntries}
          setWorklogEntries={setWorklogEntries}
          refetchData={() => {
            fetchWorklogInfo();
            fetchWorklogTasks();
          }}
          triggerSync={triggerSync}
          onCloseHandler={() => {
            setSelectedTask(null);
            setTaskModalState({ type: null, task: null });
          }}
        />
      )}

      {taskModalState.type === "delete" && (
        <WorklogModal
          title={`Delete ${selectedTask?.taskName} Task Log`}
          properties={{
            open: true,
            centered: true,
            closable: !isDeletingTask,
            keyboard: !isDeletingTask,
            maskClosable: !isDeletingTask,
            okText: "Delete",
            onOk: handleDeleteWorkLogTask,
            okButtonProps: { loading: isDeletingTask, disabled: isDeletingTask, danger: true },
            cancelButtonProps: { disabled: isDeletingTask },
            onCancel: clearDeleteTaskModalFields,
          }}
        >
          <p className={worklogModalClasses.delete_message}>
            Are you sure you want to delete this task log? This action cannot be undone.
          </p>
          {(selectedTask?.status === WorkLogStatus.SYNCED || selectedTask?.status === WorkLogStatus.PARTIALLY) && (
            <Alert
              message="This task has synced data with Jira and will be unsynced upon deletion."
              type="warning"
              showIcon
              className={worklogModalClasses.alert_message}
            />
          )}
        </WorklogModal>
      )}

      <AppLayout>
        {isFetchingLogInfo ? (
          <Skeleton active paragraph={{ rows: 2 }} />
        ) : showNotFound || !worklogInfo ? (
          <Empty
            description={`${showNotFound ? "Worklog not found" : "Failed to load worklog data"}`}
            image={showNotFound ? Empty.PRESENTED_IMAGE_DEFAULT : <img src="/Assets/not_found.svg" alt="Not Found" />}
            styles={{ description: { fontSize: "16px" } }}
          />
        ) : (
          <>
            <WorklogInfo worklogInfo={worklogInfo} jiraLinked={loggedUserData?.jiraLinked ?? false} />
            <div className={classes.worklog_details_content}>
              {worklogTasks.length > 0 && (
                <TrackeraTable<WorklogTask>
                  properties={{
                    columns: worklogTaskColumns,
                    dataSource: worklogTasks,
                    rowSelection: {
                      selectedRowKeys: selectedTaskNames,
                      onChange: (_, selectedRows: WorklogTask[]) => {
                        setSelectedTaskNames(selectedRows.map((task) => task.taskName));
                      },
                      getCheckboxProps: (record) => ({
                        disabled:
                          !loggedUserData?.jiraLinked ||
                          record.status === WorkLogStatus.SYNC_IN_PROGRESS ||
                          record.status === WorkLogStatus.UNSYNC_IN_PROGRESS,
                      }),
                    },
                    loading: isFetchingTasks,
                  }}
                  rowKey={(record) => record.taskName}
                  actionButtons={buildSyncButtonProps({
                    loggedUserData: loggedUserData,
                    selectedItems: selectedWorklogTasks,
                    extractIdentifier: (task: WorklogTask) => task.taskName,
                    isEntry: false,
                    triggerSync: ({ taskNames, sync }) => triggerSync({ workLogId: worklogId, taskNames, sync }),
                  })}
                />
              )}
            </div>
          </>
        )}
      </AppLayout>
    </>
  );
};

export default WorklogDetails;
