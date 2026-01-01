import { useState } from "react";
import { userQueries } from "../../../../state/queries";
import classes from "./scss/change-password.module.css";
import { useFormik } from "formik";
import { updatePasswordSchema } from "../../../../shared/yup-schemas";
import { showErrorToast, showSuccessToast } from "../../../../utils/toast-handler/showToast";
import InputField from "../../../input-field/InputField";
import { Lock } from "lucide-react";
import { Button } from "antd";
import type { ChangePasswordFormFields } from "../../../../shared/types";
import { userApis } from "../../../../state/api";
import { getFormikFieldProps } from "../../../../utils";

const ChangePasswordSection = () => {
  const { data: userData, refetch: refetchUser } = userQueries.useMeQuery();
  const [isUpdatingPassword, setIsUpdatingPassword] = useState(false);
  const hasPassword: boolean = !!userData?.passwordSet;

  const changePasswordFormik = useFormik<ChangePasswordFormFields>({
    initialValues: {
      ...(hasPassword ? { currentPassword: "" } : {}),
      newPassword: "",
      confirmNewPassword: "",
    },
    validationSchema: updatePasswordSchema(hasPassword),
    onSubmit: async (values: ChangePasswordFormFields) => {
      setIsUpdatingPassword(true);
      try {
        const result = await userApis.changePassword(values);
        await refetchUser();
        changePasswordFormik.resetForm();
        showSuccessToast(result);
      } catch (error: any) {
        showErrorToast(error);
        if (hasPassword) {
          changePasswordFormik.setFieldValue("currentPassword", "");
        }
      }
      setIsUpdatingPassword(false);
    },
  });

  return (
    <form onSubmit={changePasswordFormik.handleSubmit} className={classes.password_form}>
      {hasPassword && (
        <InputField
          label="Current Password"
          icon={<Lock />}
          placeholder="Enter your current password"
          type="password"
          {...getFormikFieldProps(changePasswordFormik, "currentPassword", isUpdatingPassword)}
        />
      )}
      <InputField
        label="New Password"
        icon={<Lock />}
        placeholder="Enter your new password"
        type="password"
        {...getFormikFieldProps(changePasswordFormik, "newPassword", isUpdatingPassword)}
      />
      <InputField
        label="Confirm New Password"
        icon={<Lock />}
        placeholder="Confirm your new password"
        type="password"
        {...getFormikFieldProps(changePasswordFormik, "confirmNewPassword", isUpdatingPassword)}
      />
      <Button
        type="primary"
        htmlType="submit"
        className={classes.password_form_button}
        loading={isUpdatingPassword}
        disabled={!changePasswordFormik.isValid || !changePasswordFormik.dirty}
      >
        {userData?.passwordSet ? "Update Password" : "Set Password"}
      </Button>
    </form>
  );
};

export default ChangePasswordSection;
