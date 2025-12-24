import { useMemo, useState } from "react";
import { userQueries } from "../../../../state/queries";
import classes from "./scss/change-password.module.css";
import { useFormik } from "formik";
import { updatePasswordSchema } from "../../../../shared/yup-schemas";
import { showErrorToast, showSuccessToast } from "../../../../utils/toast-handler/showToast";
import inputFieldClasses from "../../../input-field/scss/input-field.module.css";
import InputField from "../../../input-field/InputField";
import { Lock } from "lucide-react";
import { Button } from "antd";
import type { ChangePasswordFormFields } from "../../../../shared/types";
import { userApis } from "../../../../state/api";

const ChangePasswordSection = () => {
  const { data: userData, refetch: refetchUser } = userQueries.useMeQuery();
  const [isUpdatingPassword, setIsUpdatingPassword] = useState(false);
  const hasPassword: boolean = useMemo(() => !!userData?.passwordSet, [userData?.passwordSet]);

  const getInitialValues = (): ChangePasswordFormFields => ({
    ...(hasPassword ? { currentPassword: "" } : {}),
    newPassword: "",
    confirmNewPassword: "",
  });

  const changePasswordFormik = useFormik({
    initialValues: getInitialValues(),
    validationSchema: updatePasswordSchema(hasPassword),
    onSubmit: async (values) => {
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

  const getFieldProps = (fieldName: keyof ChangePasswordFormFields) => ({
    name: fieldName,
    value: changePasswordFormik.values[fieldName],
    onChange: changePasswordFormik.handleChange,
    onBlur: changePasswordFormik.handleBlur,
    type: "password" as const,
    disabled: isUpdatingPassword,
    error:
      changePasswordFormik.touched[fieldName] && changePasswordFormik.errors[fieldName]
        ? changePasswordFormik.errors[fieldName]
        : undefined,
  });

  return (
    <form onSubmit={changePasswordFormik.handleSubmit} className={classes.password_form}>
      {hasPassword && (
        <InputField
          label="Current Password"
          icon={<Lock className={inputFieldClasses.input_icon} />}
          placeholder="Enter your current password"
          {...getFieldProps("currentPassword")}
        />
      )}
      <InputField
        label="New Password"
        icon={<Lock className={inputFieldClasses.input_icon} />}
        placeholder="Enter your new password"
        {...getFieldProps("newPassword")}
      />
      <InputField
        label="Confirm New Password"
        icon={<Lock className={inputFieldClasses.input_icon} />}
        placeholder="Confirm your new password"
        {...getFieldProps("confirmNewPassword")}
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
