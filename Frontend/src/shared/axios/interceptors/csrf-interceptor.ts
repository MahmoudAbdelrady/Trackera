import type { InternalAxiosRequestConfig } from "axios";

const csrfInterceptor = (config: InternalAxiosRequestConfig) => {
  const csrfToken = document.cookie
    .split("; ")
    .find((v) => v.startsWith("csrfToken="))
    ?.split("=")[1];

  if (csrfToken) {
    config.headers["X-CSRF-TOKEN"] = csrfToken;
  }

  return config;
};

export default csrfInterceptor;
