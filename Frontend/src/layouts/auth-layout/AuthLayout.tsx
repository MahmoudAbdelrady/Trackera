import classes from "./scss/auth-layout.module.css";

const AuthLayout = ({ children }: { children: React.ReactNode }) => {
  return (
    <div className={classes.auth_layout}>
      <div className={classes.header}>
        <h3>Trackera</h3>
        <p>Track Your Jira Worklog Efficiently</p>
      </div>
      <div className={classes.layout_content}>{children}</div>
    </div>
  );
};

export default AuthLayout;
