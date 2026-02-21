import classes from "./scss/worklog-info.module.css";
import { Calendar, CircleAlert, Clock, RefreshCcw, Target } from "lucide-react";
import { Tooltip } from "antd";
import {
  type Worklog,
  type WorklogEvaluationType,
  type WorklogStatusType,
  WORKLOG_STATUS,
} from "../../../shared/types";
import { StatusBadge, statusMetadata, worklogEvaluationMetadata } from "../..";

interface WorklogInfoProps {
  worklogInfo: Worklog;
  jiraLinked: boolean;
}

const WorklogInfo = (props: WorklogInfoProps) => {
  const { worklogInfo, jiraLinked } = props;
  const evaluationMetaItem = worklogEvaluationMetadata[worklogInfo?.evaluation as WorklogEvaluationType];
  const statusMetaItem = statusMetadata[worklogInfo?.status as WorklogStatusType];

  return (
    <div className={classes.worklog_info}>
      <h3 className={classes.title}>{worklogInfo?.name}</h3>
      <div className={classes.info}>
        <div className={classes.info_box}>
          <Tooltip title="Total Hours">
            <Clock className={classes.info_icon} />
          </Tooltip>
          <span className={classes.info_label}>{worklogInfo?.totalTime}</span>
        </div>
        <div className={classes.info_box}>
          <Tooltip title="Date">
            <Calendar className={classes.info_icon} />
          </Tooltip>
          <span className={classes.info_label}>{worklogInfo?.workDate}</span>
        </div>
        <div className={classes.info_box}>
          <Tooltip title="Evaluation">
            <Target className={classes.info_icon} />
          </Tooltip>
          <StatusBadge {...evaluationMetaItem} />
        </div>
        <div className={classes.info_box}>
          <Tooltip title={`Status${jiraLinked ? "" : " (Jira not linked)"}`}>
            <RefreshCcw className={classes.info_icon} />
          </Tooltip>
          {jiraLinked ? (
            <StatusBadge
              {...{
                ...statusMetaItem,
                icon:
                  worklogInfo.hasError &&
                  worklogInfo.status !== WORKLOG_STATUS.IN_QUEUE &&
                  worklogInfo.status !== WORKLOG_STATUS.SYNC_IN_PROGRESS &&
                  worklogInfo.status !== WORKLOG_STATUS.UNSYNC_IN_PROGRESS ? (
                    <CircleAlert />
                  ) : undefined,
              }}
            />
          ) : (
            <span className={classes.info_label}>-</span>
          )}
        </div>
      </div>
    </div>
  );
};

export default WorklogInfo;
