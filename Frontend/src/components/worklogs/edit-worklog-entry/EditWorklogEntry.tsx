import type { Dayjs } from "dayjs";
import type { WorklogEntry, UpdateWorklogDetailResponse } from "../../../shared/types";
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
import { worklogApis } from "../../../state/api";
import { showErrorToast, showSuccessToast } from "../../../utils/toast-handler/showToast";

interface EditWorklogEntryProps {
  worklogId: string;
  taskName: string;
  worklogEntry: WorklogEntry;
  taskEntriesSize: number;
  setIsOpen: (isOpen: boolean) => void;
  jiraLinked: boolean;
  onBeforeSync: () => void;
  onUpdateSuccess: (data: UpdateWorklogDetailResponse, oldTaskName: string, taskNameChanged?: boolean) => void;
  onCloseEntries: () => void;
}

interface EditWorklogEntryFormValues {
  taskName: string;
  startTime: Dayjs | null;
  endTime: Dayjs | null;
  duration: string;
  description: string;
  syncToJira: boolean;
}

const EditWorklogEntry = (props: EditWorklogEntryProps) => {
  const {
    worklogId,
    taskName,
    worklogEntry,
    taskEntriesSize,
    setIsOpen,
    jiraLinked,
    onBeforeSync,
    onUpdateSuccess,
    onCloseEntries,
  } = props;
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const editWorklogEntryFormik = useFormik<EditWorklogEntryFormValues>({
    initialValues: {
      taskName: taskName,
      startTime: dayjs(worklogEntry.fromTime, "h:mm A"),
      endTime: dayjs(worklogEntry.toTime, "h:mm A"),
      duration: worklogEntry.duration,
      description: worklogEntry.description,
      syncToJira: false,
    },
    validationSchema: editWorklogEntry,
    onSubmit: async (values) => {
      setIsLoading(true);
      if (values.syncToJira) {
        onBeforeSync();
      }
      try {
        const result = await worklogApis.updateWorklogDetail(worklogId, {
          taskName: taskName,
          entryId: worklogEntry.id,
          isTask: false,
          syncToJira: values.syncToJira,
          newData: {
            name: values.taskName,
            startTime: values.startTime?.format("h:mm A"),
            endTime: values.endTime?.format("h:mm A"),
            duration: values.duration,
            description: values.description,
          },
        });
        showSuccessToast(result.message);
        onUpdateSuccess(result, taskName, taskName !== values.taskName);
        if (taskName !== values.taskName && taskEntriesSize === 1) {
          onCloseEntries();
        }
        setIsOpen(false);
      } catch (error: unknown) {
        showErrorToast(error);
      }
      setIsLoading(false);
    },
  });

  const updateDuration = (startTime: Dayjs | null, endTime: Dayjs | null) => {
    if (!startTime || !endTime) {
      editWorklogEntryFormik.setFieldValue("duration", "");
      return;
    }

    let diffMinutes = endTime.diff(startTime, "minute");

    // Handle case where endTime is before startTime (crosses midnight)
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
            validateStatus={getFormikFieldStatus(editWorklogEntryFormik, "startTime")}
            help={getFormikFieldError(editWorklogEntryFormik, "startTime") as string}
          >
            <span className={classes.label}>Start time:</span>
            <TimePicker
              format="h:mm A"
              name="startTime"
              value={editWorklogEntryFormik.values.startTime}
              placeholder="Select start time"
              onChange={(date) => {
                editWorklogEntryFormik.setFieldValue("startTime", date);
                updateDuration(date, editWorklogEntryFormik.values.endTime);
              }}
              onBlur={() => editWorklogEntryFormik.setFieldTouched("startTime", true)}
              disabled={isLoading}
            />
          </Form.Item>
        </div>
        <div className={classes.form_group}>
          <Form.Item
            className={classes.form_item}
            validateStatus={getFormikFieldStatus(editWorklogEntryFormik, "endTime")}
            help={getFormikFieldError(editWorklogEntryFormik, "endTime") as string}
          >
            <span className={classes.label}>End time:</span>
            <TimePicker
              format="h:mm A"
              name="endTime"
              value={editWorklogEntryFormik.values.endTime}
              placeholder="Select end time"
              onChange={(date) => {
                editWorklogEntryFormik.setFieldValue("endTime", date);
                updateDuration(editWorklogEntryFormik.values.startTime, date);
              }}
              onBlur={() => editWorklogEntryFormik.setFieldTouched("endTime", true)}
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
              title="The current synced data of this entry will be unsynced upon update"
              type="warning"
              showIcon
              style={{ marginTop: "10px" }}
            />
          )}
        {editWorklogEntryFormik.dirty && (
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
