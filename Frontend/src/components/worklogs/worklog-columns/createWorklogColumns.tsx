import type { TableProps } from "antd";
import {
  WORKLOG_STATUS,
  type SyncPayload,
  type Worklog,
  type WorklogEvaluationType,
  type WorklogStatusType,
} from "../../../shared/types";
import { StatusBadge, statusMetadata, WorklogActionButtons, worklogEvaluationMetadata } from "../..";
import { CircleAlert } from "lucide-react";

interface WorklogColumnsParams {
  jiraLinked: boolean;
  isConnecting: boolean;
  syncRequested: boolean;
  onSync: (params: SyncPayload) => void;
  onEdit: (record: Worklog) => void;
  onDelete: (record: Worklog) => void;
}

const createWorklogColumns = (props: WorklogColumnsParams): TableProps<Worklog>["columns"] => {
  const { jiraLinked, isConnecting, syncRequested, onSync, onEdit, onDelete } = props;

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
        <StatusBadge {...worklogEvaluationMetadata[evaluation as WorklogEvaluationType]} />
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
    },
    {
      title: "Actions",
      key: "actions",
      render: (_, record) => (
        <WorklogActionButtons
          record={record}
          jiraLinked={jiraLinked}
          isConnecting={isConnecting}
          syncRequested={syncRequested}
          onSync={onSync}
          onEdit={() => onEdit(record)}
          onDelete={() => onDelete(record)}
          viewLink={`/worklog-details/${record.id}`}
          syncParams={{ worklogId: record.id, sync: record.status !== WORKLOG_STATUS.SYNCED }}
        />
      ),
    },
  ];
};

export default createWorklogColumns;
