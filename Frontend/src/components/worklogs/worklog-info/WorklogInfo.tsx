import classes from "./scss/worklog-info.module.css";
import { Calendar, CircleAlert, Clock, RefreshCcw, Target } from "lucide-react";
import { Tooltip } from "antd";
import {
  worklogEvaluationMetadata,
  statusMetadata,
  type Worklog,
  type WorkLogEvaluationType,
  type WorkLogStatusType,
} from "../../../shared/types";
import { StatusBadge } from "../../";

const WorklogInfo = ({ worklogInfo, jiraLinked }: { worklogInfo: Worklog; jiraLinked: boolean }) => {
  const evaluationMetaItem = worklogEvaluationMetadata[worklogInfo?.evaluation as WorkLogEvaluationType];
  const statusMetaItem = statusMetadata[worklogInfo?.status as WorkLogStatusType];

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
          <StatusBadge badgeProps={evaluationMetaItem} />
        </div>
        <div className={classes.info_box}>
          <Tooltip title={`Status${jiraLinked ? "" : " (Jira not linked)"}`}>
            <RefreshCcw className={classes.info_icon} />
          </Tooltip>
          {jiraLinked ? (
            <StatusBadge badgeProps={{ ...statusMetaItem, icon: worklogInfo.hasError ? <CircleAlert /> : undefined }} />
          ) : (
            <span className={classes.info_label}>-</span>
          )}
        </div>
      </div>
    </div>
  );
};

export default WorklogInfo;
