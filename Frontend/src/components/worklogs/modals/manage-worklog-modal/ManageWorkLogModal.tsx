import dayjs from "dayjs";
import { useFormik } from "formik";
import { manageWorkLog } from "../../../../shared/yup-schemas";
import worklogModalClasses from "../worklog-modal/scss/worklog-modal.module.css";
import { DatePicker, Form, Input, Switch, Tooltip, type TableProps, type UploadFile } from "antd";
import { Inbox, Info } from "lucide-react";
import Dragger from "antd/es/upload/Dragger";
import { CollapsibleSection, WorklogModal, WorklogTable } from "../../..";
import { useMemo, useState } from "react";
import { showErrorToast, showSuccessToast } from "../../../../utils/toast-handler/showToast";
import requestInstance from "../../../../shared/axios/request-instance";
import type { ManageWorkLogModalProps, WorklogError } from "../../../../shared/types";
import { getFormikFieldError, getFormikFieldStatus } from "../../../../utils";

const ManageWorkLogModal = (props: ManageWorkLogModalProps) => {
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [worklogFileErrors, setWorklogFileErrors] = useState<WorklogError[]>([]);
  const [reEvaluateFile, setReEvaluateFile] = useState<boolean>(false);

  const isEditMode = (): boolean => {
    return props.selectedWorkLog !== undefined;
  };

  const handleFileUpload = (file: File | null) => {
    manageWorkLogFormik.setFieldValue("logFile", file);
    manageWorkLogFormik.setFieldTouched("logFile", true);
  };

  const handleFileDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      handleFileUpload(files[0]);
    }
  };

  const handleModalClose = () => {
    manageWorkLogFormik.resetForm();
    setFileList([]);
    setWorklogFileErrors([]);
    props.setIsOpen(false);
    props.setSelectedWorkLog?.(undefined);
  };

  const manageWorkLogFormik = useFormik({
    initialValues: useMemo(
      () => ({
        logName: isEditMode() ? props.selectedWorkLog!.name : "",
        logDate: isEditMode() ? dayjs(props.selectedWorkLog!.workDate) : dayjs().hour() < 12 ? dayjs().subtract(1, "day") : dayjs(),
        logFile: null,
        syncToJira: false,
      }),
      [props.selectedWorkLog]
    ),
    validationSchema: manageWorkLog,
    enableReinitialize: true,
    onSubmit: async (values) => {
      setIsLoading(true);
      try {
        const { logFile, ...worklogValues } = values;
        const formData = new FormData();
        formData.append(
          "worklog",
          new Blob([JSON.stringify(worklogValues)], {
            type: "application/json",
          })
        );
        formData.append("file", values.logFile! as Blob);
        let response;
        if (props.selectedWorkLog) {
          // response = await requestInstance.put(`/worklog/update/${props.selectedWorkLog.id}`, formData);
          response = { data: "test" };
          console.log("Values received");
        } else {
          response = await requestInstance.post("/worklog/add", formData);
        }
        showSuccessToast(response.data);
        handleModalClose();
        props.setFetchWorkLog(true);
      } catch (error: any) {
        if (error.response?.data.isError) {
          setWorklogFileErrors(error.response?.data.errors);
        } else {
          showErrorToast(error);
        }
      }
      setIsLoading(false);
    },
  });

  const worklogFileErrorsColumns: TableProps<WorklogError>["columns"] = [
    {
      title: "Row Number",
      dataIndex: "row",
      key: "row",
    },
    {
      title: "Error",
      dataIndex: "error",
      key: "error",
    },
  ];

  return (
    <WorklogModal
      title={!isEditMode() ? "Add Worklog" : "Edit Worklog"}
      properties={{
        open: props.isOpen,
        centered: true,
        closable: !isLoading,
        keyboard: !isLoading,
        maskClosable: !isLoading,
        okText: !isEditMode() ? "Add" : "Update",
        okButtonProps: {
          loading: isLoading,
          disabled: !manageWorkLogFormik.isValid || !manageWorkLogFormik.dirty || isLoading,
        },
        cancelButtonProps: {
          disabled: isLoading,
        },
        onOk: () => {
          manageWorkLogFormik.submitForm();
        },
        onCancel: handleModalClose,
        width: worklogFileErrors.length > 0 ? 900 : 520,
      }}
    >
      <form className={worklogModalClasses.worklog_form}>
        <div className={worklogModalClasses.form_group}>
          <Form.Item
            style={{ marginBottom: 0, width: "100%" }}
            validateStatus={getFormikFieldStatus(manageWorkLogFormik, "logName")}
            help={getFormikFieldError(manageWorkLogFormik, "logName")}
          >
            <span className={worklogModalClasses.label}>Log Name:</span>
            <Input
              placeholder="Enter log name"
              name="logName"
              value={manageWorkLogFormik.values.logName}
              onChange={manageWorkLogFormik.handleChange}
              onBlur={manageWorkLogFormik.handleBlur}
              style={{ width: "70%", marginRight: "10px" }}
              disabled={isLoading}
            />
          </Form.Item>
          <Tooltip title="If not provided, the log name will be auto-generated based on the upload date and weekday.">
            <Info size={22} cursor={"pointer"} />
          </Tooltip>
        </div>
        <div className={worklogModalClasses.form_group}>
          <Form.Item
            style={{ marginBottom: 0, width: "100%" }}
            validateStatus={getFormikFieldStatus(manageWorkLogFormik, "logDate")}
            help={getFormikFieldError(manageWorkLogFormik, "logDate") as string}
          >
            <span className={worklogModalClasses.label}>Log date:</span>
            <DatePicker
              name="logDate"
              value={manageWorkLogFormik.values.logDate}
              placeholder="Select log date"
              onChange={(date) => manageWorkLogFormik.setFieldValue("logDate", date)}
              onBlur={() => manageWorkLogFormik.setFieldTouched("logDate", true)}
              disabled={isLoading}
            />
          </Form.Item>
        </div>
        {isEditMode() && (
          <div className={worklogModalClasses.form_group}>
            <span className={worklogModalClasses.label}>Re-evaluate worklog file:</span>
            <Switch disabled={isLoading} value={reEvaluateFile} onChange={(value) => setReEvaluateFile(value)} />
          </div>
        )}
        {!isEditMode() ||
          (reEvaluateFile && (
            <div className={`${worklogModalClasses.form_group} ${worklogModalClasses.upload_group}`}>
              <Form.Item
                style={{ marginBottom: 0, width: "100%" }}
                validateStatus={getFormikFieldStatus(manageWorkLogFormik, "logFile")}
                help={getFormikFieldError(manageWorkLogFormik, "logFile")}
              >
                <span className={worklogModalClasses.label}>Upload log file:</span>
                <Dragger
                  className={worklogModalClasses.upload_box}
                  showUploadList={true}
                  multiple={false}
                  maxCount={1}
                  accept=".xlsx,.csv"
                  fileList={fileList}
                  beforeUpload={(file) => {
                    handleFileUpload(file);
                    setFileList([file]);
                    return false; // prevent auto upload
                  }}
                  onDrop={handleFileDrop}
                  onRemove={() => {
                    handleFileUpload(null);
                    setFileList([]);
                    manageWorkLogFormik.setFieldTouched("logFile", true);
                    return true; // allow removal
                  }}
                  disabled={isLoading}
                >
                  <div className={worklogModalClasses.upload_icon_box}>
                    <Inbox className={worklogModalClasses.upload_icon} />
                  </div>
                  <span className={worklogModalClasses.upload_title}>Click or drag worklog file to this area to upload</span>
                  <p className={worklogModalClasses.upload_subtitle}>Supported formats: .xlsx and .csv. Max file size: 5MB.</p>
                </Dragger>
              </Form.Item>
            </div>
          ))}
        <div className={worklogModalClasses.form_group}>
          <span className={worklogModalClasses.label}>Sync to Jira after upload:</span>
          <Switch
            disabled={isLoading}
            value={manageWorkLogFormik.values.syncToJira}
            onChange={(value) => manageWorkLogFormik.setFieldValue("syncToJira", value)}
          />
        </div>
      </form>
      {worklogFileErrors.length > 0 && (
        <div className={worklogModalClasses.file_errors_container}>
          <CollapsibleSection title="Uploaded File Errors" icon={<Info color="#dc2626" />}>
            <WorklogTable
              properties={{
                columns: worklogFileErrorsColumns,
                dataSource: worklogFileErrors,
                pagination: { pageSize: 5 },
              }}
              actionButtons={[]}
            />
          </CollapsibleSection>
        </div>
      )}
    </WorklogModal>
  );
};

export default ManageWorkLogModal;
