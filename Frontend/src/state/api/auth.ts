import requestInstance from "../../shared/axios/request-instance";

const isAuthenticated = async () => {
  try {
    await requestInstance.get("/auth/session");
  } catch (error: any) {
    throw error;
  }
};

const authApis = {
  isAuthenticated,
};

export default authApis;
