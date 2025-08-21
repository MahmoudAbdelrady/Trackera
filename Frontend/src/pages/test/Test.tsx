import { Input } from "antd";
import { AuthFooter, AuthLayout } from "../../components";
import classes from "./scss/test.module.css";
import { Lock, Mail } from "lucide-react";
import { Link } from "react-router-dom";

const Test = () => {
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
          footerLink="/signup"
          footerLinkText="Sign up"
        />
      }
    >
      <div className={classes.input_group}>
        <div className={classes.input_label}>Email</div>
        <Input
          prefix={<Mail className={classes.input_icon} />}
          placeholder="Enter your email"
          className={classes.input_field}
        />
      </div>
      <div className={classes.input_group}>
        <div className={classes.input_label}>Password</div>
        <Input.Password
          prefix={<Lock className={classes.input_icon} />}
          placeholder="Enter your password"
          className={classes.input_field}
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

export default Test;
