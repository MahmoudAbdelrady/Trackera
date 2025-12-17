import requestInstance from "../../shared/axios/request-instance";

const isAuthenticated = async () => {
  try {
    await requestInstance.get("/auth/session");
  } catch (error: any) {
    throw error;
  }
};

const refreshToken = async () => {
  try {
    await requestInstance.post("/auth/jwt/refresh");
  } catch (error: any) {
    throw error;
  }
};

const authApis = {
  isAuthenticated,
  refreshToken,
};

export default authApis;
