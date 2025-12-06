import type { StatusBadgeProps } from "../../shared/types";
import classes from "./scss/status-badge.module.css";

const StatusBadge = ({ badgeProps }: { badgeProps: StatusBadgeProps }) => {
  return (
    <div className={`${classes.padded_item} ${classes[badgeProps.type]}`}>
      {badgeProps.icon && <div className={classes.badge_icon}>{badgeProps.icon}</div>}
      <div className={classes.badge_label}>{badgeProps.label}</div>
    </div>
  );
};

export default StatusBadge;
