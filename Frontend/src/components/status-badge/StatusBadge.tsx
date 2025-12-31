import type { StatusBadgeProps } from "./status-badge.types";
import classes from "./scss/status-badge.module.css";

const StatusBadge = (props: StatusBadgeProps) => {
  const { type, icon, label } = props;

  return (
    <div className={`${classes.padded_item} ${classes[type]}`}>
      {icon && <div className={classes.badge_icon}>{icon}</div>}
      <div className={classes.badge_label}>{label}</div>
    </div>
  );
};

export default StatusBadge;
