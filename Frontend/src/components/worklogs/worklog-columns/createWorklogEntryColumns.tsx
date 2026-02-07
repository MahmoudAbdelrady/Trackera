import { Tooltip, type TableProps } from "antd";
import { WORKLOG_STATUS, type WorklogEntry, type WorklogStatusType } from "../../../shared/types";
import StatusBadge from "../../status-badge/StatusBadge";
import { CircleAlert } from "lucide-react";
import WorkLogActionButtons from "../worklog-action-buttons/WorklogActionButtons";
import { statusMetadata } from "../worklog.metadata";
import type { JiraSyncSSEReturn } from "../../../shared/hooks/useJiraSyncSSE";

interface WorklogEntryColumnsParams {
  worklogId: string;
  worklogEntries: WorklogEntry[];
  jiraLinked: boolean;
  jiraSyncSSE: JiraSyncSSEReturn;
  onEdit: (entry: WorklogEntry) => void;
  onDelete: (task: WorklogEntry) => void;
}

const createWorklogEntryColumns = (props: WorklogEntryColumnsParams): TableProps<WorklogEntry>["columns"] => {
  const { worklogId, worklogEntries, jiraLinked, jiraSyncSSE, onEdit, onDelete } = props;

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
                {...{
                  ...statusMetadata[status as WorklogStatusType],
                  icon:
                    syncError &&
                    status !== WORKLOG_STATUS.SYNC_IN_PROGRESS &&
                    status !== WORKLOG_STATUS.UNSYNC_IN_PROGRESS ? (
                      <CircleAlert />
                    ) : undefined,
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
          jiraSyncSSE={jiraSyncSSE}
          onEdit={() => onEdit(record)}
          onDelete={() => onDelete(record)}
          syncParams={{
            worklogId: worklogId,
            entryIds: [record.id],
            sync: record.status !== WORKLOG_STATUS.SYNCED,
          }}
        />
      ),
    },
  ];
};

export default createWorklogEntryColumns;
