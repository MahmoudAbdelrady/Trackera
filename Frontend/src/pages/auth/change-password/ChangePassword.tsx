import { AuthLayout } from "../../../components";
import { Input } from "antd";
import { Lock } from "lucide-react";
import authClasses from "../scss/auth.module.css";

const ChangePassword = () => {
  return (
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
