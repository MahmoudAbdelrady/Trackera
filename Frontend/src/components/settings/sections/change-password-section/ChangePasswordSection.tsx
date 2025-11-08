import { useState } from "react";
import { userQueries } from "../../../../state/queries";
import classes from "./scss/change-password.module.css";
import { useFormik } from "formik";
import { updatePasswordSchema } from "../../../../shared/yup-schemas";
import requestInstance from "../../../../shared/axios/request-instance";
import { showErrorToast, showSuccessToast } from "../../../../utils/toast-handler/showToast";
import inputFieldClasses from "../../../input-field/scss/input-field.module.css";
import InputField from "../../../input-field/InputField";
import { Lock } from "lucide-react";
import { Button } from "antd";

interface ChangePasswordFormFields {
  currentPassword?: string;
  newPassword: string;
  confirmNewPassword: string;
}

const ChangePasswordSection = () => {
  const { data: userData, refetch: refetchUser } = userQueries.useMeQuery();
  const [isUpdatingPassword, setIsUpdatingPassword] = useState(false);
  const changePasswordFormik = useFormik({
    initialValues: (Object.keys(updatePasswordSchema(!!userData?.passwordSet).fields) as (keyof ChangePasswordFormFields)[]).reduce((acc, key) => {
      acc[key] = "";
      return acc;
    }, {} as ChangePasswordFormFields),
    validationSchema: updatePasswordSchema(!!userData?.passwordSet),
    onSubmit: async (values) => {
      setIsUpdatingPassword(true);
      try {
        const response = await requestInstance.post("/user/password", {
          ...values,
        });
        changePasswordFormik.resetForm();
        await refetchUser();
        showSuccessToast(response.data);
      } catch (error: any) {
        showErrorToast(error);
        changePasswordFormik.setFieldValue("currentPassword", "");
      }
      setIsUpdatingPassword(false);
    },
  });

  return (
    <form onSubmit={changePasswordFormik.handleSubmit} className={classes.password_form}>
      {userData?.passwordSet && (
        <InputField
          label="Current Password"
          icon={<Lock className={inputFieldClasses.input_icon} />}
          placeholder="Enter your current password"
          name="currentPassword"
          value={changePasswordFormik.values.currentPassword}
          onChange={changePasswordFormik.handleChange}
          onBlur={changePasswordFormik.handleBlur}
          type="password"
          disabled={isUpdatingPassword}
          error={changePasswordFormik.touched.currentPassword && changePasswordFormik.errors.currentPassword ? changePasswordFormik.errors.currentPassword : undefined}
        />
      )}
      <InputField
        label="New Password"
        icon={<Lock className={inputFieldClasses.input_icon} />}
        placeholder="Enter your new password"
        name="newPassword"
        value={changePasswordFormik.values.newPassword}
        onChange={changePasswordFormik.handleChange}
        onBlur={changePasswordFormik.handleBlur}
        type="password"
        disabled={isUpdatingPassword}
        error={changePasswordFormik.touched.newPassword && changePasswordFormik.errors.newPassword ? changePasswordFormik.errors.newPassword : undefined}
      />
      <InputField
        label="Confirm New Password"
        icon={<Lock className={inputFieldClasses.input_icon} />}
        placeholder="Confirm your new password"
        name="confirmNewPassword"
        value={changePasswordFormik.values.confirmNewPassword}
        onChange={changePasswordFormik.handleChange}
        onBlur={changePasswordFormik.handleBlur}
        type="password"
        disabled={isUpdatingPassword}
        error={changePasswordFormik.touched.confirmNewPassword && changePasswordFormik.errors.confirmNewPassword ? changePasswordFormik.errors.confirmNewPassword : undefined}
      />
      <Button
        type="primary"
        htmlType="submit"
        className={classes.password_form_button}
        loading={isUpdatingPassword}
        disabled={!changePasswordFormik.isValid || !changePasswordFormik.dirty || isUpdatingPassword}
      >
        {userData?.passwordSet ? "Update Password" : "Set Password"}
      </Button>
    </form>
  );
};

export default ChangePasswordSection;
