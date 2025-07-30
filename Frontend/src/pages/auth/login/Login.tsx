import { Input } from "antd";
import { AuthFooter, AuthLayout } from "../../../components";
import authClasses from "../scss/auth.module.css";
import classes from "./scss/login.module.css";
import { Lock, Mail } from "lucide-react";
import { Link } from "react-router-dom";

const Login = () => {
  return (
    <AuthLayout
      title="Sign in to Trackera"
      description="Enter your credentials to access your account"
      submitButtonText="Login"
      onSubmit={() => {}}
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
      <div className={authClasses.input_group}>
        <div className={authClasses.input_label}>Email</div>
        <Input
          prefix={<Mail className={authClasses.input_icon} />}
          placeholder="Enter your email"
          className={authClasses.input_field}
        />
      </div>
      <div className={authClasses.input_group}>
        <div className={authClasses.input_label}>Password</div>
        <Input.Password
          prefix={<Lock className={authClasses.input_icon} />}
          placeholder="Enter your password"
          className={authClasses.input_field}
        />
      </div>
      <div className={classes.forget_password_box}>
        <Link to="/forgot-password" className={classes.forget_password_link}>
          Forgot Password?
        </Link>
      </div>
    </AuthLayout>
  );
};

export default Login;
