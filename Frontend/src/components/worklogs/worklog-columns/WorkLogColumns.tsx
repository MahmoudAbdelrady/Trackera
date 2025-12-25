import type { TableProps } from "antd";
import {
  statusMetadata,
  worklogEvaluationMetadata,
  WorkLogStatus,
  type Worklog,
  type WorkLogEvaluationType,
  type WorkLogStatusType,
} from "../../../shared/types";
import { StatusBadge, WorklogActionButtons } from "../..";
import { CircleAlert } from "lucide-react";

interface WorklogColumnsParams {
  jiraLinked: boolean;
  onSync: (params: any) => void;
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
        <StatusBadge badgeProps={worklogEvaluationMetadata[evaluation as WorkLogEvaluationType]} />
      ),
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
