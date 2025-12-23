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
} from "../../shared/types";
import { useEffect, useMemo, useState } from "react";
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
  const [canFetchWorkLogInfo, setCanFetchWorkLogInfo] = useState<boolean>(!!worklogId);
  const [showNotFound, setShowNotFound] = useState<boolean>(false);

  // worklog tasks
  const [worklogTasks, setWorklogTasks] = useState<WorklogTask[]>([]);
  const [selectedTask, setSelectedTask] = useState<WorklogTask | null>(null);
  const [selectedWorklogTasks, setSelectedWorklogTasks] = useState<WorklogTask[]>([]);
  const [viewTaskVisible, setViewTaskVisible] = useState<boolean>(false);
  const [deleteTaskVisible, setDeleteTaskVisible] = useState<boolean>(false);
  const [canFetchTasks, setCanFetchTasks] = useState<boolean>(false);
  const [isFetchingTasks, setIsFetchingTasks] = useState<boolean>(false);
  const [isDeletingTask, setIsDeletingTask] = useState<boolean>(false);

  // worklog entries
  const [worklogEntries, setWorklogEntries] = useState<WorklogEntry[]>([]);
  const [canFetchEntries, setCanFetchEntries] = useState<boolean>(false);
  const [isFetchingEntries, setIsFetchingEntries] = useState<boolean>(false);

  // sse subscription
  const hasInProgress = useMemo(() => {
    const inProgressStatuses: WorkLogStatusType[] = ["SYNC_IN_PROGRESS", "UNSYNC_IN_PROGRESS"];
    return (
      worklogTasks.some((task) => inProgressStatuses.includes(task.status)) ||
      worklogEntries.some((entry) => inProgressStatuses.includes(entry.status)) ||
      inProgressStatuses.includes(worklogInfo?.status as WorkLogStatusType)
    );
  }, [worklogTasks, worklogEntries, worklogInfo]);

  useEffect(() => {
    const fetchWorklogInfo = async () => {
      try {
        const result = await workLogApis.getWorkLogInfo(worklogId!);
        setWorklogInfo(result);
        setCanFetchTasks(true);
      } catch (error: any) {
        if (error.response?.status === 404) {
          setShowNotFound(true);
        } else {
          showErrorToast(error);
        }
      }
      setIsFetchingLogInfo(false);
    };

    if (canFetchWorkLogInfo) {
      fetchWorklogInfo();
      setCanFetchWorkLogInfo(false);
    }
  }, [canFetchWorkLogInfo]);

  useEffect(() => {
    const fetchWorklogTasks = async () => {
      setIsFetchingTasks(true);
      try {
        const result = await workLogApis.getWorkLogTasks(worklogId!);
        setWorklogTasks(result.map((task: WorklogTask, idx: number) => ({ ...task, id: idx + 1 })));
      } catch (error: any) {
        showErrorToast(error);
      }
      setIsFetchingTasks(false);
    };

    if (canFetchTasks) {
      fetchWorklogTasks();
      setCanFetchTasks(false);
    }
  }, [canFetchTasks]);

  useEffect(() => {
    const fetchTaskEntries = async () => {
      setIsFetchingEntries(true);
      try {
        const result = await workLogApis.getWorkLogTaskEntries(worklogId!, selectedTask!.taskName);
        setWorklogEntries(result);
      } catch (error: any) {
        showErrorToast(error);
      }
      setIsFetchingEntries(false);
    };

    if (canFetchEntries) {
      fetchTaskEntries();
      setCanFetchEntries(false);
    }
  }, [canFetchEntries]);

  useEffect(() => {
    if (selectedWorklogTasks.length > 0) {
      const updatedSelectedTasks = selectedWorklogTasks
        .map((selectedTask) => worklogTasks.find((task) => task.taskName === selectedTask.taskName))
        .filter((task): task is WorklogTask => task !== undefined);

      setSelectedWorklogTasks(updatedSelectedTasks);
    }
  }, [worklogTasks]);

  const handleDeleteWorkLogTask = async () => {
    setIsDeletingTask(true);

    try {
      const result = await handleWorkLogDeletion({ taskNames: [selectedTask?.taskName ?? ""] });
      if (result.isLast) {
        navigate("/");
      } else {
        setCanFetchTasks(true);
        setCanFetchWorkLogInfo(true);
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
    hasInProgress: hasInProgress,
    onStatusEvent: (event) => {
      if (["TASK", "ALL"].includes(event.type)) {
        setWorklogTasks((prevTasks) =>
          prevTasks.map((task) =>
            event.taskNames?.includes(task.taskName)
              ? { ...task, status: event.status, hasError: !!event.syncError }
              : task
          )
        );
      }

      if (["ENTRY", "ALL"].includes(event.type)) {
        setWorklogEntries((prevEntries) =>
          prevEntries.map((entry) =>
            event.entryIds?.includes(entry.id) ? { ...entry, status: event.status, syncError: event.syncError } : entry
          )
        );
      }

      if (["WORKLOG", "ALL"].includes(event.type)) {
        setWorklogInfo((prev) => {
          if (!prev) return prev;
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
          setViewTaskVisible(true);
          setCanFetchEntries(true);
        },
        onDelete: (record) => {
          setDeleteTaskVisible(true);
          setSelectedTask(record);
        },
      }),
    [loggedUserData?.jiraLinked, triggerSync]
  );

  const clearDeleteTaskModalFields = () => {
    setDeleteTaskVisible(false);
    setSelectedTask(null);
  };

  return (
    <>
      {viewTaskVisible && (
        <WorklogTaskEntries
          loggedUserData={loggedUserData!}
          worklogId={worklogId!}
          selectedTask={selectedTask!}
          worklogEntries={worklogEntries}
          isFetchingEntries={isFetchingEntries}
          setCanFetchEntries={setCanFetchEntries}
          refetchData={() => {
            setCanFetchTasks(true);
            setCanFetchWorkLogInfo(true);
          }}
          triggerSync={triggerSync}
          onCloseHandler={() => {
            setSelectedTask(null);
            setViewTaskVisible(false);
          }}
        />
      )}

      {deleteTaskVisible && (
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
          {(selectedTask?.status === "SYNCED" || selectedTask?.status === "PARTIALLY") && (
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
                      selectedRowKeys: selectedWorklogTasks.map((task) => task.id.toString()),
                      onChange: (_, selectedRows: WorklogTask[]) => {
                        setSelectedWorklogTasks(selectedRows);
                      },
                      getCheckboxProps: (record) => ({
                        disabled:
                          !loggedUserData?.jiraLinked ||
                          ["SYNC_IN_PROGRESS", "UNSYNC_IN_PROGRESS"].includes(record.status),
                      }),
                    },
                    loading: isFetchingTasks,
                  }}
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
