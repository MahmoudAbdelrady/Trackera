import {
  AuthForm,
  AuthLayout,
  AuthResult,
  InputField,
  LoadingSpinner,
} from "../../../components";
import { Lock } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import inputFieldClasses from "../../../components/input-field/scss/input-field.module.css";
import { useFormik } from "formik";
import { resetPasswordSchema } from "../../../shared/yup-schemas";
import requestInstance from "../../../shared/axios/request-instance";
import type { AuthResultFields } from "../../../shared/types";
import { showErrorToast } from "../../../utils/toast-handler/showToast";

interface ChangePasswordFormFields {
  newPassword: string;
  confirmNewPassword: string;
}

const ChangePassword = () => {
  const [isVerifying, setIsVerifying] = useState<boolean>(true);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [showAuthResult, setShowAuthResult] = useState<boolean>(false);
  const [passwordChangeResult, setPasswordChangeResult] =
    useState<AuthResultFields>({});
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const changePasswordFormik = useFormik({
    initialValues: (
      Object.keys(
        resetPasswordSchema.fields
      ) as (keyof ChangePasswordFormFields)[]
    ).reduce((acc, key) => {
      acc[key] = "";
      return acc;
    }, {} as ChangePasswordFormFields),
    validationSchema: resetPasswordSchema,
    onSubmit: async (values) => {
      setIsLoading(true);
      try {
        const response = await requestInstance.post(
          `/auth/change-password?token=${token}`,
          {
            ...values,
            token,
          }
        );
        setPasswordChangeResult({
          description: response.data.message,
        });
        setShowAuthResult(true);
      } catch (error: any) {
        console.log("Error:", error);
        if (error.response?.status === 403) {
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
    },
  });

  useEffect(() => {
    if (!token) {
      setPasswordChangeResult({
        description: "Url is expired or invalid",
        isError: true,
      });
      setShowAuthResult(true);
    } else {
      const validateToken = async () => {
        try {
          await requestInstance.post(`/auth/validate-token?token=${token}`);
        } catch (error: any) {
          setPasswordChangeResult({
            description: error.response?.data?.message,
            isError: true,
          });
          setShowAuthResult(true);
        }
      };

      validateToken();
    }
    setIsVerifying(false);
  }, [token]);

  return isVerifying ? (
    <LoadingSpinner />
  ) : (
    <AuthLayout>
      {showAuthResult ? (
        <AuthResult
          title="Password Change"
          description={
            passwordChangeResult.isError
              ? "Error occurred while changing password"
              : passwordChangeResult.description!
          }
          message={
            passwordChangeResult.isError
              ? passwordChangeResult.description!
              : "You can now sign in with your new password"
          }
          buttonText="Back to Sign In"
          isError={passwordChangeResult.isError}
          onClick={() => navigate("/login")}
        />
      ) : (
        <AuthForm
          title="Change your password"
          description="Enter your new password"
          submitButtonText="Change Password"
          onSubmit={changePasswordFormik.handleSubmit}
          isSubmitBtnDisabled={
            !changePasswordFormik.isValid ||
            !changePasswordFormik.dirty ||
            isLoading
          }
          isSubmitBtnLoading={isLoading}
        >
          <InputField
            label="New Password"
            icon={<Lock className={inputFieldClasses.input_icon} />}
            placeholder="Enter your new password"
            name="newPassword"
            value={changePasswordFormik.values.newPassword}
            onChange={changePasswordFormik.handleChange}
            onBlur={changePasswordFormik.handleBlur}
            type="password"
            disabled={isLoading}
            error={
              changePasswordFormik.touched.newPassword &&
              changePasswordFormik.errors.newPassword
                ? changePasswordFormik.errors.newPassword
                : undefined
            }
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
            disabled={isLoading}
            error={
              changePasswordFormik.touched.confirmNewPassword &&
              changePasswordFormik.errors.confirmNewPassword
                ? changePasswordFormik.errors.confirmNewPassword
                : undefined
            }
          />
        </AuthForm>
      )}
    </AuthLayout>
  );
};

export default ChangePassword;
