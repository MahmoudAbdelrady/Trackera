import { useAuthStore } from "../../../state/store";
import { showErrorToast } from "../../../utils/toast-handler/showToast";
import requestInstance from "../request-instance";

const refreshJwtInterceptor = async (error: any) => {
  const filteredAPIs = ["/auth/jwt/refresh", "/auth/login", "/auth/oauth/[^/]+/callback"].map((api) => new RegExp(api));
  const originalRequest = error.config;
  if (error.response.status === 401 && !originalRequest._retry && filteredAPIs.every((pattern) => !pattern.test(originalRequest.url))) {
    originalRequest._retry = true;
    try {
      const response = await requestInstance.post("/auth/jwt/refresh");
      useAuthStore.getState().login(response.data.token);
      return requestInstance(originalRequest);
    } catch (refreshError: any) {
      useAuthStore.getState().logout();
      showErrorToast("Session expired, please login again.");
      throw refreshError;
    }
  }

  return Promise.reject(error);
};

export default refreshJwtInterceptor;
