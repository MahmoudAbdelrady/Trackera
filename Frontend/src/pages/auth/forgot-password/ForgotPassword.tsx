import {
  AuthFooter,
  AuthForm,
  AuthLayout,
  AuthResult,
  InputField,
} from "../../../components";
import { Mail } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import inputFieldClasses from "../../../components/input-field/scss/input-field.module.css";
import { useFormik } from "formik";
import { emailSchema } from "../../../shared/yup-schemas";
import { showErrorToast } from "../../../utils/toast-handler/showToast";
import requestInstance from "../../../shared/axios/request-instance";
import type { AuthResultFields } from "../../../shared/types";

interface ForgotPasswordFormFields {
  email: string;
}

const ForgotPassword = () => {
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [showAuthResult, setShowAuthResult] = useState<boolean>(false);
  const [passwordResetResult, setPasswordResetResult] =
    useState<AuthResultFields>({});
  const navigate = useNavigate();

  const forgotPasswordFormik = useFormik({
    initialValues: (
      Object.keys(emailSchema.fields) as (keyof ForgotPasswordFormFields)[]
    ).reduce((acc, key) => {
      acc[key] = "";
      return acc;
    }, {} as ForgotPasswordFormFields),
    validationSchema: emailSchema,
    onSubmit: async (values) => {
      setIsLoading(true);
      try {
        const response = await requestInstance.post(
          "/auth/send-reset-password",
          values
        );
        setPasswordResetResult({
          description: response.data,
        });
        setShowAuthResult(true);
      } catch (error) {
        showErrorToast(error);
        forgotPasswordFormik.resetForm();
      }
      setIsLoading(false);
    },
  });

  return (
    <AuthLayout>
      {showAuthResult ? (
        <AuthResult
          title="Password Reset"
          message={passwordResetResult.description!}
          buttonText="Back to Sign In"
          onClick={() => navigate("/login")}
        />
      ) : (
        <AuthForm
          title="Reset your password"
          description="Enter your email to receive a password reset link"
          submitButtonText="Send Reset Link"
          onSubmit={forgotPasswordFormik.handleSubmit}
          isSubmitBtnDisabled={
            !forgotPasswordFormik.isValid ||
            !forgotPasswordFormik.dirty ||
            isLoading
          }
          isSubmitBtnLoading={isLoading}
          footer={
            <AuthFooter
              footerText="Remember your password?"
              footerLink="/login"
              footerLinkText="Sign in"
            />
          }
        >
          <InputField
            label="Email"
            icon={<Mail className={inputFieldClasses.input_icon} />}
            placeholder="Enter your email"
            name="email"
            value={forgotPasswordFormik.values.email}
            onChange={forgotPasswordFormik.handleChange}
            onBlur={forgotPasswordFormik.handleBlur}
            type="email"
            disabled={isLoading}
            error={
              forgotPasswordFormik.touched.email &&
              forgotPasswordFormik.errors.email
                ? forgotPasswordFormik.errors.email
                : undefined
            }
          />
        </AuthForm>
      )}
    </AuthLayout>
  );
};

export default ForgotPassword;
