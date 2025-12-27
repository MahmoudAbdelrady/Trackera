import { Modal, type ModalProps } from "antd";
import classes from "./scss/worklog-modal.module.css";

interface WorklogModalProps {
  title: string;
  properties: ModalProps;
  children: React.ReactNode;
}

const WorklogModal = (props: WorklogModalProps) => {
  return (
    <Modal {...props.properties} className={classes.worklog_modal}>
      <h2 className={classes.header}>{props.title}</h2>
      {props.children}
    </Modal>
  );
};

export default WorklogModal;
