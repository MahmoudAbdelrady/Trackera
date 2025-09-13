import type React from "react";
import classes from "./scss/worklog-status-card.module.css";
import { CircleCheckBig, Clock, Info } from "lucide-react";
import type { WorkLogSummaryCard } from "../../../shared/types";

const WorklogStatusCard = (props: WorkLogSummaryCard) => {
  const getCardIcon = (code: string): React.ReactNode => {
    switch (code) {
      case "logged":
        return <Clock />;
      case "target":
        return <CircleCheckBig />;
      case "remaining":
        return <Info />;
    }
  };

  return (
    <div className={`${classes.worklog_status_card} ${classes[props.code]}`}>
      <div className={classes.card_info}>
        <span className={classes.card_label}>{props.label}</span>
        <span className={classes.card_value}>{props.value}</span>
        <span className={classes.card_sub_label}>{props.subLabel}</span>
      </div>
      <div className={classes.card_icon}>{getCardIcon(props.code)}</div>
    </div>
  );
};

export default WorklogStatusCard;
