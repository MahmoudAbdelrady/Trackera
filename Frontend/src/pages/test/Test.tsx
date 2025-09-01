import dayjs from "dayjs";
import { WorklogModal } from "../../components";
import { useFormik } from "formik";
import { manageWorkLog } from "../../shared/yup-schemas";
import worklogModalClasses from "../../components/worklogs/modals/worklog-modal/scss/worklog-modal.module.css";
import { DatePicker, Form, Input, Switch, Tooltip } from "antd";
import { Inbox, Info } from "lucide-react";
import Dragger from "antd/es/upload/Dragger";

const Test = () => {
  const addWorkLogFormik = useFormik({
    initialValues: {
      logName: "",
      logDate: dayjs().hour() < 12 ? dayjs().subtract(1, "day") : dayjs(),
      logFile: null,
      syncToJira: false,
    },
    validationSchema: manageWorkLog,
    onSubmit: async (values) => {
      console.log("Form is valid");
      console.log("Form values:", values);
    },
  });

  const handleFileUpload = (file: File | null) => {
    addWorkLogFormik.setFieldValue("logFile", file);
  };

  const handleFileDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      handleFileUpload(files[0]);
    }
  };

  return (
    <WorklogModal
      title="Add Worklog"
      properties={{
        open: true,
        centered: true,
        okText: "Add",
        onOk: () => {
          addWorkLogFormik.submitForm();
        },
        onCancel: () => {
          console.log("Modal closed");
        },
      }}
    >
      <form className={worklogModalClasses.worklog_form}>
        <div className={worklogModalClasses.form_group}>
          <Form.Item
            style={{ marginBottom: 0, width: "100%" }}
            validateStatus={
              addWorkLogFormik.touched.logName &&
              addWorkLogFormik.errors.logName
                ? "error"
                : ""
            }
            help={
              addWorkLogFormik.touched.logName &&
              addWorkLogFormik.errors.logName
                ? addWorkLogFormik.errors.logName
                : ""
            }
          >
            <span className={worklogModalClasses.label}>Log Name:</span>
            <Input
              placeholder="Enter log name"
              style={{ width: "70%", marginRight: "10px" }}
            />
          </Form.Item>
          <Tooltip title="If not provided, the log name will be auto-generated based on the upload date and weekday.">
            <Info size={22} cursor={"pointer"} />
          </Tooltip>
        </div>
        <div className={worklogModalClasses.form_group}>
          <Form.Item
            style={{ marginBottom: 0, width: "100%" }}
            validateStatus={
              addWorkLogFormik.touched.logDate &&
              addWorkLogFormik.errors.logDate
                ? "error"
                : ""
            }
            help={
              addWorkLogFormik.touched.logDate &&
              addWorkLogFormik.errors.logDate
                ? (addWorkLogFormik.errors.logDate as string)
                : ""
            }
          >
            <span className={worklogModalClasses.label}>Log date:</span>
            <DatePicker
              name="logDate"
              value={addWorkLogFormik.values.logDate}
              placeholder="Select log date"
              onChange={(date) =>
                addWorkLogFormik.setFieldValue("logDate", date)
              }
              onBlur={() => addWorkLogFormik.setFieldTouched("logDate", true)}
            />
          </Form.Item>
        </div>
        <div
          className={`${worklogModalClasses.form_group} ${worklogModalClasses.upload_group}`}
        >
          <Form.Item
            style={{ marginBottom: 0, width: "100%" }}
            validateStatus={
              addWorkLogFormik.touched.logFile &&
              addWorkLogFormik.errors.logFile
                ? "error"
                : ""
            }
            help={
              addWorkLogFormik.touched.logFile &&
              addWorkLogFormik.errors.logFile
                ? addWorkLogFormik.errors.logFile
                : ""
            }
          >
            <span className={worklogModalClasses.label}>Upload log file:</span>
            <Dragger
              className={worklogModalClasses.upload_box}
              showUploadList={true}
              multiple={false}
              maxCount={1}
              accept=".xlsx,.csv"
              beforeUpload={(file) => {
                handleFileUpload(file);
                return false; // prevent auto upload
              }}
              onDrop={handleFileDrop}
              onRemove={() => {
                handleFileUpload(null);
                return true; // allow removal
              }}
            >
              <div className={worklogModalClasses.upload_icon_box}>
                <Inbox className={worklogModalClasses.upload_icon} />
              </div>
              <span className={worklogModalClasses.upload_title}>
                Click or drag worklog file to this area to upload
              </span>
              <p className={worklogModalClasses.upload_subtitle}>
                Supported formats: .xlsx and .csv. Max file size: 5MB.
              </p>
            </Dragger>
          </Form.Item>
        </div>
        <div className={worklogModalClasses.form_group}>
          <span className={worklogModalClasses.label}>
            Sync to Jira after upload:
          </span>
          <Switch />
        </div>
      </form>
    </WorklogModal>
  );
};

export default Test;
