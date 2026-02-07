import { useFormik } from "formik";
import WorklogModal from "../modals/worklog-modal/WorklogModal";
import { editWorklogTask } from "../../../shared/yup-schemas/worklog";
import InputField from "../../input-field/InputField";
import { ClipboardList, Info } from "lucide-react";
import { getFormikFieldProps } from "../../../utils";
import { useState } from "react";
import classes from "./scss/edit-worklog-task.module.css";
import { Alert, Switch, Tooltip } from "antd";
import type { WorklogTask } from "../../../shared/types";

interface EditWorklogTaskProps {
  worklogTask: WorklogTask;
  setIsOpen: (isOpen: boolean) => void;
  jiraLinked: boolean;
}

interface EditWorklogTaskFormValues {
  taskName: string;
  syncToJira: boolean;
}

const EditWorklogTask = (props: EditWorklogTaskProps) => {
  const { worklogTask, setIsOpen, jiraLinked } = props;
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const editWorklogTaskFormik = useFormik<EditWorklogTaskFormValues>({
    initialValues: { taskName: worklogTask.taskName, syncToJira: false },
    validationSchema: editWorklogTask,
    onSubmit: (values) => {
      setIsLoading(true);
      console.log(values);
      // TODO
      setIsLoading(false);
    },
  });

  return (
    <WorklogModal
      title={`Edit Task: ${worklogTask.taskName}`}
      properties={{
        open: true,
        centered: true,
        closable: !isLoading,
        keyboard: !isLoading,
        maskClosable: !isLoading,
        okText: "Update",
        okButtonProps: {
          loading: isLoading,
          disabled: !editWorklogTaskFormik.isValid || !editWorklogTaskFormik.dirty || isLoading,
        },
        cancelButtonProps: { disabled: isLoading },
        onOk: () => {
          editWorklogTaskFormik.submitForm();
        },
        onCancel: () => setIsOpen(false),
      }}
    >
      <form onSubmit={editWorklogTaskFormik.handleSubmit} className={classes.edit_worklog_task_form}>
        <div className={classes.form_group}>
          <InputField
            label="Task Name"
            icon={<ClipboardList />}
            placeholder="Enter task name"
            type="text"
            {...getFormikFieldProps(editWorklogTaskFormik, "taskName", isLoading)}
            value={editWorklogTaskFormik.values.taskName}
          />
          {!editWorklogTaskFormik.values.syncToJira &&
            editWorklogTaskFormik.dirty &&
            worklogTask.status !== "NOT_SYNCED" && (
              <Alert
                title="Changing the task name will be applied to the synced tasks"
                type="warning"
                showIcon
                style={{ marginTop: "10px" }}
              />
            )}
        </div>
        {editWorklogTaskFormik.dirty && worklogTask.status !== "NOT_SYNCED" && (
          <div className={classes.form_group}>
            <span className={classes.label}>Sync to Jira after update:</span>
            <Switch
              disabled={!jiraLinked || isLoading}
              value={editWorklogTaskFormik.values.syncToJira}
              onChange={(value) => editWorklogTaskFormik.setFieldValue("syncToJira", value)}
            />
            {!jiraLinked && (
              <Tooltip title="Link your Jira account in settings to enable this option.">
                <Info size={16} color="#dc2626" style={{ marginLeft: "8px" }} />
              </Tooltip>
            )}
          </div>
        )}
      </form>
    </WorklogModal>
  );
};

export default EditWorklogTask;
