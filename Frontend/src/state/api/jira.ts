import requestInstance from "../../shared/axios/request-instance";

const getJiraTasks = async (forceUpdate: boolean = false) => {
  const response = await requestInstance.get(`/jira/tasks${forceUpdate ? "?forceUpdate=true" : ""}`);
  return response.data;
};

const getJiraSites = async () => {
  const response = await requestInstance.get("/jira/sites");
  return response.data;
};

const jiraApis = {
  getJiraTasks,
  getJiraSites,
};

export default jiraApis;
