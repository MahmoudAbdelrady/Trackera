import { CalendarSync, CalendarX2, ExternalLink, Eye, Trash } from "lucide-react";
import { AppLayout, WorklogInfo, WorklogModal, WorklogTable } from "../../components";
import classes from "./scss/worklog-details.module.css";
import worklogTableClasses from "../../components/worklogs/worklog-table/scss/worklog-table.module.css";
import { Empty, Popconfirm, Skeleton, Switch, Tooltip, type TableProps } from "antd";
import { statusMetadata, type Worklog, type WorklogEntry, type WorkLogStatusType, type WorklogTask } from "../../shared/types";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { getWorklogTablePaddedItem } from "../../utils";
import worklogModalClasses from "../../components/worklogs/modals/worklog-modal/scss/worklog-modal.module.css";
import requestInstance from "../../shared/axios/request-instance";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";

const WorklogDetails = () => {
  const { worklogId } = useParams();
  const navigate = useNavigate();

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

  const worklogTaskColumns: TableProps<WorklogTask>["columns"] = [
    {
      title: "Task Name",
      dataIndex: "taskName",
      key: "taskName",
      render: (_, record) => {
        return record.taskUrl ? (
          <Link to={record.taskUrl} className={worklogTableClasses.task_link} target="_blank">
            {record.taskName}
            <ExternalLink className={worklogTableClasses.link_icon} />
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
      sorter: (a, b) => a.totalTime - b.totalTime,
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (_, { status }) => {
        return getWorklogTablePaddedItem(worklogTableClasses, "status_item", statusMetadata[status as WorkLogStatusType]);
      },
      filters: Array.from(new Set(worklogTasks.map((task) => task.status))).map((status) => ({
        text: statusMetadata[status as WorkLogStatusType]?.label ?? status,
        value: status,
      })),
      onFilter: (value, record) => record.status === value,
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
          <Tooltip title="View">
            <Eye
              className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.view}`}
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
              className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.delete}`}
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
        return getWorklogTablePaddedItem(worklogTableClasses, "status_item", statusMetadata[status as WorkLogStatusType]);
      },
      filters: Array.from(new Set(worklogEntries.map((task) => task.status))).map((status) => ({
        text: statusMetadata[status as WorkLogStatusType]?.label ?? status,
        value: status,
      })),
      onFilter: (value, record) => record.status === value,
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
          <Popconfirm
            title="Are you sure to delete this entry?"
            description={
              <div className={worklogModalClasses.switch_option}>
                <span className={worklogModalClasses.label} style={{ marginRight: 8 }}>
                  Also unsync from Jira:
                </span>
                <Switch />
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
              <Trash className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.delete} ${isDeletingEntry && selectedEntry?.id !== record.id && worklogTableClasses.disabled}`} />
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

  const handleDeleteWorkLogEntry = async (entryId: number) => {
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
          <WorklogTable<WorklogEntry>
            properties={{
              columns: worklogEntryColumns,
              dataSource: worklogEntries,
              rowSelection: {
                selectedRowKeys: selectedWorklogEntries,
                onChange: (_, selectedRows: WorklogEntry[]) => {
                  setSelectedWorklogEntries(selectedRows.map((row) => row.id.toString()));
                },
                getCheckboxProps: (record) => ({
                  disabled: record.status === "SYNCED",
                }),
              },
            }}
            actionButtons={[
              {
                label: "Sync to Jira",
                icon: <CalendarSync />,
                onClick: () => {},
                disabled: selectedWorklogEntries.length === 0,
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
            <Switch disabled={isDeletingTask} />
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
            <WorklogInfo worklogInfo={worklogInfo} />
            <div className={classes.worklog_details_content}>
              {worklogTasks.length > 0 && (
                <WorklogTable<WorklogTask>
                  properties={{
                    columns: worklogTaskColumns,
                    dataSource: worklogTasks,
                    rowSelection: {
                      selectedRowKeys: selectedWorklogTasks,
                      onChange: (_, selectedRows: WorklogTask[]) => {
                        setSelectedWorklogTasks(selectedRows.map((row) => row.id.toString()));
                      },
                      getCheckboxProps: (record) => ({
                        disabled: record.status === "SYNCED",
                      }),
                    },
                    loading: isFetchingTasks,
                  }}
                  actionButtons={[
                    {
                      label: "Sync to Jira",
                      icon: <CalendarSync />,
                      disabled: selectedWorklogTasks.length === 0,
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
