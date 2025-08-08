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
          <h3>
            {props.showResponseContent
              ? props.responseContent?.title
              : props.title}
          </h3>
          <p>
            {props.showResponseContent
              ? props.responseContent?.description
              : props.description}
          </p>
        </div>
        {props.showResponseContent ? (
          <div className={classes.response_content}>
            <div className={classes.response_icon_container}>
              {props.responseContent?.icon}
            </div>
            <p className={classes.response_message}>
              {props.responseContent?.message}
            </p>
            <Button
              type="primary"
              onClick={props.responseContent?.onClick}
              className={classes.response_button}
            >
              {props.responseContent?.buttonText}
            </Button>
          </div>
        ) : (
          <>
            <form className={classes.auth_form} onSubmit={props.onSubmit}>
              <div className={classes.input_groups}>{props.children}</div>
              <div className={classes.submit_button_container}>
                <Button
                  htmlType="submit"
                  type="primary"
                  disabled={
                    props.isSubmitBtnDisabled || props.isSubmitBtnLoading
                  }
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
          </>
        )}
      </div>
    </div>
  );
};

export default AuthLayout;
