import { CalendarOff, CalendarSync, CalendarX2, CircleAlert, Eye, Trash } from "lucide-react";
import { AppLayout, WorklogInfo, WorklogModal, TrackeraTable, StatusBadge } from "../../components";
import classes from "./scss/worklog-details.module.css";
import trackeraTableClasses from "../../components/trackera-table/scss/trackera-table.module.css";
import { Alert, Button, Empty, Popconfirm, Skeleton, Tooltip, type TableProps } from "antd";
import { statusMetadata, type Worklog, type WorklogEntry, type WorklogSelection, type WorkLogStatusType, type WorklogTask } from "../../shared/types";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import worklogModalClasses from "../../components/worklogs/modals/worklog-modal/scss/worklog-modal.module.css";
import requestInstance from "../../shared/axios/request-instance";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import { userQueries } from "../../state/queries";
import buildSyncButtonProps from "../../utils/buildWorkLogSyncButtonProps";
import { useWorklogStatusSSE } from "../../shared/hooks";

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
  const [selectedWorklogEntries, setSelectedWorklogEntries] = useState<WorklogEntry[]>([]);
  const [canFetchEntries, setCanFetchEntries] = useState<boolean>(false);
  const [isFetchingEntries, setIsFetchingEntries] = useState<boolean>(false);
  const [isDeletingEntry, setIsDeletingEntry] = useState<boolean>(false);

  // jira sync
  const [isSyncingJiraTasks, setIsSyncingJiraTasks] = useState<boolean>(false);
  const [isSyncingJiraEntries, setIsSyncingJiraEntries] = useState<boolean>(false);

  // sse subscription
  const [pendingSyncPayload, setPendingSyncPayload] = useState<Record<string, any> | null>(null);
  const [forceSubscribeToSSE, setForceSubscribeToSSE] = useState<boolean>(false);
  const [sseReady, setSseReady] = useState<boolean>(false);
  const [sseAck, setSseAck] = useState<boolean>(false);
  const hasInProgress = useMemo(() => {
    const inProgressStatuses: WorkLogStatusType[] = ["SYNC_IN_PROGRESS", "UNSYNC_IN_PROGRESS"];
    return (
      worklogTasks.some((task) => inProgressStatuses.includes(task.status)) ||
      worklogEntries.some((entry) => inProgressStatuses.includes(entry.status)) ||
      inProgressStatuses.includes(worklogInfo?.status as WorkLogStatusType)
    );
  }, [worklogTasks, worklogEntries, worklogInfo]);

  const worklogTaskColumns: TableProps<WorklogTask>["columns"] = [
    {
      title: "Task Name",
      dataIndex: "taskName",
      key: "taskName",
      render: (_, record) => {
        return record.taskName;
      },
      filters: worklogTasks.map((detail) => ({
        text: detail.taskName,
        value: detail.taskName,
      })),
      onFilter: (value, record) => record.taskName.includes(value as string),
    },
    {
      title: "Total Hours",
      dataIndex: "totalHours",
      key: "totalHours",
      sorter: (a, b) => a.totalMinutes - b.totalMinutes,
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (_, { status, hasError }) => {
        return loggedUserData?.jiraLinked ? <StatusBadge badgeProps={{ ...statusMetadata[status as WorkLogStatusType], icon: hasError ? <CircleAlert /> : undefined }} /> : "-";
      },
      ...(loggedUserData?.jiraLinked && {
        filters: Array.from(new Set(worklogTasks.map((task) => task.status))).map((status) => ({
          text: statusMetadata[status as WorkLogStatusType]?.label ?? status,
          value: status,
        })),
        onFilter: (value, record) => record.status === value,
      }),
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
                onClick={() => performJiraTaskSync([record.taskName], false)}
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
                onClick={() => performJiraTaskSync([record.taskName], true)}
                className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.sync} ${
                  (!loggedUserData?.jiraLinked || record.status === "SYNC_IN_PROGRESS") && trackeraTableClasses.disabled
                }`}
                disabled={!loggedUserData?.jiraLinked || record.status === "SYNC_IN_PROGRESS"}
              />
            </Tooltip>
          )}
          <Tooltip title="View">
            <Eye
              className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.view}`}
              onClick={() => {
                setSelectedTask(record);
                setViewTaskVisible(true);
                setCanFetchEntries(true);
              }}
            />
          </Tooltip>
          <Tooltip title="Delete">
            <Button
              type="text"
              icon={<Trash />}
              onClick={() => {
                setDeleteTaskVisible(true);
                setSelectedTask(record);
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

  const worklogEntryColumns: TableProps<WorklogEntry>["columns"] = [
    {
      title: "From Time",
      dataIndex: "fromTime",
      key: "fromTime",
    },
    {
      title: "To Time",
      dataIndex: "toTime",
      key: "toTime",
    },
    {
      title: "Duration",
      dataIndex: "duration",
      key: "duration",
    },
    {
      title: "Description",
      dataIndex: "description",
      key: "description",
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (_, { status, syncError }) => {
        return loggedUserData?.jiraLinked ? (
          <Tooltip title={syncError && `Sync Error: ${syncError}`}>
            <span style={{ display: "inline-block" }}>
              <StatusBadge badgeProps={{ ...statusMetadata[status as WorkLogStatusType], icon: syncError ? <CircleAlert /> : undefined }} />
            </span>
          </Tooltip>
        ) : (
          "-"
        );
      },
      ...(loggedUserData?.jiraLinked && {
        filters: Array.from(new Set(worklogEntries.map((task) => task.status))).map((status) => ({
          text: statusMetadata[status as WorkLogStatusType]?.label ?? status,
          value: status,
        })),
        onFilter: (value, record) => record.status === value,
      }),
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
                onClick={() => performJiraLogEntrySync([record.id], false)}
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
                onClick={() => performJiraLogEntrySync([record.id], true)}
                className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.sync} ${
                  (!loggedUserData?.jiraLinked || record.status === "SYNC_IN_PROGRESS") && trackeraTableClasses.disabled
                }`}
                disabled={!loggedUserData?.jiraLinked || record.status === "SYNC_IN_PROGRESS"}
              />
            </Tooltip>
          )}
          <Popconfirm
            title="Are you sure to delete this entry?"
            description={
              record?.status === "SYNCED" && (
                <Alert message="This entry is synced with Jira and will be unsynced upon deletion." type="warning" showIcon className={worklogModalClasses.alert_message} />
              )
            }
            onConfirm={() => {
              handleDeleteWorkLogEntry(record.id);
            }}
            okText="Yes"
            okButtonProps={{ danger: true, loading: isDeletingEntry, disabled: isDeletingEntry }}
            destroyOnHidden={true}
            cancelText="No"
            cancelButtonProps={{ disabled: isDeletingEntry }}
          >
            <Tooltip title="Delete">
              <Button
                type="text"
                icon={<Trash />}
                className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.delete} ${
                  (record.status === "SYNC_IN_PROGRESS" || record.status === "UNSYNC_IN_PROGRESS") && trackeraTableClasses.disabled
                }`}
                disabled={record.status === "SYNC_IN_PROGRESS" || record.status === "UNSYNC_IN_PROGRESS"}
              />
            </Tooltip>
          </Popconfirm>
        </div>
      ),
    },
  ];

  const viewTaskModalCloseHandler = () => {
    setSelectedTask(null);
    setViewTaskVisible(false);
    selectedWorklogEntries.length > 0 && setSelectedWorklogEntries([]);
  };

  useEffect(() => {
    const fetchWorklogInfo = async () => {
      try {
        const response = await requestInstance.get(`/worklog/${worklogId}`);
        setWorklogInfo(response.data);
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
        const response = await requestInstance.get(`/worklog/${worklogId}/details`);
        setWorklogTasks(response.data.map((task: WorklogTask, idx: number) => ({ ...task, id: idx + 1 })));
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
        const response = await requestInstance.get(`/worklog/${worklogId}/details/task?taskName=${selectedTask?.taskName}`);
        setWorklogEntries(response.data);
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

  useEffect(() => {
    if (selectedWorklogEntries.length > 0) {
      const updatedSelectedEntries = selectedWorklogEntries
        .map((selectedEntry) => worklogEntries.find((entry) => entry.id === selectedEntry.id))
        .filter((entry): entry is WorklogEntry => entry !== undefined);

      setSelectedWorklogEntries(updatedSelectedEntries);
    }
  }, [worklogEntries]);

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
    setDeleteTaskVisible(false);
  };

  const handleDeleteWorkLogEntry = async (entryId: string | number) => {
    setIsDeletingEntry(true);

    try {
      const result = await handleWorkLogDeletion({ entryIds: [entryId.toString()] });
      if (result.isLast) {
        navigate("/");
      } else {
        if (result.isLastOfTask) {
          setViewTaskVisible(false);
          setSelectedTask(null);
        } else {
          setCanFetchEntries(true);
        }
        setCanFetchTasks(true);
        setCanFetchWorkLogInfo(true);
      }
    } catch (error: any) {
      showErrorToast(error);
    }

    setIsDeletingEntry(false);
  };

  const handleWorkLogDeletion = async (selection: WorklogSelection) => {
    const response = await requestInstance.delete(`/worklog/${worklogId}`, { data: { ...selection } });
    showSuccessToast(response.data.message);
    return response.data;
  };

  const performJiraTaskSync = (taskNames: string[], sync: boolean) => {
    setIsSyncingJiraTasks(true);
    triggerJiraSync(taskNames, [], sync);
    setIsSyncingJiraTasks(false);
  };

  const performJiraLogEntrySync = (entryIds: (string | number)[], sync: boolean) => {
    setIsSyncingJiraEntries(true);
    triggerJiraSync([], entryIds, sync);
    setIsSyncingJiraEntries(false);
  };

  const triggerJiraSync = (taskNames: string[], entryIds: (string | number)[], sync: boolean) => {
    setPendingSyncPayload({ taskNames, entryIds, sync });
    setForceSubscribeToSSE(true);
    setSseAck(false);
    setSseReady(false);
  };

  useWorklogStatusSSE({
    enabled: forceSubscribeToSSE || hasInProgress,
    onOpen: () => setSseReady(true),
    onStatusEvent: (event) => {
      setSseAck(true);
      if (["TASK", "ALL"].includes(event.type)) {
        setWorklogTasks((prevTasks) => prevTasks.map((task) => (event.taskNames?.includes(task.taskName) ? { ...task, status: event.status, hasError: !!event.syncError } : task)));
      }

      if (["ENTRY", "ALL"].includes(event.type)) {
        setWorklogEntries((prevEntries) => prevEntries.map((entry) => (event.entryIds?.includes(entry.id) ? { ...entry, status: event.status, syncError: event.syncError } : entry)));
      }

      if (["WORKLOG", "ALL"].includes(event.type)) {
        setWorklogInfo((prev) => {
          if (!prev) return prev;
          return { ...prev, status: event.status, hasError: !!event.syncError };
        });
      }
    },
  });

  useEffect(() => {
    if (!sseReady || !pendingSyncPayload) return;

    performJiraSync(pendingSyncPayload.taskNames ?? [], pendingSyncPayload.entryIds ?? [], pendingSyncPayload.sync ?? true);

    setPendingSyncPayload(null);
  }, [sseReady, pendingSyncPayload]);

  useEffect(() => {
    if (sseAck && !hasInProgress && !pendingSyncPayload) {
      setForceSubscribeToSSE(false);
    }
  }, [sseAck, hasInProgress, pendingSyncPayload]);

  const performJiraSync = async (taskNames: string[], entryIds: (string | number)[], sync: boolean) => {
    await requestInstance.post(`/worklog/${worklogId}/sync${sync ? "" : "?sync=false"}`, {
      taskNames,
      entryIds,
    });
  };

  return (
    <>
      {viewTaskVisible && (
        <WorklogModal
          title={`${selectedTask?.taskName} Task Logs`}
          properties={{
            open: true,
            centered: true,
            footer: null,
            width: "80%",
            onCancel: viewTaskModalCloseHandler,
          }}
        >
          <TrackeraTable<WorklogEntry>
            properties={{
              columns: worklogEntryColumns,
              dataSource: worklogEntries,
              loading: isFetchingEntries || isSyncingJiraEntries,
              pagination: { pageSize: 5 },
              rowSelection: {
                selectedRowKeys: selectedWorklogEntries.map((entry) => entry.id.toString()),
                onChange: (_, selectedRows: WorklogEntry[]) => {
                  setSelectedWorklogEntries(selectedRows);
                },
                getCheckboxProps: (record) => ({
                  disabled: !loggedUserData?.jiraLinked || ["SYNC_IN_PROGRESS", "UNSYNC_IN_PROGRESS"].includes(record.status),
                }),
              },
            }}
            actionButtons={buildSyncButtonProps({
              loggedUserData: loggedUserData,
              selectedItems: selectedWorklogEntries,
              extractIdentifier: (entry: WorklogEntry) => entry.id.toString(),
              performSync: performJiraLogEntrySync,
            })}
          />
        </WorklogModal>
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
            onCancel: () => setDeleteTaskVisible(false),
          }}
        >
          <p className={worklogModalClasses.delete_message}>Are you sure you want to delete this task log? This action cannot be undone.</p>
          {(selectedTask?.status === "SYNCED" || selectedTask?.status === "PARTIALLY") && (
            <Alert message="This task has synced data with Jira and will be unsynced upon deletion." type="warning" showIcon className={worklogModalClasses.alert_message} />
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
                        disabled: !loggedUserData?.jiraLinked || ["SYNC_IN_PROGRESS", "UNSYNC_IN_PROGRESS"].includes(record.status),
                      }),
                    },
                    loading: isFetchingTasks || isSyncingJiraTasks,
                  }}
                  actionButtons={buildSyncButtonProps({
                    loggedUserData: loggedUserData,
                    selectedItems: selectedWorklogTasks,
                    extractIdentifier: (task: WorklogTask) => task.taskName,
                    performSync: performJiraTaskSync,
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
