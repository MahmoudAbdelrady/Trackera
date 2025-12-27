import {
  WorkLogStatus,
  type SyncPayload,
  type Worklog,
  type WorklogEntry,
  type WorklogTask,
} from "../../../shared/types";
import trackeraTableClasses from "../../trackera-table/scss/trackera-table.module.css";
import { WorklogSyncActions } from "../../";
import { Button, Tooltip } from "antd";
import { Eye, SquarePen, Trash } from "lucide-react";
import { Link } from "react-router-dom";

interface ActionButtonsProps {
  record: Worklog | WorklogTask | WorklogEntry;
  jiraLinked: boolean;
  viewLink?: string;
  syncParams: SyncPayload;
  onSync: (params: SyncPayload) => void;
  onEdit?: () => void;
  onView?: () => void;
  onDelete: () => void;
}

const WorklogActionButtons = (props: ActionButtonsProps) => {
  const { record, jiraLinked, viewLink, syncParams, onSync, onEdit, onView, onDelete } = props;
  const syncInProgress =
    record.status === WorkLogStatus.SYNC_IN_PROGRESS || record.status === WorkLogStatus.UNSYNC_IN_PROGRESS;

  return (
    <div className={trackeraTableClasses.actions_container}>
      <WorklogSyncActions record={record} jiraLinked={jiraLinked} onSync={onSync} syncParams={syncParams} />

      {onEdit && (
        <Tooltip title="Edit">
          <SquarePen
            onClick={onEdit}
            className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.edit}`}
          />
        </Tooltip>
      )}

      {(onView || viewLink) && (
        <Tooltip title="View">
          {viewLink ? (
            <Link to={viewLink} className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.view}`}>
              <Eye />
            </Link>
          ) : (
            <Eye onClick={onView} className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.view}`} />
          )}
        </Tooltip>
      )}

      <Tooltip title="Delete">
        <Button
          type="text"
          icon={<Trash />}
          onClick={onDelete}
          className={`${trackeraTableClasses.log_action_btn} ${trackeraTableClasses.delete} ${
            syncInProgress && trackeraTableClasses.disabled
          }`}
          disabled={syncInProgress}
        />
      </Tooltip>
    </div>
  );
};

export default WorklogActionButtons;
