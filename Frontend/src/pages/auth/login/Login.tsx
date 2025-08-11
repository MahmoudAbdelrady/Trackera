import {
  AuthFooter,
  AuthForm,
  AuthLayout,
  InputField,
} from "../../../components";
import { Lock, Mail } from "lucide-react";
import { Link } from "react-router-dom";
import { useFormik } from "formik";
import { loginSchema } from "../../../shared/yup-schemas";
import { useState } from "react";
import { getFormikErrors } from "../../../utils";
import { showErrorToast } from "../../../utils/toast-handler/show-toast";
import inputFieldClasses from "../../../components/input-field/scss/input-field.module.css";
import classes from "./scss/login.module.css";

interface LoginFormFields {
  email: string;
  password: string;
}

const Login = () => {
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const loginFormik = useFormik({
    initialValues: (
      Object.keys(loginSchema.fields) as (keyof LoginFormFields)[]
    ).reduce((acc, key) => {
      acc[key] = "";
      return acc;
    }, {} as LoginFormFields),
    validationSchema: loginSchema,
    onSubmit: async (values) => {
      setIsLoading(true);
      try {
        console.log("Login values:", values);
      } catch (error: any) {
        if (error.response?.data.message === "Validation Error") {
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
        onSubmit={() => {}}
        isSubmitBtnDisabled={
          !loginFormik.isValid || !loginFormik.dirty || isLoading
        }
        isSubmitBtnLoading={isLoading}
        footer={
          <AuthFooter
            oAuthButtons={[
              {
                label: "Continue with Google",
                icon: <img src="./Assets/google_logo.webp" alt="Google Icon" />,
                onClick: () => {},
              },
            ]}
            footerText="Don't have an account?"
            footerLink="/sign-up"
            footerLinkText="Sign up"
          />
        }
      >
        <InputField
          label="Email"
          icon={<Mail className={inputFieldClasses.input_icon} />}
          placeholder="Enter your email"
          name="email"
          value={loginFormik.values.email}
          onChange={loginFormik.handleChange}
          onBlur={loginFormik.handleBlur}
          type="email"
          disabled={isLoading}
          error={
            loginFormik.touched.email && loginFormik.errors.email
              ? loginFormik.errors.email
              : undefined
          }
        />
        <InputField
          label="Password"
          icon={<Lock className={inputFieldClasses.input_icon} />}
          placeholder="Enter your password"
          name="password"
          value={loginFormik.values.password}
          onChange={loginFormik.handleChange}
          onBlur={loginFormik.handleBlur}
          type="password"
          disabled={isLoading}
          error={
            loginFormik.touched.password && loginFormik.errors.password
              ? loginFormik.errors.password
              : undefined
          }
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
