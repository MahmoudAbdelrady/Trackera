import { Tooltip, type TableProps } from "antd";
import { statusMetadata, type SyncPayload, type WorklogEntry, type WorkLogStatusType } from "../../../shared/types";
import StatusBadge from "../../status-badge/StatusBadge";
import { CircleAlert } from "lucide-react";
import WorklogActionButtons from "../worklog-action-buttons/WorklogActionButtons";

interface WorklogEntryColumnsParams {
  worklogId: string;
  worklogEntries: WorklogEntry[];
  jiraLinked: boolean;
  onSync: (params: SyncPayload) => void;
  onDelete: (task: WorklogEntry) => void;
}

const WorkLogEntryColumns = (props: WorklogEntryColumnsParams): TableProps<WorklogEntry>["columns"] => {
  const { worklogId, worklogEntries, jiraLinked, onSync, onDelete } = props;
  return [
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
        return jiraLinked ? (
          <Tooltip title={syncError && `Sync Error: ${syncError}`}>
            <span style={{ display: "inline-block" }}>
              <StatusBadge
                badgeProps={{
                  ...statusMetadata[status as WorkLogStatusType],
                  icon: syncError ? <CircleAlert /> : undefined,
                }}
              />
            </span>
          </Tooltip>
        ) : (
          "-"
        );
      },
      ...(jiraLinked && {
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
        <WorklogActionButtons
          record={record}
          jiraLinked={jiraLinked}
          onSync={onSync}
          onDelete={() => onDelete(record)}
          syncParams={{
            workLogId: worklogId,
            entryIds: [record.id],
            sync: record.status !== "SYNCED",
          }}
        />
      ),
    },
  ];
};

export default WorkLogEntryColumns;
