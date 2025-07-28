import { AuthFooter } from "../../";
import type { AuthLayoutProps } from "../../../utils/types";
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
          <h3>Sign in to Trackera</h3>
          <p>Enter your credentials to access your account</p>
        </div>
        <form className={classes.auth_form}>{props.children}</form>
        <div className={classes.auth_footer}>
          <AuthFooter />
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;
