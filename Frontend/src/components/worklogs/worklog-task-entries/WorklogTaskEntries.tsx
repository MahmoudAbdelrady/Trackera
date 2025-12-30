import { useCallback, useEffect, useMemo, useState } from "react";
import { WORKLOG_STATUS, type SyncPayload, type WorklogEntry, type WorklogTask } from "../../../shared/types";
import TrackeraTable from "../../trackera-table/TrackeraTable";
import WorkLogModal from "../modals/worklog-modal/WorklogModal";
import { createWorklogEntryColumns } from "../../";
import buildSyncButtonProps from "../../../utils/buildWorklogSyncButtonProps";
import { worklogApis } from "../../../state/api";
import { showErrorToast } from "../../../utils/toast-handler/showToast";
import { useNavigate } from "react-router-dom";
import { Alert } from "antd";
import worklogModalClasses from "../modals/worklog-modal/scss/worklog-modal.module.css";
import type { UserInfo } from "../../../shared/types/auth";

interface WorklogTaskEntriesProps {
  loggedUserData: UserInfo;
  worklogId: string;
  selectedTask: WorklogTask;
  worklogEntries: WorklogEntry[];
  setWorklogEntries: (entries: WorklogEntry[]) => void;
  refetchData: () => void;
  triggerSync: (params: SyncPayload) => void;
  onCloseHandler: () => void;
}

const WorklogTaskEntries = (props: WorklogTaskEntriesProps) => {
  const {
    loggedUserData,
    worklogId,
    selectedTask,
    worklogEntries,
    setWorklogEntries,
    refetchData,
    triggerSync,
    onCloseHandler,
  } = props;
  const navigate = useNavigate();

  const [isFetchingEntries, setIsFetchingEntries] = useState<boolean>(false);
  const [selectedWorklogEntries, setSelectedWorklogEntries] = useState<WorklogEntry[]>([]);
  const [isDeletingEntry, setIsDeletingEntry] = useState<boolean>(false);
  const [selectedEntry, setSelectedEntry] = useState<WorklogEntry | null>(null);
  const [deleteEntryVisible, setDeleteEntryVisible] = useState<boolean>(false);

  const worklogEntryColumns = useMemo(
    () =>
      createWorklogEntryColumns({
        worklogId: worklogId!,
        worklogEntries: worklogEntries,
        jiraLinked: loggedUserData?.jiraLinked,
        onSync: triggerSync,
        onDelete: (record) => {
          setDeleteEntryVisible(true);
          setSelectedEntry(record);
        },
      }),
    [worklogId, worklogEntries, loggedUserData?.jiraLinked, triggerSync]
  );

  const fetchEntries = useCallback(async () => {
    setIsFetchingEntries(true);
    try {
      const result = await worklogApis.getWorklogTaskEntries(worklogId!, selectedTask!.taskName);
      setWorklogEntries(result);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsFetchingEntries(false);
  }, [worklogId, selectedTask.taskName]);

  useEffect(() => {
    fetchEntries();
  }, []);

  useEffect(() => {
    if (selectedWorklogEntries.length > 0) {
      const updatedSelectedEntries = selectedWorklogEntries
        .map((selectedEntry) => worklogEntries.find((entry) => entry.id === selectedEntry.id))
        .filter((entry): entry is WorklogEntry => entry !== undefined);

      setSelectedWorklogEntries(updatedSelectedEntries);
    }
  }, [worklogEntries]);

  const clearDeleteEntryModalFields = () => {
    setDeleteEntryVisible(false);
    setSelectedEntry(null);
  };

  const handleDeleteWorkLogEntry = async () => {
    setIsDeletingEntry(true);

    try {
      const result = await worklogApis.deleteWorklog(worklogId, { entryIds: [selectedEntry?.id.toString() ?? ""] });
      if (result.isLast) {
        navigate("/");
      } else {
        if (result.isLastOfTask) {
          onCloseHandler();
        } else {
          fetchEntries();
        }
        refetchData();
      }
    } catch (error: any) {
      showErrorToast(error);
    }

    setIsDeletingEntry(false);
    clearDeleteEntryModalFields();
  };

  return (
    <>
      {deleteEntryVisible && (
        <WorkLogModal
          title={`Delete ${selectedTask?.taskName} Entry`}
          properties={{
            open: true,
            centered: true,
            closable: !isDeletingEntry,
            keyboard: !isDeletingEntry,
            maskClosable: !isDeletingEntry,
            okText: "Delete",
            onOk: handleDeleteWorkLogEntry,
            okButtonProps: { loading: isDeletingEntry, disabled: isDeletingEntry, danger: true },
            cancelButtonProps: { disabled: isDeletingEntry },
            onCancel: clearDeleteEntryModalFields,
          }}
        >
          <p className={worklogModalClasses.delete_message}>
            Are you sure you want to delete this entry? This action cannot be undone.
          </p>
          {(selectedTask?.status === WORKLOG_STATUS.SYNCED || selectedTask?.status === WORKLOG_STATUS.PARTIALLY) && (
            <Alert
              message="This entry is synced with Jira and will be unsynced upon deletion."
              type="warning"
              showIcon
              className={worklogModalClasses.alert_message}
            />
          )}
        </WorkLogModal>
      )}

      <WorkLogModal
        title={`${selectedTask?.taskName} Task Entries`}
        properties={{
          open: true,
          centered: true,
          footer: null,
          width: "80%",
          onCancel: () => {
            onCloseHandler();
            selectedWorklogEntries.length > 0 && setSelectedWorklogEntries([]);
          },
        }}
      >
        <TrackeraTable<WorklogEntry>
          properties={{
            columns: worklogEntryColumns,
            dataSource: worklogEntries,
            loading: isFetchingEntries,
            pagination: { pageSize: 5 },
            rowSelection: {
              selectedRowKeys: selectedWorklogEntries.map((entry) => entry.id.toString()),
              onChange: (_, selectedRows: WorklogEntry[]) => {
                setSelectedWorklogEntries(selectedRows);
              },
              getCheckboxProps: (record) => ({
                disabled:
                  !loggedUserData?.jiraLinked ||
                  record.status === WORKLOG_STATUS.SYNC_IN_PROGRESS ||
                  record.status === WORKLOG_STATUS.UNSYNC_IN_PROGRESS,
              }),
            },
          }}
          rowKey={(record) => record.id}
          actionButtons={buildSyncButtonProps({
            loggedUserData: loggedUserData,
            selectedItems: selectedWorklogEntries,
            extractIdentifier: (entry: WorklogEntry) => entry.id,
            isEntry: true,
            triggerSync: ({ entryIds, sync }) => triggerSync({ worklogId: worklogId, entryIds, sync }),
          })}
        />
      </WorkLogModal>
    </>
  );
};

export default WorklogTaskEntries;
