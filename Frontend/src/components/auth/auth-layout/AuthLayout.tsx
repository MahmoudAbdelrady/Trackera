import { Button } from "antd";
import type { AuthLayoutProps } from "../../../shared/types";
import classes from "./scss/auth-layout.module.css";

const AuthLayout = (props: AuthLayoutProps) => {
  return (
    <div className={classes.auth_layout}>
      <div className={classes.header}>
        <h3>Trackera</h3>
        <p>Track Your Jira Worklog Efficiently</p>
      </div>
      <div className={classes.auth_fields_container}>
        <div className={classes.auth_title}>
          <h3>{props.title}</h3>
          <p>{props.description}</p>
        </div>
        <form className={classes.auth_form} onSubmit={props.onSubmit}>
          <div className={classes.input_groups}>{props.children}</div>
          <div className={classes.submit_button_container}>
            <Button
              htmlType="submit"
              type="primary"
              disabled={props.isSubmitBtnDisabled || props.isSubmitBtnLoading}
              loading={props.isSubmitBtnLoading}
              className={classes.submit_button}
            >
              {props.submitButtonText}
            </Button>
          </div>
        </form>
        {props.footer && (
          <div className={classes.auth_footer}>{props.footer}</div>
        )}
      </div>
    </div>
  );
};

export default AuthLayout;
