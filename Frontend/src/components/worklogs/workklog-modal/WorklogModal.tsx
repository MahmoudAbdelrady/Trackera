import { Modal } from "antd";
import classes from "./scss/worklog-modal.module.css";
import type { WorklogModalProps } from "../../../utils/types";

const WorklogModal = (props: WorklogModalProps) => {
  return (
    <Modal {...props.properties} className={classes.worklog_modal}>
      <h2 className={classes.header}>{props.title}</h2>
      {props.children}
    </Modal>
  );
};

export default WorklogModal;
