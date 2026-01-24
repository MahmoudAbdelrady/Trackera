import {
  WORKLOG_STATUS,
  type SyncPayload,
  type Worklog,
  type WorklogEntry,
  type WorklogTask,
} from "../../../shared/types";
import { Button, Tooltip } from "antd";
import { CalendarOff, CalendarSync, CalendarX2, Eye, SquarePen, Trash } from "lucide-react";
import { Link } from "react-router-dom";
import classes from "./scss/worklog-action-buttons.module.css";

interface ActionButtonsProps {
  record: Worklog | WorklogTask | WorklogEntry;
  jiraLinked: boolean;
  viewLink?: string;
  syncParams: SyncPayload;
  isConnecting: boolean;
  syncRequested: boolean;
  onSync: (params: SyncPayload) => void;
  onEdit?: () => void;
  onView?: () => void;
  onDelete: () => void;
}

const WorklogActionButtons = (props: ActionButtonsProps) => {
  const { record, jiraLinked, viewLink, syncParams, isConnecting, syncRequested, onSync, onEdit, onView, onDelete } =
    props;

  const isSynced = record.status === WORKLOG_STATUS.SYNCED;
  const isSyncing = record.status === WORKLOG_STATUS.SYNC_IN_PROGRESS;
  const isUnsyncing = record.status === WORKLOG_STATUS.UNSYNC_IN_PROGRESS;
  const syncInProgress = isSyncing || isUnsyncing || isConnecting || syncRequested;
  const isDisabled = !jiraLinked || syncInProgress;

  const getTooltipTitle = () => {
    if (!jiraLinked) {
      return "Link your Jira account in settings to enable this option.";
    }
    return isSynced || isUnsyncing ? "Unsync from Jira" : "Sync to Jira";
  };

  const getIcon = () => {
    if (!jiraLinked) return <CalendarOff />;
    return isSynced || isUnsyncing ? <CalendarX2 /> : <CalendarSync />;
  };

  return (
    <div className={classes.actions_container}>
      <Tooltip title={getTooltipTitle()}>
        <Button
          type="text"
          icon={getIcon()}
          onClick={() => onSync(syncParams)}
          className={`${classes.log_action_btn} ${isSynced ? classes.unsync : classes.sync} ${
            isDisabled && classes.disabled
          }`}
          disabled={isDisabled}
        />
      </Tooltip>

      {onEdit && (
        <Tooltip title="Edit">
          <SquarePen onClick={onEdit} className={`${classes.log_action_btn} ${classes.edit}`} />
        </Tooltip>
      )}

      {(onView || viewLink) && (
        <Tooltip title="View">
          {viewLink ? (
            <Link to={viewLink} className={`${classes.log_action_btn} ${classes.view}`}>
              <Eye />
            </Link>
          ) : (
            <Eye onClick={onView} className={`${classes.log_action_btn} ${classes.view}`} />
          )}
        </Tooltip>
      )}

      <Tooltip title="Delete">
        <Button
          type="text"
          icon={<Trash />}
          onClick={onDelete}
          className={`${classes.log_action_btn} ${classes.delete} ${syncInProgress && classes.disabled}`}
          disabled={syncInProgress}
        />
      </Tooltip>
    </div>
  );
};

export default WorklogActionButtons;
