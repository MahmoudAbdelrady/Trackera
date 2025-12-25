import type { TableProps } from "antd";
import {
  statusMetadata,
  WorkLogStatus,
  type SyncPayload,
  type WorkLogStatusType,
  type WorklogTask,
} from "../../../shared/types";
import StatusBadge from "../../status-badge/StatusBadge";
import { CircleAlert } from "lucide-react";
import WorklogActionButtons from "../worklog-action-buttons/WorklogActionButtons";

interface WorklogTaskColumnsParams {
  worklogId: string;
  worklogTasks: WorklogTask[];
  jiraLinked: boolean;
  onSync: (params: SyncPayload) => void;
  onView: (task: WorklogTask) => void;
  onDelete: (task: WorklogTask) => void;
}

const WorkLogTaskColumns = (props: WorklogTaskColumnsParams): TableProps<WorklogTask>["columns"] => {
  const { worklogId, worklogTasks, jiraLinked, onSync, onView, onDelete } = props;
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
            badgeProps={{
              ...statusMetadata[status as WorkLogStatusType],
              icon: hasError ? <CircleAlert /> : undefined,
            }}
          />
        ) : (
          "-"
        ),
      ...(jiraLinked && {
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
        <WorklogActionButtons
          record={record}
          jiraLinked={jiraLinked}
          onSync={onSync}
          onView={() => onView(record)}
          onDelete={() => onDelete(record)}
          syncParams={{
            workLogId: worklogId,
            taskNames: [record.taskName],
            sync: record.status !== WorkLogStatus.SYNCED,
          }}
        />
      ),
    },
  ];
};

export default WorkLogTaskColumns;
