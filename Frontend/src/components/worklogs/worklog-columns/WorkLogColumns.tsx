import type { TableProps } from "antd";
import {
  WorkLogStatus,
  type SyncPayload,
  type Worklog,
  type WorkLogEvaluationType,
  type WorkLogStatusType,
} from "../../../shared/types";
import { StatusBadge, statusMetadata, WorklogActionButtons, worklogEvaluationMetadata } from "../..";
import { CircleAlert } from "lucide-react";

interface WorklogColumnsParams {
  jiraLinked: boolean;
  onSync: (params: SyncPayload) => void;
  onEdit: (record: Worklog) => void;
  onDelete: (record: Worklog) => void;
}

const WorkLogColumns = (props: WorklogColumnsParams): TableProps<Worklog>["columns"] => {
  const { jiraLinked, onSync, onEdit, onDelete } = props;

  return [
    {
      title: "Log Name",
      dataIndex: "name",
      key: "name",
    },
    {
      title: "Total Time",
      dataIndex: "totalTime",
      key: "totalTime",
    },
    {
      title: "Date",
      dataIndex: "workDate",
      key: "workDate",
    },
    {
      title: "Evaluation",
      dataIndex: "evaluation",
      key: "evaluation",
      render: (_, { evaluation }) => (
        <StatusBadge {...worklogEvaluationMetadata[evaluation as WorkLogEvaluationType]} />
      ),
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (_, { status, hasError }) =>
        jiraLinked ? (
          <StatusBadge
            {...{
              ...statusMetadata[status as WorkLogStatusType],
              icon:
                hasError && status !== WorkLogStatus.SYNC_IN_PROGRESS && status !== WorkLogStatus.UNSYNC_IN_PROGRESS ? (
                  <CircleAlert />
                ) : undefined,
            }}
          />
        ) : (
          "-"
        ),
    },
    {
      title: "Actions",
      key: "actions",
      render: (_, record) => (
        <WorklogActionButtons
          record={record}
          jiraLinked={jiraLinked}
          onSync={onSync}
          onEdit={() => onEdit(record)}
          onDelete={() => onDelete(record)}
          viewLink={`/worklog-details/${record.id}`}
          syncParams={{ workLogId: record.id, sync: record.status !== WorkLogStatus.SYNCED }}
        />
      ),
    },
  ];
};

export default WorkLogColumns;
