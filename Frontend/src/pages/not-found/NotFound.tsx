import classes from "./scss/not-found.module.css";

const NotFound = () => {
  return (
    <div className={classes.not_found_container}>
      <div className={classes.content}>
        <img src="/Assets/not_found.svg" alt="Not Found" className={classes.not_found_img} />
        <h2 className={classes.title}>Not Found</h2>
        <div className={classes.message}>Oops! The page you're looking for doesn't exist.</div>
      </div>
    </div>
  );
};

export default NotFound;
