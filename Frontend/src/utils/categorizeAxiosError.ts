import { AxiosError } from "axios";
import type { AxiosErrorType } from "../shared/types";

export const categorizeAxiosError = (error: unknown): AxiosErrorType | null => {
  if (error instanceof AxiosError && error.response?.status) {
    const status = error.response.status;
    if (status >= 500) return "SERVER";
    if (status === 401) return "AUTH";
    if (status === 403) return "FORBIDDEN";
  }
  return error ? "SERVER" : null;
};
