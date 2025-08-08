import toast from "react-hot-toast";
import { formateToastResponse } from "./format-toast-response";

export const showSuccessToast = (response: any) => {
  toast.success(formateToastResponse(response));
};

export const showErrorToast = (error: any) => {
  if (error?.response?.status !== 429) {
    toast.error(formateToastResponse(error, true));
  }
};
