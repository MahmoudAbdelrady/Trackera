import classes from "./scss/access-denied.module.css";

const AccessDenied = () => {
  return (
    <div className={classes.access_denied_container}>
      <div className={classes.content}>
        <img src="/Assets/access_denied.svg" alt="Access Denied" className={classes.access_denied_img} />
        <h2 className={classes.title}>Access Denied</h2>
        <div className={classes.message}>Jira is not linked to your account. Please link Jira to access this page.</div>
      </div>
    </div>
  );
};

export default AccessDenied;
