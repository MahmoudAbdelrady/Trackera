import { Button } from "antd";
import classes from "./scss/auth-form.module.css";

interface AuthFormProps {
  title: string;
  description: string;
  children: React.ReactNode;
  submitButtonText: string;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  isSubmitBtnLoading?: boolean;
  isSubmitBtnDisabled?: boolean;
  footer?: React.ReactNode;
}

const AuthForm = (props: AuthFormProps) => {
  const { title, description, children, submitButtonText, onSubmit, isSubmitBtnLoading, isSubmitBtnDisabled, footer } =
    props;

  return (
    <div className={classes.auth_form_container}>
      <div className={classes.af_header}>
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
      <form className={classes.af_content} onSubmit={onSubmit}>
        <div className={classes.input_groups}>{children}</div>
        <div className={classes.submit_button_container}>
          <Button
            htmlType="submit"
            type="primary"
            disabled={isSubmitBtnDisabled || isSubmitBtnLoading}
            loading={isSubmitBtnLoading}
            className={classes.submit_button}
          >
            {submitButtonText}
          </Button>
        </div>
      </form>
      {footer && <div className={classes.auth_footer}>{footer}</div>}
    </div>
  );
};

export default AuthForm;
