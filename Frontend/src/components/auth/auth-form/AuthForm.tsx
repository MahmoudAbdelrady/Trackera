import { Button } from "antd";
import type { AuthFormProps } from "../../../shared/types";
import classes from "./scss/auth-form.module.css";

const AuthForm = (props: AuthFormProps) => {
  return (
    <div className={classes.auth_form_container}>
      <div className={classes.af_header}>
        <h3>{props.title}</h3>
        <p>{props.description}</p>
      </div>
      <form className={classes.af_content} onSubmit={props.onSubmit}>
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
  );
};

export default AuthForm;
