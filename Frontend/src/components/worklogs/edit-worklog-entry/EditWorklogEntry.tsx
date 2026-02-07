import type { Dayjs } from "dayjs";
import type { WorklogEntry } from "../../../shared/types";
import { useState } from "react";
import WorklogModal from "../modals/worklog-modal/WorklogModal";
import { useFormik } from "formik";
import dayjs from "dayjs";
import { editWorklogEntry } from "../../../shared/yup-schemas/worklog";
import InputField from "../../input-field/InputField";
import { AlignLeft, ClipboardList, Info, Timer } from "lucide-react";
import { getFormikFieldError, getFormikFieldProps, getFormikFieldStatus } from "../../../utils";
import Alert from "antd/es/alert/Alert";
import Tooltip from "antd/es/tooltip";
import { Form, Switch, TimePicker } from "antd";
import classes from "./scss/edit-worklog-entry.module.css";

interface EditWorklogEntryProps {
  taskName: string;
  worklogEntry: WorklogEntry;
  setIsOpen: (isOpen: boolean) => void;
  jiraLinked: boolean;
}

interface EditWorklogEntryFormValues {
  taskName: string;
  fromTime: Dayjs | null;
  toTime: Dayjs | null;
  duration: string;
  description: string;
  syncToJira: boolean;
}

const EditWorklogEntry = (props: EditWorklogEntryProps) => {
  const { taskName, worklogEntry, setIsOpen, jiraLinked } = props;
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const editWorklogEntryFormik = useFormik<EditWorklogEntryFormValues>({
    initialValues: {
      taskName: taskName,
      fromTime: dayjs(worklogEntry.fromTime, "h:mm A"),
      toTime: dayjs(worklogEntry.toTime, "h:mm A"),
      duration: worklogEntry.duration,
      description: worklogEntry.description,
      syncToJira: false,
    },
    validationSchema: editWorklogEntry,
    onSubmit: (values) => {
      setIsLoading(true);
      console.log(values);
      // TODO
      setIsLoading(false);
    },
  });

  const updateDuration = (fromTime: Dayjs | null, toTime: Dayjs | null) => {
    if (!fromTime || !toTime) {
      editWorklogEntryFormik.setFieldValue("duration", "");
      return;
    }

    let diffMinutes = toTime.diff(fromTime, "minute");

    // Handle case where toTime is before fromTime (crosses midnight)
    if (diffMinutes < 0) {
      diffMinutes += 24 * 60; // Add 24 hours worth of minutes
    }

    const hours = Math.floor(diffMinutes / 60);
    const minutes = diffMinutes % 60;

    const duration = `${hours > 0 ? `${hours}h` : ""} ${minutes > 0 ? `${minutes}m` : ""}`;
    editWorklogEntryFormik.setFieldValue("duration", duration);
  };

  return (
    <WorklogModal
      title={`Edit Entry: ${taskName}`}
      properties={{
        open: true,
        centered: true,
        closable: !isLoading,
        keyboard: !isLoading,
        maskClosable: !isLoading,
        okText: "Update",
        okButtonProps: {
          loading: isLoading,
          disabled: !editWorklogEntryFormik.isValid || !editWorklogEntryFormik.dirty || isLoading,
        },
        cancelButtonProps: { disabled: isLoading },
        onOk: () => {
          editWorklogEntryFormik.submitForm();
        },
        onCancel: () => setIsOpen(false),
      }}
    >
      <form onSubmit={editWorklogEntryFormik.handleSubmit} className={classes.edit_worklog_entry_form}>
        <div className={classes.form_group}>
          <InputField
            label="Task Name"
            icon={<ClipboardList />}
            placeholder="Enter task name"
            type="text"
            {...getFormikFieldProps(editWorklogEntryFormik, "taskName", isLoading)}
            value={editWorklogEntryFormik.values.taskName}
          />
        </div>
        <div className={classes.form_group}>
          <Form.Item
            className={classes.form_item}
            validateStatus={getFormikFieldStatus(editWorklogEntryFormik, "fromTime")}
            help={getFormikFieldError(editWorklogEntryFormik, "fromTime") as string}
          >
            <span className={classes.label}>From time:</span>
            <TimePicker
              format="h:mm A"
              name="fromTime"
              value={editWorklogEntryFormik.values.fromTime}
              placeholder="Select from time"
              onChange={(date) => {
                editWorklogEntryFormik.setFieldValue("fromTime", date);
                updateDuration(date, editWorklogEntryFormik.values.toTime);
              }}
              onBlur={() => editWorklogEntryFormik.setFieldTouched("fromTime", true)}
              disabled={isLoading}
            />
          </Form.Item>
        </div>
        <div className={classes.form_group}>
          <Form.Item
            className={classes.form_item}
            validateStatus={getFormikFieldStatus(editWorklogEntryFormik, "toTime")}
            help={getFormikFieldError(editWorklogEntryFormik, "toTime") as string}
          >
            <span className={classes.label}>To time:</span>
            <TimePicker
              format="h:mm A"
              name="toTime"
              value={editWorklogEntryFormik.values.toTime}
              placeholder="Select to time"
              onChange={(date) => {
                editWorklogEntryFormik.setFieldValue("toTime", date);
                updateDuration(editWorklogEntryFormik.values.fromTime, date);
              }}
              onBlur={() => editWorklogEntryFormik.setFieldTouched("toTime", true)}
              disabled={isLoading}
            />
          </Form.Item>
        </div>
        <div className={classes.form_group}>
          <InputField
            label="Duration"
            icon={<Timer />}
            placeholder="Duration"
            type="text"
            {...getFormikFieldProps(editWorklogEntryFormik, "duration", isLoading)}
            disabled={true}
            value={editWorklogEntryFormik.values.duration}
          />
        </div>
        <div className={classes.form_group}>
          <InputField
            label="Description"
            icon={<AlignLeft />}
            placeholder="Enter description"
            type="textarea"
            {...getFormikFieldProps(editWorklogEntryFormik, "description", isLoading)}
            value={editWorklogEntryFormik.values.description}
          />
        </div>
        {!editWorklogEntryFormik.values.syncToJira &&
          editWorklogEntryFormik.dirty &&
          worklogEntry.status !== "NOT_SYNCED" && (
            <Alert
              title="The changed data of this entry will be synced to Jira"
              type="warning"
              showIcon
              style={{ marginTop: "10px" }}
            />
          )}
        {editWorklogEntryFormik.dirty && worklogEntry.status !== "NOT_SYNCED" && (
          <div className={classes.form_group}>
            <span className={classes.label}>Sync to Jira after update:</span>
            <Switch
              disabled={!jiraLinked || isLoading}
              value={editWorklogEntryFormik.values.syncToJira}
              onChange={(value) => editWorklogEntryFormik.setFieldValue("syncToJira", value)}
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

export default EditWorklogEntry;
