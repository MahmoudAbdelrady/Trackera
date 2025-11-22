import { CalendarSync, CalendarX2, ExternalLink, Eye, Info, Trash } from "lucide-react";
import { AppLayout, WorklogInfo, WorklogModal, TrackeraTable, StatusBadge } from "../../components";
import classes from "./scss/worklog-details.module.css";
import trackeraTableClasses from "../../components/trackera-table/scss/trackera-table.module.css";
import { Button, Empty, Popconfirm, Skeleton, Switch, Tooltip, type TableProps } from "antd";
import { statusMetadata, type Worklog, type WorklogEntry, type WorkLogStatusType, type WorklogTask } from "../../shared/types";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import worklogModalClasses from "../../components/worklogs/modals/worklog-modal/scss/worklog-modal.module.css";
import requestInstance from "../../shared/axios/request-instance";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import { userQueries } from "../../state/queries";

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
  const [selectedWorklogTasks, setSelectedWorklogTasks] = useState<string[]>([]);
  const [viewTaskVisible, setViewTaskVisible] = useState<boolean>(false);
  const [deleteTaskVisible, setDeleteTaskVisible] = useState<boolean>(false);
  const [canFetchTasks, setCanFetchTasks] = useState<boolean>(false);
  const [isFetchingTasks, setIsFetchingTasks] = useState<boolean>(false);
  const [isDeletingTask, setIsDeletingTask] = useState<boolean>(false);

  // worklog entries
  const [worklogEntries, setWorklogEntries] = useState<WorklogEntry[]>([]);
  const [selectedEntry, setSelectedEntry] = useState<WorklogEntry | null>(null);
  const [selectedWorklogEntries, setSelectedWorklogEntries] = useState<string[]>([]);
  const [canFetchEntries, setCanFetchEntries] = useState<boolean>(false);
  const [isFetchingEntries, setIsFetchingEntries] = useState<boolean>(false);
  const [isDeletingEntry, setIsDeletingEntry] = useState<boolean>(false);

  // jira sync
  const [isSyncingJiraTasks, setIsSyncingJiraTasks] = useState<boolean>(false);
  const [isSyncingJiraEntries, setIsSyncingJiraEntries] = useState<boolean>(false);

  const worklogTaskColumns: TableProps<WorklogTask>["columns"] = [
    {
      title: "Task Name",
      dataIndex: "taskName",
      key: "taskName",
      render: (_, record) => {
        return record.taskUrl ? (
          <Link to={record.taskUrl} className={trackeraTableClasses.task_link} target="_blank">
            {record.taskName}
            <ExternalLink className={trackeraTableClasses.link_icon} />
          </Link>
        ) : (
          record.taskName
        );
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
      render: (_, { status }) => {
        return loggedUserData?.jiraLinked ? <StatusBadge badgeProps={statusMetadata[status as WorkLogStatusType]} /> : "-";
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
          {record.status === "SYNCED" ? (
            <Tooltip title={`Unsync from Jira${loggedUserData?.jiraLinked ? "" : " (Jira not linked)"}`}>
              <Button
                type="text"
                icon={<CalendarX2 />}
                onClick={() => performJiraTaskSync([record.taskName], false)}
                className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.unsync} ${!loggedUserData?.jiraLinked && trackeraTableClasses.disabled}`}
                disabled={!loggedUserData?.jiraLinked}
              />
            </Tooltip>
          ) : (
            <Tooltip title={`Sync to Jira${loggedUserData?.jiraLinked ? "" : " (Jira not linked)"}`}>
              <Button
                type="text"
                icon={<CalendarSync />}
                onClick={() => performJiraTaskSync([record.taskName], true)}
                className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.sync} ${!loggedUserData?.jiraLinked && trackeraTableClasses.disabled}`}
                disabled={!loggedUserData?.jiraLinked}
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
            <Trash
              onClick={() => {
                setDeleteTaskVisible(true);
                setSelectedTask(record);
              }}
              className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.delete}`}
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
      render: (_, { status }) => {
        return loggedUserData?.jiraLinked ? <StatusBadge badgeProps={statusMetadata[status as WorkLogStatusType]} /> : "-";
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
          {record.status === "SYNCED" ? (
            <Tooltip title={`${loggedUserData?.jiraLinked ? "Unsync from Jira" : "Link your Jira account in settings to enable this option."}`}>
              <Button
                type="text"
                icon={<CalendarX2 />}
                onClick={() => performJiraLogEntrySync([record.id], false)}
                className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.unsync} ${!loggedUserData?.jiraLinked && trackeraTableClasses.disabled}`}
                disabled={!loggedUserData?.jiraLinked}
              />
            </Tooltip>
          ) : (
            <Tooltip title={`${loggedUserData?.jiraLinked ? "Sync to Jira" : "Link your Jira account in settings to enable this option."}`}>
              <Button
                type="text"
                icon={<CalendarSync />}
                onClick={() => performJiraLogEntrySync([record.id], true)}
                className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.sync} ${!loggedUserData?.jiraLinked && trackeraTableClasses.disabled}`}
                disabled={!loggedUserData?.jiraLinked}
              />
            </Tooltip>
          )}
          <Popconfirm
            title="Are you sure to delete this entry?"
            description={
              <div className={worklogModalClasses.switch_option}>
                <span className={worklogModalClasses.label} style={{ marginRight: 8 }}>
                  Also unsync from Jira:
                </span>
                <Switch disabled={!loggedUserData?.jiraLinked} />
                {!loggedUserData?.jiraLinked && (
                  <Tooltip title="Link your Jira account in settings to enable this option.">
                    <Info size={16} color="#dc2626" style={{ marginLeft: "4px" }} />
                  </Tooltip>
                )}
              </div>
            }
            onConfirm={() => {
              setSelectedEntry(record);
              handleDeleteWorkLogEntry(record.id);
            }}
            okText="Yes"
            okButtonProps={{ danger: true, loading: isDeletingEntry, disabled: isDeletingEntry }}
            destroyOnHidden={true}
            cancelText="No"
            cancelButtonProps={{ disabled: isDeletingEntry }}
          >
            <Tooltip title="Delete">
              <Trash className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.delete} ${isDeletingEntry && selectedEntry?.id !== record.id && trackeraTableClasses.disabled}`} />
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

  const handleDeleteWorkLogTask = async () => {
    setIsDeletingTask(true);
    try {
      const response = await requestInstance.delete(`/worklog/${worklogId}/details/task?taskName=${selectedTask?.taskName}`);
      if (response.data.isLast) {
        navigate("/");
      } else {
        setCanFetchTasks(true);
        setCanFetchWorkLogInfo(true);
      }
      showSuccessToast(response.data.message);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsDeletingTask(false);
    setDeleteTaskVisible(false);
  };

  const handleDeleteWorkLogEntry = async (entryId: string | number) => {
    setIsDeletingEntry(true);
    try {
      const response = await requestInstance.delete(`/worklog/details/entry/${entryId}`);
      if (response.data.isLast) {
        navigate("/");
      } else {
        if (response.data.isLastOfTask) {
          setViewTaskVisible(false);
          setSelectedTask(null);
        } else {
          setCanFetchEntries(true);
        }
        setCanFetchTasks(true);
        setCanFetchWorkLogInfo(true);
      }
      showSuccessToast(response.data.message);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsDeletingEntry(false);
    setSelectedEntry(null);
  };

  const performJiraTaskSync = async (taskNames: string[], sync: boolean) => {
    setIsSyncingJiraTasks(true);
    await performJiraSync(worklogInfo!.id, taskNames, [], sync);
    setIsSyncingJiraTasks(false);
  };

  const performJiraLogEntrySync = async (logIds: (string | number)[], sync: boolean) => {
    setIsSyncingJiraEntries(true);
    await performJiraSync(worklogInfo!.id, [], logIds, sync);
    setIsSyncingJiraEntries(false);
  };

  const performJiraSync = async (workLogId: string | number, taskNames: string[], logIds: (string | number)[], sync: boolean) => {
    try {
      const response = await requestInstance.post(`/worklog/${workLogId}/sync${sync ? "" : "?sync=false"}`, { taskNames, logIds });
      showSuccessToast(response.data);
      setCanFetchWorkLogInfo(true);
      setCanFetchTasks(true);
      if (viewTaskVisible) {
        setCanFetchEntries(true);
      }
    } catch (error: any) {
      showErrorToast(error);
    }
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
            loading: isFetchingEntries,
          }}
        >
          <TrackeraTable<WorklogEntry>
            properties={{
              columns: worklogEntryColumns,
              dataSource: worklogEntries,
              loading: isFetchingEntries || isSyncingJiraEntries,
              rowSelection: {
                selectedRowKeys: selectedWorklogEntries,
                onChange: (_, selectedRows: WorklogEntry[]) => {
                  setSelectedWorklogEntries(selectedRows.map((row) => row.id.toString()));
                },
                getCheckboxProps: (record) => ({
                  disabled: !loggedUserData?.jiraLinked || record.status === "SYNCED",
                }),
              },
            }}
            actionButtons={[
              {
                label: `Sync to Jira${loggedUserData?.jiraLinked ? "" : " (Jira not linked)"}`,
                icon: <CalendarSync />,
                onClick: () => {},
                disabled: !loggedUserData?.jiraLinked || selectedWorklogEntries.length === 0,
              },
            ]}
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
          <div className={worklogModalClasses.switch_option}>
            <span className={worklogModalClasses.label}>Also unsync from Jira:</span>
            <Switch disabled={!loggedUserData?.jiraLinked || isDeletingTask} />
            {!loggedUserData?.jiraLinked && (
              <Tooltip title="Link your Jira account in settings to enable this option.">
                <Info size={16} color="#dc2626" />
              </Tooltip>
            )}
          </div>
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
                      selectedRowKeys: selectedWorklogTasks,
                      onChange: (_, selectedRows: WorklogTask[]) => {
                        setSelectedWorklogTasks(selectedRows.map((row) => row.id.toString()));
                      },
                      getCheckboxProps: (record) => ({
                        disabled: !loggedUserData?.jiraLinked || record.status === "SYNCED",
                      }),
                    },
                    loading: isFetchingTasks || isSyncingJiraTasks,
                  }}
                  actionButtons={[
                    {
                      label: `Sync to Jira${loggedUserData?.jiraLinked ? "" : " (Jira not linked)"}`,
                      icon: <CalendarSync />,
                      disabled: !loggedUserData?.jiraLinked || selectedWorklogTasks.length === 0,
                      onClick: () => {},
                    },
                  ]}
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
