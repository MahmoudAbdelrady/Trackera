import { useAuthStore } from "../../../state/store";
import { showErrorToast } from "../../../utils/toast-handler/showToast";
import requestInstance from "../request-instance";

const refreshJwtInterceptor = async (error: any) => {
  const filteredAPIs = ["/auth/session", "/auth/jwt/refresh", "/auth/login", "/auth/oauth/[^/]+/callback"].map((api) => new RegExp(api));
  const originalRequest = error.config;
  if (filteredAPIs.every((pattern) => !pattern.test(originalRequest.url)) && error.response?.status === 401 && !originalRequest._retry) {
    originalRequest._retry = true;
    try {
      await requestInstance.post("/auth/jwt/refresh");
      return requestInstance(originalRequest);
    } catch (refreshError: any) {
      useAuthStore.getState().setAuthenticated(false);
      // showErrorToast("Session expired, please login again.");
      throw refreshError;
    }
  }

  return Promise.reject(error);
};

export default refreshJwtInterceptor;
