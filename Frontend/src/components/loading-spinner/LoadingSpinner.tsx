import { Spin } from "antd";
import classes from "./scss/loading-spinner.module.css";

const LoadingSpinner = () => {
  return (
    <div className={classes.loading_spinner_container}>
      <Spin fullscreen />
    </div>
  );
};

export default LoadingSpinner;
