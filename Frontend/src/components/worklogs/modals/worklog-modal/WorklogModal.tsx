import { Modal, type ModalProps } from "antd";
import classes from "./scss/worklog-modal.module.css";

interface WorklogModalProps {
  title: string;
  properties: ModalProps;
  children: React.ReactNode;
}

const WorklogModal = (props: WorklogModalProps) => {
  const { title, properties, children } = props;

  return (
    <Modal {...properties} className={classes.worklog_modal}>
      <h2 className={classes.header}>{title}</h2>
      {children}
    </Modal>
  );
};

export default WorklogModal;
