import toast from "react-hot-toast";
import { formateToastResponse } from "./formatToastResponse";

export const showSuccessToast = (response: any) => {
  toast.success(formateToastResponse(response));
};

export const showErrorToast = (error: any) => {
  toast.error(formateToastResponse(error, true));
};
