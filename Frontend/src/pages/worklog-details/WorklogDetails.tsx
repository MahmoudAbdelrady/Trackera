import { CalendarSync, CalendarX2, ExternalLink, Eye, Trash } from "lucide-react";
import { AppLayout, WorklogInfo, WorklogModal, WorklogTable } from "../../components";
import classes from "./scss/worklog-details.module.css";
import worklogTableClasses from "../../components/worklogs/worklog-table/scss/worklog-table.module.css";
import { Empty, Skeleton, Switch, Tooltip, type TableProps } from "antd";
import { statusMetadata, type Worklog, type WorklogEntry, type WorkLogStatusType, type WorklogTask } from "../../shared/types";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getWorklogTablePaddedItem } from "../../utils";
import worklogModalClasses from "../../components/worklogs/modals/worklog-modal/scss/worklog-modal.module.css";
import requestInstance from "../../shared/axios/request-instance";
import { showErrorToast } from "../../utils/toast-handler/showToast";

const WorklogDetails = () => {
  const { worklogId } = useParams();
  const [isFetchingLogInfo, setIsFetchingLogInfo] = useState<boolean>(true);
  const [canFetchDetails, setCanFetchDetails] = useState<boolean>(false);
  const [isFetchingDetails, setIsFetchingDetails] = useState<boolean>(false);
  const [isFetchingTasks, setIsFetchingTasks] = useState<boolean>(false);
  const [worklogInfo, setWorklogInfo] = useState<Worklog | null>(null);
  const [showNotFound, setShowNotFound] = useState<boolean>(false);
  const [worklogTasks, setWorklogTasks] = useState<WorklogTask[]>([]);
  const [viewDetailTask, setViewDetailTask] = useState<WorklogTask | null>(null);
  const [deleteDetailVisible, setDeleteDetailVisible] = useState<boolean>(false);
  const [worklogEntries, setWorkLogEntries] = useState<WorklogEntry[]>([]);

  const [selectedLogDetails, setSelectedLogDetails] = useState<string[]>([]);
  const [selectedWorklogTasks, setSelectedWorklogTasks] = useState<string[]>([]);

  // const worklogEntries: WorklogEntry[] = [
  //   {
  //     id: 1,
  //     fromTime: "13:00",
  //     toTime: "15:00",
  //     description: "Worked on feature X",
  //     status: "SYNCED",
  //   },
  //   {
  //     id: 2,
  //     fromTime: "15:30",
  //     toTime: "17:00",
  //     description: "Fixed bug Y",
  //     status: "SYNCED",
  //   },
  //   {
  //     id: 3,
  //     fromTime: "09:00",
  //     toTime: "11:00",
  //     description: "Reviewed PR Z",
  //     status: "NOT_SYNCED",
  //   },
  //   {
  //     id: 4,
  //     fromTime: "11:30",
  //     toTime: "12:30",
  //     description: "Team meeting",
  //     status: "NOT_SYNCED",
  //   },
  // ];

  const logDetailsColumns: TableProps<WorklogTask>["columns"] = [
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
      sorter: (a, b) => a.totalHours - b.totalHours,
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (_, { status }) => {
        return getWorklogTablePaddedItem(worklogTableClasses, "status_item", statusMetadata[status as WorkLogStatusType]);
      },
      filters: Object.entries(statusMetadata).map(([key, value]) => ({
        text: value.label,
        value: key,
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
            <Eye className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.view}`} onClick={() => setViewDetailTask(record)} />
          </Tooltip>
          <Tooltip title="Delete">
            <Trash onClick={() => setDeleteDetailVisible(true)} className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.delete}`} />
          </Tooltip>
        </div>
      ),
    },
  ];

  const taskLogsColumns: TableProps<WorklogEntry>["columns"] = [
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
      filters: Object.entries(statusMetadata)
        .filter(([key]) => key != "PARTIALLY")
        .map(([key, value]) => ({
          text: value.label,
          value: key,
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
          <Tooltip title="Delete">
            <Trash onClick={() => {}} className={`${worklogTableClasses.log_action_btn} ${worklogTableClasses.delete}`} />
          </Tooltip>
        </div>
      ),
    },
  ];

  const viewTaskModalCloseHandler = () => {
    setViewDetailTask(null);
    setSelectedWorklogTasks([]);
  };

  useEffect(() => {
    if (!worklogId) return;
    const fetchWorklogInfo = async () => {
      try {
        const response = await requestInstance.get(`/worklog/${worklogId}`);
        setWorklogInfo(response.data);
        setCanFetchDetails(true);
      } catch (error: any) {
        if (error.response?.status === 404) {
          setShowNotFound(true);
        } else {
          showErrorToast(error);
        }
      }
      setIsFetchingLogInfo(false);
    };

    fetchWorklogInfo();
  }, [worklogId]);

  useEffect(() => {
    if (!canFetchDetails) return;
    const fetchWorklogDetails = async () => {
      setIsFetchingDetails(true);
      try {
        const response = await requestInstance.get(`/worklog/${worklogId}/details`);
        setWorklogTasks(response.data.map((task: WorklogTask, idx: number) => ({ ...task, id: idx + 1 })));
      } catch (error: any) {
        showErrorToast(error);
      }
      setIsFetchingDetails(false);
    };

    fetchWorklogDetails();
  }, [canFetchDetails]);

  useEffect(() => {
    if (!viewDetailTask) return;
    const fetchTaskDetails = async () => {
      setIsFetchingTasks(true);
      try {
        const response = await requestInstance.get(`/worklog/${worklogId}/details/task?taskName=${viewDetailTask.taskName}`);
        setWorkLogEntries(response.data);
      } catch (error: any) {
        showErrorToast(error);
      }
      setIsFetchingTasks(false);
    };

    fetchTaskDetails();
  }, [viewDetailTask]);

  return (
    <>
      {viewDetailTask && (
        <WorklogModal
          title={`${viewDetailTask?.taskName} Task Logs`}
          properties={{
            open: true,
            centered: true,
            footer: null,
            width: "80%",
            onCancel: viewTaskModalCloseHandler,
            loading: isFetchingTasks,
          }}
        >
          <WorklogTable<WorklogEntry>
            properties={{
              columns: taskLogsColumns,
              dataSource: worklogEntries,
              rowSelection: {
                selectedRowKeys: selectedWorklogTasks,
                onChange: (_, selectedRows: WorklogEntry[]) => {
                  setSelectedWorklogTasks(selectedRows.map((row) => row.id.toString()));
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
                disabled: selectedWorklogTasks.length === 0,
              },
            ]}
          />
        </WorklogModal>
      )}

      {deleteDetailVisible && (
        <WorklogModal
          title="Delete Task Log"
          properties={{
            open: true,
            centered: true,
            okText: "Delete",
            onOk: () => {},
            onCancel: () => setDeleteDetailVisible(false),
            okButtonProps: { danger: true },
          }}
        >
          <p className={worklogModalClasses.delete_message}>Are you sure you want to delete this task log? This action cannot be undone.</p>
          <div className={worklogModalClasses.switch_option}>
            <span className={worklogModalClasses.label}>Also unsync from Jira:</span>
            <Switch />
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
              {canFetchDetails && (
                <WorklogTable<WorklogTask>
                  properties={{
                    columns: logDetailsColumns,
                    dataSource: worklogTasks,
                    rowSelection: {
                      selectedRowKeys: selectedLogDetails,
                      onChange: (_, selectedRows: WorklogTask[]) => {
                        setSelectedLogDetails(selectedRows.map((row) => row.id.toString()));
                      },
                      getCheckboxProps: (record) => ({
                        disabled: record.status === "SYNCED",
                      }),
                    },
                    loading: isFetchingDetails,
                  }}
                  actionButtons={[
                    {
                      label: "Sync to Jira",
                      icon: <CalendarSync />,
                      disabled: selectedLogDetails.length === 0,
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
