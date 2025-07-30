import { Input } from "antd";
import { AuthFooter, AuthLayout } from "../../../components";
import authClasses from "../scss/auth.module.css";
import { Lock, Mail, User } from "lucide-react";

const SignUp = () => {
  return (
    <AuthLayout
      title="Create an account"
      description="Sign up to get started with Trackera"
      submitButtonText="Create Account"
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
          footerText="Already have an account?"
          footerLink="/login"
          footerLinkText="Sign in"
        />
      }
    >
      <div className={authClasses.composite_input_group}>
        <div className={authClasses.input_group}>
          <div className={authClasses.input_label}>First Name</div>
          <Input
            prefix={<User className={authClasses.input_icon} />}
            placeholder="Enter your first name"
            className={authClasses.input_field}
          />
        </div>
        <div className={authClasses.input_group}>
          <div className={authClasses.input_label}>Last Name</div>
          <Input
            prefix={<User className={authClasses.input_icon} />}
            placeholder="Enter your last name"
            className={authClasses.input_field}
          />
        </div>
      </div>
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
      <div className={authClasses.input_group}>
        <div className={authClasses.input_label}>Confirm Password</div>
        <Input.Password
          prefix={<Lock className={authClasses.input_icon} />}
          placeholder="Confirm your password"
          className={authClasses.input_field}
        />
      </div>
    </AuthLayout>
  );
};

export default SignUp;
