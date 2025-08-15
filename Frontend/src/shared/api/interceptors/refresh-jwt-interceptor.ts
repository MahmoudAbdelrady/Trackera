import { useAuthStore } from "../../../state/store";
import requestInstance from "../request-instance";

const refreshJwtInterceptor = async (error: any) => {
  const filteredAPIs = ["/auth/refresh-jwt", "/auth/login"];
  const originalRequest = error.config;
  if (
    error.response.status === 401 &&
    !originalRequest._retry &&
    filteredAPIs.every((api) => !originalRequest.url.includes(api))
  ) {
    originalRequest._retry = true;
    try {
      const response = await requestInstance.post("/auth/refresh-jwt");
      useAuthStore.getState().login(response.data.data.token);
      return requestInstance(originalRequest);
    } catch (refreshError: any) {
      useAuthStore.getState().logout();
      throw refreshError;
    }
  }

  return Promise.reject(error);
};

export default refreshJwtInterceptor;
