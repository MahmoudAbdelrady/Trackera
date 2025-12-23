import requestInstance from "../../shared/axios/request-instance";
import type { WorklogSelection } from "../../shared/types";

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

const deleteWorkLog = async (workLogId: string, workLogSelection: WorklogSelection | null = null) => {
  const response = await requestInstance.delete(`/worklog/${workLogId}`, { data: { ...workLogSelection } });
  return response.data;
};

const workLogApis = {
  getWorkLogs,
  getWorkLogInfo,
  getWorkLogSummary,
  getWorkLogTasks,
  getWorkLogTaskEntries,
  deleteWorkLog,
};

export default workLogApis;
