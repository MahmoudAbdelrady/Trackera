import { authApis } from "../../../state/api";
import { useAuthStore } from "../../../state/store";
import requestInstance from "../request-instance";

const refreshJwtInterceptor = async (error: any) => {
  const filteredAPIs = ["/auth/session", "/auth/jwt/refresh", "/auth/login", "/auth/oauth/[^/]+/callback"].map(
    (api) => new RegExp(api)
  );
  const originalRequest = error.config;

  if (
    filteredAPIs.every((pattern) => !pattern.test(originalRequest.url)) &&
    error.response?.status === 401 &&
    !originalRequest._retry
  ) {
    originalRequest._retry = true;
    try {
      await authApis.refreshToken();
      return requestInstance(originalRequest);
    } catch (refreshError: any) {
      useAuthStore.getState().setAuthenticated(false);
      throw refreshError;
    }
  }

  return Promise.reject(error);
};

export default refreshJwtInterceptor;
