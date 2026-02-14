import { useCallback, useEffect, useMemo, useState } from "react";
import { WORKLOG_STATUS, type WorklogEntry, type WorklogTask } from "../../../shared/types";
import TrackeraTable from "../../trackera-table/TrackeraTable";
import WorkLogModal from "../modals/worklog-modal/WorklogModal";
import { createWorklogEntryColumns, DeleteWarning } from "../../";
import buildSyncButtonProps from "../../../utils/buildWorklogSyncButtonProps";
import { worklogApis } from "../../../state/api";
import { showErrorToast } from "../../../utils/toast-handler/showToast";
import { useNavigate } from "react-router-dom";
import type { UserInfo } from "../../../shared/types";
import type { JiraSyncSSEReturn } from "../../../shared/hooks/useJiraSyncSSE";
import EditWorklogEntry from "../edit-worklog-entry/EditWorklogEntry";

interface WorklogTaskEntriesProps {
  loggedUserData: UserInfo;
  worklogId: string;
  selectedTask: WorklogTask;
  worklogEntries: WorklogEntry[];
  setWorklogEntries: (entries: WorklogEntry[]) => void;
  refetchData: () => void;
  jiraSyncSSE: JiraSyncSSEReturn;
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
    jiraSyncSSE,
    onCloseHandler,
  } = props;
  const navigate = useNavigate();

  const [isFetchingEntries, setIsFetchingEntries] = useState<boolean>(false);
  const [selectedWorklogEntries, setSelectedWorklogEntries] = useState<WorklogEntry[]>([]);
  const [selectedEntry, setSelectedEntry] = useState<WorklogEntry | null>(null);
  const [entryModalState, setEntryModalState] = useState<{
    type: "edit" | "delete" | null;
    entry: WorklogEntry | null;
  }>({
    type: null,
    entry: null,
  });
  const [isDeletingEntry, setIsDeletingEntry] = useState<boolean>(false);

  const worklogEntryColumns = useMemo(
    () =>
      createWorklogEntryColumns({
        worklogId: worklogId!,
        worklogEntries: worklogEntries,
        jiraLinked: loggedUserData?.jiraLinked,
        jiraSyncSSE: jiraSyncSSE,
        onEdit: (record) => {
          setSelectedEntry(record);
          setEntryModalState({ type: "edit", entry: record });
        },
        onDelete: (record) => {
          setSelectedEntry(record);
          setEntryModalState({ type: "delete", entry: record });
        },
      }),
    [worklogId, worklogEntries, loggedUserData?.jiraLinked, jiraSyncSSE],
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

  const clearEntryModalFields = () => {
    setEntryModalState({ type: null, entry: null });
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
    clearEntryModalFields();
  };

  return (
    <>
      {entryModalState.type === "edit" && (
        <EditWorklogEntry
          worklogId={worklogId}
          taskName={selectedTask.taskName}
          worklogEntry={entryModalState.entry!}
          setIsOpen={clearEntryModalFields}
          jiraLinked={loggedUserData?.jiraLinked ?? false}
          refetchData={refetchData}
        />
      )}

      {entryModalState.type === "delete" && (
        <WorkLogModal
          title={`Delete ${selectedTask.taskName} Entry`}
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
            onCancel: clearEntryModalFields,
          }}
        >
          <DeleteWarning
            message="Are you sure you want to delete this entry? This action cannot be undone."
            showAlert={
              selectedTask?.status === WORKLOG_STATUS.SYNCED || selectedTask?.status === WORKLOG_STATUS.PARTIALLY
            }
            alertMessage="This entry is synced with Jira and will be unsynced upon deletion."
          />
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
                  record.status === WORKLOG_STATUS.IN_QUEUE ||
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
            worklogId: worklogId!,
            isEntry: true,
            jiraSyncSSE: jiraSyncSSE,
          })}
        />
      </WorkLogModal>
    </>
  );
};

export default WorklogTaskEntries;
