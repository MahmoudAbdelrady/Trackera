import { AuthLayout } from "../../../components";
import { Input } from "antd";
import { Check, Lock } from "lucide-react";
import authClasses from "../scss/auth.module.css";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import classes from "./scss/change-password.module.css";

const ChangePassword = () => {
  const [isPasswordChanged, setIsPasswordChanged] = useState(true);
  const navigate = useNavigate();

  return isPasswordChanged ? (
    <AuthLayout
      title="Password Updated"
      description="Your password has been successfully updated"
      submitButtonText="Back to Sign In"
      onSubmit={(e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        navigate("/login");
      }}
    >
      <div className={classes.change_password_content}>
        <div className={classes.check_icon_container}>
          <Check className={classes.check_icon} />
        </div>
        <p className={classes.description}>
          You can now sign in with your new password
        </p>
      </div>
    </AuthLayout>
  ) : (
    <AuthLayout
      title="Change your password"
      description="Enter your new password"
      submitButtonText="Change Password"
      onSubmit={() => {}}
    >
      <div className={authClasses.input_group}>
        <div className={authClasses.input_label}>New Password</div>
        <Input.Password
          prefix={<Lock className={authClasses.input_icon} />}
          placeholder="Enter your new password"
          className={authClasses.input_field}
        />
      </div>
      <div className={authClasses.input_group}>
        <div className={authClasses.input_label}>Confirm New Password</div>
        <Input.Password
          prefix={<Lock className={authClasses.input_icon} />}
          placeholder="Confirm your new password"
          className={authClasses.input_field}
        />
      </div>
    </AuthLayout>
  );
};

export default ChangePassword;
