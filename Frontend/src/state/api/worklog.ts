import requestInstance from "../../shared/axios/request-instance";
import type { SyncPayload, UpdateWorkLogDetailPayloadDTO, WorklogSelection } from "../../shared/types";

const getWorklogs = async (pageNum: number = 0, pageSize: number = 10, searchFilters?: Record<string, any>) => {
  const response = await requestInstance.post(`/worklog/search?page=${pageNum}&size=${pageSize}`, searchFilters);
  return response.data;
};

const getWorklogInfo = async (worklogId: string) => {
  const response = await requestInstance.get(`/worklog/${worklogId}`);
  return response.data;
};

const getWorklogSummary = async () => {
  const response = await requestInstance.get("/worklog/summary");
  return response.data;
};

const getWorklogTasks = async (worklogId: string) => {
  const response = await requestInstance.get(`/worklog/${worklogId}/details`);
  return response.data;
};

const getWorklogTaskEntries = async (worklogId: string, taskName: string) => {
  const response = await requestInstance.get(`/worklog/${worklogId}/details/task?taskName=${taskName}`);
  return response.data;
};

const updateWorklogDetail = async (worklogId: string, detailsPayload: UpdateWorkLogDetailPayloadDTO) => {
  const response = await requestInstance.put(`/worklog/${worklogId}/details`, detailsPayload);
  return response.data;
};

const updateWorklog = async (worklogId: string | null, formData: FormData) => {
  let response;
  if (worklogId) {
    response = await requestInstance.put(`/worklog/${worklogId}`, formData);
  } else {
    response = await requestInstance.post("/worklog", formData);
  }
  return response.data;
};

const deleteWorklog = async (worklogId: string, worklogSelection: WorklogSelection | null = null) => {
  const response = await requestInstance.delete(`/worklog/${worklogId}`, { data: { ...worklogSelection } });
  return response.data;
};

const syncWorklog = async (payload: SyncPayload) => {
  const { worklogId: worklogId, taskNames, entryIds, sync } = payload;
  const response = await requestInstance.post(`/worklog/${worklogId}/sync${sync ? "" : "?sync=false"}`, {
    taskNames,
    entryIds,
  });
  return response.data;
};

const worklogApis = {
  getWorklogs,
  getWorklogInfo,
  getWorklogSummary,
  getWorklogTasks,
  getWorklogTaskEntries,
  updateWorklog,
  updateWorklogDetail,
  deleteWorklog,
  syncWorklog,
};

export default worklogApis;
