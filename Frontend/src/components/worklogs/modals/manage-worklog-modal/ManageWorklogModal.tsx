import dayjs from "dayjs";
import { useFormik } from "formik";
import { manageWorklog } from "../../../../shared/yup-schemas";
import worklogModalClasses from "../worklog-modal/scss/worklog-modal.module.css";
import { Alert, DatePicker, Form, Input, Switch, Tooltip, type TableProps, type UploadFile } from "antd";
import { Inbox, Info } from "lucide-react";
import Dragger from "antd/es/upload/Dragger";
import { CollapsibleSection, WorklogModal, TrackeraTable } from "../../..";
import { useMemo, useState } from "react";
import { showErrorToast, showSuccessToast } from "../../../../utils/toast-handler/showToast";
import { WORKLOG_STATUS, type Worklog, type WorklogError } from "../../../../shared/types";
import { formatDate, getFormikFieldError, getFormikFieldStatus } from "../../../../utils";
import { worklogApis } from "../../../../state/api";

interface ManageWorklogModalProps {
  setIsOpen: (isOpen: boolean) => void;
  refreshWorklogData: () => void;
  selectedWorklog?: Worklog;
  setSelectedWorklog?: (worklog: Worklog | undefined) => void;
  jiraLinked: boolean;
}

const ManageWorklogModal = (props: ManageWorklogModalProps) => {
  const { jiraLinked, selectedWorklog, setIsOpen, setSelectedWorklog, refreshWorklogData } = props;
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [worklogFileErrors, setWorklogFileErrors] = useState<WorklogError[]>([]);

  const isEditMode = (): boolean => {
    return selectedWorklog !== undefined;
  };

  const getLogDate = (): dayjs.Dayjs => {
    if (isEditMode()) {
      return dayjs(selectedWorklog!.workDate);
    } else {
      return dayjs().hour() < 12 ? dayjs().subtract(1, "day") : dayjs();
    }
  };

  const getInitialValues = useMemo(
    () => ({
      mode: isEditMode() ? "edit" : "add",
      logName: isEditMode() ? selectedWorklog!.name : "",
      logDate: getLogDate(),
      logFile: null,
      reEvaluate: false,
      syncToJira: false,
    }),
    [selectedWorklog]
  );

  const manageWorklogFormik = useFormik({
    initialValues: getInitialValues,
    validationSchema: manageWorklog,
    enableReinitialize: true,
    onSubmit: async (values) => {
      setIsLoading(true);
      try {
        const formData = getFormData(values, isEditMode());
        const response = await worklogApis.updateWorklog(isEditMode() ? selectedWorklog!.id : null, formData);
        showSuccessToast(response.data);
        handleModalClose();
        refreshWorklogData();
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

  const handleFileUpload = (file: File | null) => {
    manageWorklogFormik.setFieldValue("logFile", file);
    manageWorklogFormik.setFieldTouched("logFile", true, false);
  };

  const handleFileDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      handleFileUpload(files[0]);
    }
  };

  const handleModalClose = () => {
    manageWorklogFormik.resetForm();
    setFileList([]);
    setWorklogFileErrors([]);
    setIsOpen(false);
    setSelectedWorklog?.(undefined);
  };

  const getFormData = (values: typeof manageWorklogFormik.values, isEdit: boolean): FormData => {
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
        okButtonProps: {
          loading: isLoading,
          disabled: !manageWorklogFormik.isValid || !manageWorklogFormik.dirty || isLoading,
        },
        cancelButtonProps: { disabled: isLoading },
        onOk: () => {
          manageWorklogFormik.submitForm();
        },
        onCancel: handleModalClose,
        width: worklogFileErrors.length > 0 ? 900 : 520,
      }}
    >
      <form className={worklogModalClasses.worklog_form}>
        <div className={worklogModalClasses.form_group}>
          <Form.Item
            className={worklogModalClasses.form_item}
            validateStatus={getFormikFieldStatus(manageWorklogFormik, "logName")}
            help={getFormikFieldError(manageWorklogFormik, "logName")}
          >
            <span className={worklogModalClasses.label}>Log Name:</span>
            <Input
              placeholder="Enter log name"
              name="logName"
              value={manageWorklogFormik.values.logName}
              onChange={manageWorklogFormik.handleChange}
              onBlur={manageWorklogFormik.handleBlur}
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
            validateStatus={getFormikFieldStatus(manageWorklogFormik, "logDate")}
            help={getFormikFieldError(manageWorklogFormik, "logDate") as string}
          >
            <span className={worklogModalClasses.label}>Log date:</span>
            <DatePicker
              name="logDate"
              value={manageWorklogFormik.values.logDate}
              placeholder="Select log date"
              onChange={(date) => manageWorklogFormik.setFieldValue("logDate", date)}
              onBlur={() => manageWorklogFormik.setFieldTouched("logDate", true)}
              disabled={isLoading}
            />
            {isEditMode() &&
              manageWorklogFormik.values.logDate !== null &&
              formatDate(manageWorklogFormik.values.logDate) !== selectedWorklog?.workDate &&
              selectedWorklog?.status !== WORKLOG_STATUS.NOT_SYNCED && (
                <Alert
                  message="Changing the log date will be applied to the synced worklogs"
                  type="warning"
                  showIcon
                  style={{ marginTop: "10px" }}
                />
              )}
          </Form.Item>
        </div>
        {isEditMode() && (
          <div className={worklogModalClasses.form_group}>
            <span className={worklogModalClasses.label}>Re-evaluate worklog file:</span>
            <Switch
              disabled={isLoading}
              value={manageWorklogFormik.values.reEvaluate}
              onChange={(value) => manageWorklogFormik.setFieldValue("reEvaluate", value)}
            />
          </div>
        )}
        {(!isEditMode() || manageWorklogFormik.values.reEvaluate) && (
          <>
            <div className={`${worklogModalClasses.form_group} ${worklogModalClasses.upload_group}`}>
              <Form.Item
                className={worklogModalClasses.form_item}
                validateStatus={getFormikFieldStatus(manageWorklogFormik, "logFile")}
                help={getFormikFieldError(manageWorklogFormik, "logFile")}
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
                    manageWorklogFormik.setFieldTouched("logFile", true, false);
                    return true; // allow removal
                  }}
                  disabled={isLoading}
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
              <span className={worklogModalClasses.label}>Sync to Jira after upload:</span>
              <Switch
                disabled={!jiraLinked || isLoading}
                value={manageWorklogFormik.values.syncToJira}
                onChange={(value) => manageWorklogFormik.setFieldValue("syncToJira", value)}
              />
              {!jiraLinked && (
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
              rowKey={(record) => record.row}
            />
          </CollapsibleSection>
        </div>
      )}
    </WorklogModal>
  );
};

export default ManageWorklogModal;
