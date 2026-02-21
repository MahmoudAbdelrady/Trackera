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
import type { JiraSyncSSEReturn } from "../../../shared/hooks/useJiraSyncSSE";

interface ActionButtonsProps {
  record: Worklog | WorklogTask | WorklogEntry;
  jiraLinked: boolean;
  viewLink?: string;
  syncParams: SyncPayload;
  jiraSyncSSE: JiraSyncSSEReturn;
  onEdit?: () => void;
  onView?: () => void;
  onDelete: () => void;
}

const WorklogActionButtons = (props: ActionButtonsProps) => {
  const { record, jiraLinked, viewLink, syncParams, jiraSyncSSE, onEdit, onView, onDelete } = props;

  const isSynced = record.status === WORKLOG_STATUS.SYNCED;
  const isInQueue = record.status === WORKLOG_STATUS.IN_QUEUE;
  const isSyncing = record.status === WORKLOG_STATUS.SYNC_IN_PROGRESS;
  const isUnsyncing = record.status === WORKLOG_STATUS.UNSYNC_IN_PROGRESS;

  const isSyncingCurrentRecord = () => {
    const { activeSyncPayload } = jiraSyncSSE;
    if (!activeSyncPayload) return false;
    if (activeSyncPayload.taskNames?.length) {
      return activeSyncPayload.taskNames.includes((record as WorklogTask).taskName);
    } else if (activeSyncPayload.entryIds?.length) {
      return activeSyncPayload.entryIds.includes((record as WorklogEntry).id);
    } else if (activeSyncPayload.worklogId && !activeSyncPayload.taskNames && !activeSyncPayload.entryIds) {
      return activeSyncPayload.worklogId === (record as Worklog).id;
    }
    return false;
  };

  const isRequestingSyncForThisRecord = jiraSyncSSE.isRequestingSync && isSyncingCurrentRecord();
  const syncInProgress = isInQueue || isSyncing || isUnsyncing || isRequestingSyncForThisRecord;
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
          onClick={() => jiraSyncSSE.triggerSync(syncParams)}
          className={`${classes.log_action_btn} ${isSynced ? classes.unsync : classes.sync} ${
            isDisabled && classes.disabled
          }`}
          disabled={isDisabled}
          loading={isRequestingSyncForThisRecord}
        />
      </Tooltip>

      {onEdit && (
        <Tooltip title="Edit">
          <Button
            type="text"
            icon={<SquarePen />}
            onClick={onEdit}
            className={`${classes.log_action_btn} ${classes.edit} ${syncInProgress && classes.disabled}`}
            disabled={syncInProgress}
          />
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
