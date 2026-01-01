import { AuthFooter, AuthForm, AuthResult, InputField, type AuthResultFields } from "../../../components";
import { Mail } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useFormik } from "formik";
import { emailSchema } from "../../../shared/yup-schemas";
import { showErrorToast } from "../../../utils/toast-handler/showToast";
import { authApis } from "../../../state/api";
import { getFormikFieldProps } from "../../../utils";
import { AuthLayout } from "../../../layouts";

interface ForgotPasswordFormFields {
  email: string;
}

const ForgotPassword = () => {
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [showAuthResult, setShowAuthResult] = useState<boolean>(false);
  const [passwordResetResult, setPasswordResetResult] = useState<AuthResultFields>({});
  const navigate = useNavigate();

  const forgotPasswordFormik = useFormik<ForgotPasswordFormFields>({
    initialValues: {
      email: "",
    },
    validationSchema: emailSchema,
    onSubmit: async (values: ForgotPasswordFormFields) => {
      setIsLoading(true);
      try {
        const result = await authApis.requestResetPassword(values.email);
        setPasswordResetResult({
          description: result,
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
          isSubmitBtnDisabled={!forgotPasswordFormik.isValid || !forgotPasswordFormik.dirty || isLoading}
          isSubmitBtnLoading={isLoading}
          footer={<AuthFooter footerText="Remember your password?" footerLink="/login" footerLinkText="Sign in" />}
        >
          <InputField
            label="Email"
            icon={<Mail />}
            placeholder="Enter your email"
            type="email"
            {...getFormikFieldProps(forgotPasswordFormik, "email", isLoading)}
          />
        </AuthForm>
      )}
    </AuthLayout>
  );
};

export default ForgotPassword;
