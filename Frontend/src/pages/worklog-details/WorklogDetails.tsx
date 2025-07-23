import { ArrowLeft, Calendar, Clock, RefreshCcw, Target } from "lucide-react";
import { AppLayout } from "../../components";
import classes from "./scss/worklog-details.module.css";
import { Link } from "react-router-dom";

const WorklogDetails = () => {
  return (
    <AppLayout>
      <div className={classes.back_button_container}>
        <Link className={classes.back_button} to="/">
          <ArrowLeft />
          <span className={classes.back_button_text}>Back to Worklogs</span>
        </Link>
      </div>
      <div className={classes.worklog_details_info}>
        <h3 className={classes.title}>Test Worklog</h3>
        <div className={classes.info}>
          <div className={classes.info_box}>
            <Clock className={classes.info_icon} />
            <span className={classes.info_label}>8.5 hours</span>
          </div>
          <div className={classes.info_box}>
            <Calendar className={classes.info_icon} />
            <span className={classes.info_label}>2023-10-01</span>
          </div>
          <div className={classes.info_box}>
            <Target className={classes.info_icon} />
            <span
              className={`${classes.info_label} ${classes.evaluation} ${classes.excellent}`}
            >
              Excellent
            </span>
          </div>
          <div className={classes.info_box}>
            <RefreshCcw className={classes.info_icon} />
            <span
              className={`${classes.info_label} ${classes.status} ${classes.synced}`}
            >
              Synced
            </span>
          </div>
        </div>
      </div>
    </AppLayout>
  );
};

export default WorklogDetails;
