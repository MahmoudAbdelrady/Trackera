import { CalendarOff, CalendarSync, CalendarX2 } from "lucide-react";
import { WorkLogStatus, type SyncPayload, type WorkLogStatusType } from "../../../shared/types";
import { Button, Tooltip } from "antd";
import trackeraTableClasses from "../../trackera-table/scss/trackera-table.module.css";

interface SyncActionProps {
  record: { status: WorkLogStatusType };
  jiraLinked: boolean;
  onSync: (params: SyncPayload) => void;
  syncParams: SyncPayload;
}

const WorklogSyncActions = (props: SyncActionProps) => {
  const { record, jiraLinked, onSync, syncParams } = props;

  const isSynced = record.status === WorkLogStatus.SYNCED;
  const isSyncing = record.status === WorkLogStatus.SYNC_IN_PROGRESS;
  const isUnsyncing = record.status === WorkLogStatus.UNSYNC_IN_PROGRESS;

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

  const isDisabled = !jiraLinked || isSyncing || isUnsyncing;

  return (
    <Tooltip title={getTooltipTitle()}>
      <Button
        type="text"
        icon={getIcon()}
        onClick={() => onSync(syncParams)}
        className={`${trackeraTableClasses.log_action_btn} ${
          isSynced ? trackeraTableClasses.unsync : trackeraTableClasses.sync
        } ${isDisabled && trackeraTableClasses.disabled}`}
        disabled={isDisabled}
      />
    </Tooltip>
  );
};

export default WorklogSyncActions;
