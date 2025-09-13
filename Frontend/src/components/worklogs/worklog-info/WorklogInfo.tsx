import classes from "./scss/worklog-info.module.css";
import { Calendar, Clock, RefreshCcw, Target } from "lucide-react";
import { Tooltip } from "antd";
import { evaluationMetadata, statusMetadata, type Worklog, type WorkLogEvaluationType, type WorkLogStatusType } from "../../../shared/types";

const WorklogInfo = ({ worklogInfo }: { worklogInfo: Worklog }) => {
  return (
    <div className={classes.worklog_info}>
      <h3 className={classes.title}>{worklogInfo?.name}</h3>
      <div className={classes.info}>
        <div className={classes.info_box}>
          <Tooltip title="Total Hours">
            <Clock className={classes.info_icon} />
          </Tooltip>
          <span className={classes.info_label}>{worklogInfo?.totalHours}</span>
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
          <span className={`${classes.info_label} ${classes.evaluation} ${classes[evaluationMetadata[worklogInfo?.evaluation as WorkLogEvaluationType].className]}`}>{worklogInfo?.evaluation}</span>
        </div>
        <div className={classes.info_box}>
          <Tooltip title="Status">
            <RefreshCcw className={classes.info_icon} />
          </Tooltip>
          <span className={`${classes.info_label} ${classes.status} ${classes[statusMetadata[worklogInfo?.status as WorkLogStatusType].className]}`}>{worklogInfo?.status}</span>
        </div>
      </div>
    </div>
  );
};

export default WorklogInfo;
