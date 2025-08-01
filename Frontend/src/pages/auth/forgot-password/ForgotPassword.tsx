import { AuthLayout } from "../../../components";
import { Input } from "antd";
import { Check, Mail } from "lucide-react";
import authClasses from "../scss/auth.module.css";
import classes from "./scss/forgot-password.module.css";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

const ForgotPassword = () => {
  const [passwordResetSent, setPasswordResetSent] = useState(false);
  const [userEmail, setUserEmail] = useState("asd123@mail.com");
  const navigate = useNavigate();

  return passwordResetSent ? (
    <AuthLayout
      title="Check your email"
      description="We have sent a password reset link to your email"
      submitButtonText="Back to Sign In"
      onSubmit={(e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        navigate("/login");
      }}
    >
      <div className={classes.forgot_password_content}>
        <div className={classes.check_icon_container}>
          <Check className={classes.check_icon} />
        </div>
        <p className={classes.description}>
          A password reset link has been sent to <strong>{userEmail}</strong>,
          Please check your inbox.
        </p>
      </div>
    </AuthLayout>
  ) : (
    <AuthLayout
      title="Reset your password"
      description="Enter your email to receive a password reset link"
      submitButtonText="Send Reset Link"
      onSubmit={() => {}}
    >
      <div className={authClasses.input_group}>
        <div className={authClasses.input_label}>Email</div>
        <Input
          prefix={<Mail className={authClasses.input_icon} />}
          placeholder="Enter your email"
          className={authClasses.input_field}
        />
      </div>
    </AuthLayout>
  );
};

export default ForgotPassword;
