import classes from "./scss/server-error.module.css";

const ServerError = () => {
  return (
    <div className={classes.server_error_container}>
      <div className={classes.content}>
        <img src="./Assets/error_icon.svg" alt="Server Error" className={classes.error_img} />
        <h2 className={classes.title}>Service Unavailable</h2>
        <div className={classes.message}>Oops! Something went wrong on our end. Please try again later.</div>
      </div>
    </div>
  );
};

export default ServerError;
