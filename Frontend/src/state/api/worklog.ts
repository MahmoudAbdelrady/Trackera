import requestInstance from "../../shared/axios/request-instance";
import type { WorklogSelection } from "../../shared/types";

const getWorkLogs = async (pageNum: number = 0, pageSize: number = 10, searchFilters: Record<string, any>) => {
  const response = await requestInstance.post(`/worklog/search?page=${pageNum}&size=${pageSize}`, searchFilters);
  return response.data;
};

const getWorkLogSummary = async () => {
  const response = await requestInstance.get("/worklog/summary");
  return response.data;
};

const deleteWorkLog = async (workLogId: string, workLogSelection: WorklogSelection | null = null) => {
  const response = await requestInstance.delete(`/worklog/${workLogId}`, { data: { ...workLogSelection } });
  return response.data;
};

const workLogApis = {
  getWorkLogs,
  getWorkLogSummary,
  deleteWorkLog,
};

export default workLogApis;
