import type React from "react";
import classes from "./scss/worklog-status-card.module.css";
import { CircleCheckBig, Clock, Info } from "lucide-react";
import type { WorklogSummaryCard } from "../../../shared/types";

const WorklogStatusCard = (props: WorklogSummaryCard) => {
  const { label, subLabel, code, value } = props;

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
    <div className={`${classes.worklog_status_card} ${classes[code]}`}>
      <div className={classes.card_info}>
        <span className={classes.card_label}>{label}</span>
        <span className={classes.card_value}>{value}</span>
        <span className={classes.card_sub_label}>{subLabel}</span>
      </div>
      <div className={classes.card_icon}>{getCardIcon(code)}</div>
    </div>
  );
};

export default WorklogStatusCard;
