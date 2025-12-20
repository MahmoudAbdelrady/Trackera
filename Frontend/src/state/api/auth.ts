import requestInstance from "../../shared/axios/request-instance";

const isAuthenticated = async () => {
  await requestInstance.get("/auth/session");
};

const refreshToken = async () => {
  await requestInstance.post("/auth/jwt/refresh");
};

const authApis = {
  isAuthenticated,
  refreshToken,
};

export default authApis;
