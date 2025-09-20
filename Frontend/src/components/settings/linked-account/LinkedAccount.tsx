import { Button } from "antd";
import type { LinkedAccountProps } from "../../../shared/types";
import classes from "./scss/linked-account.module.css";

const LinkedAccount = (props: LinkedAccountProps) => {
  return (
    <div className={classes.linked_account}>
      <div className={classes.account_info}>
        <div className={classes.platform}>
          <div className={classes.icon}>{props.icon}</div>
          <div className={classes.platform_name}>{props.platform}</div>
        </div>
        <div className={classes.account_identifier}>{props.accountIdentifier}</div>
      </div>
      <div className={classes.actions}>
        <Button
          color={`${props.isLinked ? "danger" : "primary"}`}
          variant={`${props.isLinked ? "outlined" : "solid"}`}
          className={classes.link_button}
          onClick={props.isLinked ? props.onUnlink : props.onLink}
        >
          {`${props.isLinked ? "Unlink" : "Link"} Account`}
        </Button>
      </div>
    </div>
  );
};

export default LinkedAccount;
