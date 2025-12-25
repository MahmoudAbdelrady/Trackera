import { useEffect, useMemo, useState } from "react";
import type { WorklogEntry, WorklogTaskEntriesProps } from "../../../shared/types";
import TrackeraTable from "../../trackera-table/TrackeraTable";
import WorklogModal from "../modals/worklog-modal/WorklogModal";
import { WorkLogEntryColumns } from "../../";
import buildSyncButtonProps from "../../../utils/buildWorkLogSyncButtonProps";
import { workLogApis } from "../../../state/api";
import { showErrorToast } from "../../../utils/toast-handler/showToast";
import { useNavigate } from "react-router-dom";
import { Alert } from "antd";
import worklogModalClasses from "../modals/worklog-modal/scss/worklog-modal.module.css";

const WorklogTaskEntries = (props: WorklogTaskEntriesProps) => {
  const {
    loggedUserData,
    worklogId,
    selectedTask,
    worklogEntries,
    isFetchingEntries,
    fetchEntries,
    refetchData,
    triggerSync,
    onCloseHandler,
  } = props;
  const navigate = useNavigate();

  const [selectedWorklogEntries, setSelectedWorklogEntries] = useState<WorklogEntry[]>([]);
  const [isDeletingEntry, setIsDeletingEntry] = useState<boolean>(false);
  const [selectedEntry, setSelectedEntry] = useState<WorklogEntry | null>(null);
  const [deleteEntryVisible, setDeleteEntryVisible] = useState<boolean>(false);

  const worklogEntryColumns = useMemo(
    () =>
      WorkLogEntryColumns({
        worklogId: worklogId!,
        worklogEntries: worklogEntries,
        jiraLinked: loggedUserData?.jiraLinked,
        onSync: triggerSync,
        onDelete: (record) => {
          setDeleteEntryVisible(true);
          setSelectedEntry(record);
        },
      }),
    [loggedUserData?.jiraLinked, triggerSync]
  );

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
      const result = await workLogApis.deleteWorkLog(worklogId, { entryIds: [selectedEntry?.id.toString() ?? ""] });
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
        <WorklogModal
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
          {(selectedTask?.status === "SYNCED" || selectedTask?.status === "PARTIALLY") && (
            <Alert
              message="This entry is synced with Jira and will be unsynced upon deletion."
              type="warning"
              showIcon
              className={worklogModalClasses.alert_message}
            />
          )}
        </WorklogModal>
      )}

      <WorklogModal
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
                  !loggedUserData?.jiraLinked || ["SYNC_IN_PROGRESS", "UNSYNC_IN_PROGRESS"].includes(record.status),
              }),
            },
          }}
          actionButtons={buildSyncButtonProps({
            loggedUserData: loggedUserData,
            selectedItems: selectedWorklogEntries,
            extractIdentifier: (entry: WorklogEntry) => entry.id,
            isEntry: true,
            triggerSync: ({ entryIds, sync }) => triggerSync({ workLogId: worklogId, entryIds, sync }),
          })}
        />
      </WorklogModal>
    </>
  );
};

export default WorklogTaskEntries;
