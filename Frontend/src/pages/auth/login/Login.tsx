import { AuthFooter, AuthForm, InputField } from "../../../components";
import { Lock, Mail } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { useFormik } from "formik";
import { loginSchema } from "../../../shared/yup-schemas";
import { useState } from "react";
import { getFormikErrors } from "../../../utils";
import { showErrorToast } from "../../../utils/toast-handler/showToast";
import inputFieldClasses from "../../../components/input-field/scss/input-field.module.css";
import classes from "./scss/login.module.css";
import authClasses from "../scss/auth.module.css";
import { useAuthStore } from "../../../state/store";
import { authApis } from "../../../state/api";
import { getFormikFieldProps } from "../../../utils";
import type { LoginFormFields } from "../../../shared/types";
import { AuthLayout } from "../../../layouts";

const Login = () => {
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const authStore = useAuthStore();
  const navigate = useNavigate();

  const getInitialValues = (): LoginFormFields => ({
    email: "",
    password: "",
  });

  const loginFormik = useFormik<LoginFormFields>({
    initialValues: getInitialValues(),
    validationSchema: loginSchema,
    onSubmit: async (values: LoginFormFields) => {
      setIsLoading(true);
      try {
        await authApis.login(values);
        authStore.setAuthenticated(true);
        navigate("/");
      } catch (error: any) {
        if (error.response?.data?.message === "Validation Error" && error.response?.data?.data) {
          loginFormik.setErrors(getFormikErrors(error.response.data.data));
        } else {
          showErrorToast(error);
        }

        loginFormik.setFieldValue("password", "");
      }
      setIsLoading(false);
    },
  });

  return (
    <AuthLayout>
      <AuthForm
        title="Sign in to Trackera"
        description="Enter your credentials to access your account"
        submitButtonText="Login"
        onSubmit={loginFormik.handleSubmit}
        isSubmitBtnDisabled={!loginFormik.isValid || !loginFormik.dirty || isLoading}
        isSubmitBtnLoading={isLoading}
        footer={
          <AuthFooter
            hasOAuthBtns={true}
            isOAuthBtnsDisabled={isLoading}
            footerText="Don't have an account?"
            footerLink="/sign-up"
            footerLinkText="Sign up"
            footerAdditionalInfo={
              <p className={authClasses.privacy_policy_text}>
                By signing up or signing in, you agree to our{" "}
                <Link to="/privacy-policy" className={authClasses.privacy_policy_link}>
                  Privacy Policy
                </Link>
                .
              </p>
            }
          />
        }
      >
        <InputField
          label="Email"
          icon={<Mail className={inputFieldClasses.input_icon} />}
          placeholder="Enter your email"
          type="email"
          {...getFormikFieldProps(loginFormik, "email", isLoading)}
        />
        <InputField
          label="Password"
          icon={<Lock className={inputFieldClasses.input_icon} />}
          placeholder="Enter your password"
          type="password"
          {...getFormikFieldProps(loginFormik, "password", isLoading)}
        />
        <div className={classes.forget_password_box}>
          <Link to="/forgot-password" className={classes.forget_password_link}>
            Forgot Password?
          </Link>
        </div>
      </AuthForm>
    </AuthLayout>
  );
};

export default Login;
