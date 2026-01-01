import { Alert } from "antd";
import classes from "./scss/delete-warning.module.css";

interface DeleteWarningProps {
  message: string;
  showAlert?: boolean;
  alertMessage?: string;
}

const DeleteWarning = (props: DeleteWarningProps) => {
  const { message, showAlert, alertMessage } = props;
  return (
    <>
      <p className={classes.delete_message}>{message}</p>
      {showAlert && <Alert title={alertMessage} type="warning" showIcon className={classes.alert_message} />}
    </>
  );
};

export default DeleteWarning;
