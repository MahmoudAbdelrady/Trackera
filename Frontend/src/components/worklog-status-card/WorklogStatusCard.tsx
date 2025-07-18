import type React from "react";
import classes from "./scss/worklog-status-card.module.css";

export interface WorklogStatusCardProps {
  cardLabel: string;
  cardSubLabel?: string;
  cardValue: string;
  cardIcon: React.ReactNode;
  cardColorTheme: string;
}

const WorklogStatusCard = (props: WorklogStatusCardProps) => {
  return (
    <div
      className={`${classes.worklog_status_card} ${
        classes[props.cardColorTheme]
      }`}
    >
      <div className={classes.card_info}>
        <span className={classes.card_label}>{props.cardLabel}</span>
        <span className={classes.card_value}>{props.cardValue}</span>
        {props.cardSubLabel && (
          <span className={classes.card_sub_label}>{props.cardSubLabel}</span>
        )}
      </div>
      <div className={classes.card_icon}>{props.cardIcon}</div>
    </div>
  );
};

export default WorklogStatusCard;
