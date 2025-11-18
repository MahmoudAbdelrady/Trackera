import dayjs from "dayjs";
import { useFormik } from "formik";
import { manageWorkLog } from "../../../../shared/yup-schemas";
import worklogModalClasses from "../worklog-modal/scss/worklog-modal.module.css";
import { Alert, DatePicker, Form, Input, Switch, Tooltip, type TableProps, type UploadFile } from "antd";
import { Inbox, Info } from "lucide-react";
import Dragger from "antd/es/upload/Dragger";
import { CollapsibleSection, WorklogModal, TrackeraTable } from "../../..";
import { useMemo, useState } from "react";
import { showErrorToast, showSuccessToast } from "../../../../utils/toast-handler/showToast";
import requestInstance from "../../../../shared/axios/request-instance";
import type { ManageWorkLogModalProps, WorklogError } from "../../../../shared/types";
import { formatDate, getFormikFieldError, getFormikFieldStatus } from "../../../../utils";

const ManageWorkLogModal = (props: ManageWorkLogModalProps) => {
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [worklogFileErrors, setWorklogFileErrors] = useState<WorklogError[]>([]);

  const isEditMode = (): boolean => {
    return props.selectedWorkLog !== undefined;
  };

  const handleFileUpload = (file: File | null) => {
    manageWorkLogFormik.setFieldValue("logFile", file);
    manageWorkLogFormik.setFieldTouched("logFile", true, false);
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

  const getFormData = (values: typeof manageWorkLogFormik.values, isEdit: boolean): FormData => {
    const formData = new FormData();
    let worklogValues: { logName: string; logDate: string; syncToJira?: boolean } = {
      logName: values.logName,
      logDate: formatDate(values.logDate)!,
    };
    if ((isEdit && values.reEvaluate) || !isEdit) {
      formData.append("file", values.logFile! as Blob);
      worklogValues.syncToJira = values.syncToJira;
    }
    formData.append(
      "worklogInfo",
      new Blob([JSON.stringify(worklogValues)], {
        type: "application/json",
      })
    );
    return formData;
  };

  const manageWorkLogFormik = useFormik({
    initialValues: useMemo(
      () => ({
        mode: isEditMode() ? "edit" : "add",
        logName: isEditMode() ? props.selectedWorkLog!.name : "",
        logDate: isEditMode() ? dayjs(props.selectedWorkLog!.workDate) : dayjs().hour() < 12 ? dayjs().subtract(1, "day") : dayjs(),
        logFile: null,
        reEvaluate: false,
        syncToJira: false,
      }),
      [props.selectedWorkLog]
    ),
    validationSchema: manageWorkLog,
    enableReinitialize: true,
    onSubmit: async (values) => {
      setIsLoading(true);
      try {
        const formData = getFormData(values, isEditMode());
        let response;
        if (isEditMode()) {
          response = await requestInstance.put(`/worklog/${props.selectedWorkLog!.id}`, formData);
        } else {
          response = await requestInstance.post("/worklog", formData);
        }
        showSuccessToast(response.data);
        handleModalClose();
        props.setFetchWorkLog(true);
        props.setFetchSummary(true);
      } catch (error: any) {
        if (error.response?.data.isError) {
          showErrorToast(error.response?.data.message);
          setWorklogFileErrors(error.response?.data.errors);
        } else {
          showErrorToast(error);
          setWorklogFileErrors([]);
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
      title: "Errors",
      dataIndex: "error",
      key: "error",
    },
  ];

  return (
    <WorklogModal
      title={!isEditMode() ? "Add Worklog" : "Edit Worklog"}
      properties={{
        open: true,
        centered: true,
        closable: !isLoading,
        keyboard: !isLoading,
        maskClosable: !isLoading,
        okText: !isEditMode() ? "Add" : "Update",
        okButtonProps: { loading: isLoading, disabled: !manageWorkLogFormik.isValid || !manageWorkLogFormik.dirty || isLoading },
        cancelButtonProps: { disabled: isLoading },
        onOk: () => {
          manageWorkLogFormik.submitForm();
        },
        onCancel: handleModalClose,
        width: worklogFileErrors.length > 0 ? 900 : 520,
      }}
    >
      <form className={worklogModalClasses.worklog_form}>
        <div className={worklogModalClasses.form_group}>
          <Form.Item className={worklogModalClasses.form_item} validateStatus={getFormikFieldStatus(manageWorkLogFormik, "logName")} help={getFormikFieldError(manageWorkLogFormik, "logName")}>
            <span className={worklogModalClasses.label}>Log Name:</span>
            <Input
              placeholder="Enter log name"
              name="logName"
              value={manageWorkLogFormik.values.logName}
              onChange={manageWorkLogFormik.handleChange}
              onBlur={manageWorkLogFormik.handleBlur}
              style={{ width: "80%" }}
              disabled={isLoading}
            />
          </Form.Item>
          {!isEditMode() && (
            <Tooltip title="If not provided, the log name will be auto-generated based on the upload date and weekday.">
              <Info size={22} cursor={"pointer"} />
            </Tooltip>
          )}
        </div>
        <div className={worklogModalClasses.form_group}>
          <Form.Item
            className={worklogModalClasses.form_item}
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
            {isEditMode() &&
              manageWorkLogFormik.values.logDate !== null &&
              formatDate(manageWorkLogFormik.values.logDate) !== props.selectedWorkLog?.workDate &&
              props.selectedWorkLog?.status !== "NOT_SYNCED" && <Alert message="Changing the log date will be applied to the synced worklogs" type="warning" showIcon style={{ marginTop: "10px" }} />}
          </Form.Item>
        </div>
        {isEditMode() && (
          <div className={worklogModalClasses.form_group}>
            <span className={worklogModalClasses.label}>Re-evaluate worklog file:</span>
            <Switch disabled={isLoading} value={manageWorkLogFormik.values.reEvaluate} onChange={(value) => manageWorkLogFormik.setFieldValue("reEvaluate", value)} />
          </div>
        )}
        {(!isEditMode() || manageWorkLogFormik.values.reEvaluate) && (
          <>
            <div className={`${worklogModalClasses.form_group} ${worklogModalClasses.upload_group}`}>
              <Form.Item className={worklogModalClasses.form_item} validateStatus={getFormikFieldStatus(manageWorkLogFormik, "logFile")} help={getFormikFieldError(manageWorkLogFormik, "logFile")}>
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
                    manageWorkLogFormik.setFieldTouched("logFile", true, false);
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
            <div className={worklogModalClasses.form_group}>
              <span className={worklogModalClasses.label}>Sync to Jira after upload:</span>
              <Switch disabled={!props.jiraLinked || isLoading} value={manageWorkLogFormik.values.syncToJira} onChange={(value) => manageWorkLogFormik.setFieldValue("syncToJira", value)} />
              {!props.jiraLinked && (
                <Tooltip title="Link your Jira account in settings to enable this option.">
                  <Info size={16} color="#dc2626" style={{ marginLeft: "8px" }} />
                </Tooltip>
              )}
            </div>
          </>
        )}
      </form>
      {worklogFileErrors.length > 0 && (
        <div className={worklogModalClasses.file_errors_container}>
          <CollapsibleSection title="Uploaded File Errors" icon={<Info color="#dc2626" />}>
            <TrackeraTable<WorklogError>
              properties={{
                columns: worklogFileErrorsColumns,
                dataSource: worklogFileErrors,
                pagination: { pageSize: 5, showSizeChanger: false, style: { marginRight: "16px" } },
              }}
            />
          </CollapsibleSection>
        </div>
      )}
    </WorklogModal>
  );
};

export default ManageWorkLogModal;
