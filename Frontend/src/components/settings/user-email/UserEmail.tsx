import StatusBadge from "../../status-badge/StatusBadge";
import classes from "./scss/user-email.module.css";

const UserEmail = () => {
  return (
    <div className={classes.user_email}>
      <div className={classes.info}>
        <span className={classes.email}>tester@mail.com</span>
        <div className={classes.tags}>
          <StatusBadge badgeProps={{ label: "Primary", type: "main" }} />
        </div>
      </div>
      <div className={classes.action_btn}></div>
    </div>
  );
};

export default UserEmail;
