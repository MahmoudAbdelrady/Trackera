import requestInstance from "../../shared/axios/request-instance";
import type { SyncPayload, WorklogSelection } from "../../shared/types";

const getWorkLogs = async (pageNum: number = 0, pageSize: number = 10, searchFilters: Record<string, any>) => {
  const response = await requestInstance.post(`/worklog/search?page=${pageNum}&size=${pageSize}`, searchFilters);
  return response.data;
};

const getWorkLogInfo = async (workLogId: string) => {
  const response = await requestInstance.get(`/worklog/${workLogId}`);
  return response.data;
};

const getWorkLogSummary = async () => {
  const response = await requestInstance.get("/worklog/summary");
  return response.data;
};

const getWorkLogTasks = async (workLogId: string) => {
  const response = await requestInstance.get(`/worklog/${workLogId}/details`);
  return response.data;
};

const getWorkLogTaskEntries = async (workLogId: string, taskName: string) => {
  const response = await requestInstance.get(`/worklog/${workLogId}/details/task?taskName=${taskName}`);
  return response.data;
};

const updateWorkLog = async (workLogId: string | null, formData: FormData) => {
  let response;
  if (workLogId) {
    response = await requestInstance.put(`/worklog/${workLogId}`, formData);
  } else {
    response = await requestInstance.post("/worklog", formData);
  }
  return response.data;
};

const deleteWorkLog = async (workLogId: string, workLogSelection: WorklogSelection | null = null) => {
  const response = await requestInstance.delete(`/worklog/${workLogId}`, { data: { ...workLogSelection } });
  return response.data;
};

const syncWorkLog = async (payload: SyncPayload) => {
  const { workLogId, taskNames, entryIds, sync } = payload;
  const response = await requestInstance.post(`/worklog/${workLogId}/sync${sync ? "" : "?sync=false"}`, {
    taskNames,
    entryIds,
  });
  return response.data;
};

const workLogApis = {
  getWorkLogs,
  getWorkLogInfo,
  getWorkLogSummary,
  getWorkLogTasks,
  getWorkLogTaskEntries,
  updateWorkLog,
  deleteWorkLog,
  syncWorkLog,
};

export default workLogApis;
