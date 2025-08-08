import { AuthLayout } from "../../../components";
import { Input } from "antd";
import { Check, Mail } from "lucide-react";
import authClasses from "../scss/auth.module.css";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

const ForgotPassword = () => {
  const [passwordResetSent, setPasswordResetSent] = useState(false);
  const [userEmail, setUserEmail] = useState("asd123@mail.com");
  const navigate = useNavigate();

  return (
    <AuthLayout
      title="Reset your password"
      description="Enter your email to receive a password reset link"
      submitButtonText="Send Reset Link"
      onSubmit={() => {}}
      showResponseContent={passwordResetSent}
      responseContent={{
        title: "Check your email",
        description: "We have sent a password reset link to your email",
        message: `A password reset link has been sent to ${userEmail}, Please check your inbox.`,
        icon: <Check className={authClasses.response_icon} />,
        buttonText: "Back to Sign In",
        onClick: () => navigate("/login"),
      }}
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
