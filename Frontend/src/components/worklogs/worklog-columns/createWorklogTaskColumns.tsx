import type { TableProps } from "antd";
import { WORKLOG_STATUS, type SyncPayload, type WorklogStatusType, type WorklogTask } from "../../../shared/types";
import StatusBadge from "../../status-badge/StatusBadge";
import { CircleAlert } from "lucide-react";
import WorkLogActionButtons from "../worklog-action-buttons/WorklogActionButtons";
import { statusMetadata } from "../worklog.metadata";

interface WorklogTaskColumnsParams {
  worklogId: string;
  worklogTasks: WorklogTask[];
  jiraLinked: boolean;
  isConnecting: boolean;
  onSync: (params: SyncPayload) => void;
  onView: (task: WorklogTask) => void;
  onDelete: (task: WorklogTask) => void;
}

const createWorklogTaskColumns = (props: WorklogTaskColumnsParams): TableProps<WorklogTask>["columns"] => {
  const { worklogId, worklogTasks, jiraLinked, isConnecting, onSync, onView, onDelete } = props;

  return [
    {
      title: "Task Name",
      dataIndex: "taskName",
      key: "taskName",
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
      render: (_, { status, hasError }) =>
        jiraLinked ? (
          <StatusBadge
            {...{
              ...statusMetadata[status as WorklogStatusType],
              icon:
                hasError &&
                status !== WORKLOG_STATUS.SYNC_IN_PROGRESS &&
                status !== WORKLOG_STATUS.UNSYNC_IN_PROGRESS ? (
                  <CircleAlert />
                ) : undefined,
            }}
          />
        ) : (
          "-"
        ),
      ...(jiraLinked && {
        filters: Array.from(new Set(worklogTasks.map((task) => task.status))).map((status) => ({
          text: statusMetadata[status as WorklogStatusType]?.label ?? status,
          value: status,
        })),
        onFilter: (value, record) => record.status === value,
      }),
    },
    {
      title: "Actions",
      key: "actions",
      render: (_, record) => (
        <WorkLogActionButtons
          record={record}
          jiraLinked={jiraLinked}
          isConnecting={isConnecting}
          onSync={onSync}
          onView={() => onView(record)}
          onDelete={() => onDelete(record)}
          syncParams={{
            worklogId: worklogId,
            taskNames: [record.taskName],
            sync: record.status !== WORKLOG_STATUS.SYNCED,
          }}
        />
      ),
    },
  ];
};

export default createWorklogTaskColumns;
