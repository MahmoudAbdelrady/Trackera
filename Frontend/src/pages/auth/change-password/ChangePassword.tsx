import {
  AuthForm,
  AuthLayout,
  AuthResult,
  InputField,
  LoadingSpinner,
  type AuthResultFields,
} from "../../../components";
import { Lock } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import inputFieldClasses from "../../../components/input-field/scss/input-field.module.css";
import { useFormik } from "formik";
import { updatePasswordSchema } from "../../../shared/yup-schemas";
import type { ChangePasswordFormFields } from "../../../shared/types";
import { showErrorToast } from "../../../utils/toast-handler/showToast";
import { useAuthStore } from "../../../state/store";
import { authApis } from "../../../state/api";
import { getFormikFieldProps } from "../../../utils";

const ChangePassword = () => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const [isVerifying, setIsVerifying] = useState<boolean>(true);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [showAuthResult, setShowAuthResult] = useState<boolean>(false);
  const [passwordChangeResult, setPasswordChangeResult] = useState<AuthResultFields>({});
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const getInitialValues = (): ChangePasswordFormFields => ({
    newPassword: "",
    confirmNewPassword: "",
  });

  const changeAccountPassword = async (values: ChangePasswordFormFields) => {
    if (!token) {
      showErrorToast({ message: "Invalid or missing token" });
      return;
    }

    setIsLoading(true);
    try {
      const result = await authApis.changePassword(token, values);
      setPasswordChangeResult({
        description: result,
      });
      setShowAuthResult(true);
    } catch (error: any) {
      if (error.response?.status === 403 && error.response?.data?.message) {
        setPasswordChangeResult({
          description: error.response?.data?.message,
          isError: true,
        });
        setShowAuthResult(true);
      } else {
        showErrorToast(error);
        changePasswordFormik.resetForm();
      }
    }
    setIsLoading(false);
  };

  const validateToken = useCallback(
    async (token: string) => {
      try {
        await authApis.validateToken(token);
      } catch (error: any) {
        setPasswordChangeResult({
          description: error.response?.data?.message,
          isError: true,
        });
        setShowAuthResult(true);
      }
    },
    [token]
  );

  const formikConfig = useMemo(
    () => ({
      initialValues: getInitialValues(),
      validationSchema: updatePasswordSchema(),
      onSubmit: async (values: ChangePasswordFormFields) => {
        await changeAccountPassword(values);
      },
    }),
    [changeAccountPassword]
  );

  const changePasswordFormik = useFormik(formikConfig);

  useEffect(() => {
    if (!token) {
      setPasswordChangeResult({
        description: "Url is expired or invalid",
        isError: true,
      });
      setShowAuthResult(true);
    } else {
      validateToken(token);
    }
    setIsVerifying(false);
  }, [token, validateToken]);

  return isVerifying ? (
    <LoadingSpinner />
  ) : (
    <AuthLayout>
      {showAuthResult ? (
        <AuthResult
          title="Password Change"
          description={
            passwordChangeResult.isError ? "Error occurred while changing password" : passwordChangeResult.description!
          }
          message={
            passwordChangeResult.isError
              ? passwordChangeResult.description!
              : "You can now sign in with your new password"
          }
          buttonText={`Back to ${isAuthenticated ? "Home" : "Sign In"}`}
          isError={passwordChangeResult.isError}
          onClick={() => navigate(`${isAuthenticated ? "/" : "/login"}`)}
        />
      ) : (
        <AuthForm
          title="Change your password"
          description="Enter your new password"
          submitButtonText="Change Password"
          onSubmit={changePasswordFormik.handleSubmit}
          isSubmitBtnDisabled={!changePasswordFormik.isValid || !changePasswordFormik.dirty || isLoading}
          isSubmitBtnLoading={isLoading}
        >
          <InputField
            label="New Password"
            icon={<Lock className={inputFieldClasses.input_icon} />}
            placeholder="Enter your new password"
            type="password"
            {...getFormikFieldProps(changePasswordFormik, "newPassword", isLoading)}
          />
          <InputField
            label="Confirm New Password"
            icon={<Lock className={inputFieldClasses.input_icon} />}
            placeholder="Confirm your new password"
            type="password"
            {...getFormikFieldProps(changePasswordFormik, "confirmNewPassword", isLoading)}
          />
        </AuthForm>
      )}
    </AuthLayout>
  );
};

export default ChangePassword;
