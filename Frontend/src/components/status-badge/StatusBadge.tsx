import type { StatusBadgeProps } from "../../shared/types";
import classes from "./scss/status-badge.module.css";

const StatusBadge = ({ badgeProps }: { badgeProps: StatusBadgeProps }) => {
  return <div className={`${classes.padded_item} ${classes[badgeProps.type]}`}>{badgeProps.label}</div>;
};

export default StatusBadge;
