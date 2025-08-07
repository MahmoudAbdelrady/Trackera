import { AuthFooter, AuthLayout, InputField } from "../../../components";
import authClasses from "../scss/auth.module.css";
import inputFieldClasses from "../../../components/input-field/scss/input-field.module.css";
import { Lock, Mail, User } from "lucide-react";
import { useFormik } from "formik";
import { signUpSchema } from "../../../shared/yup-schemas";

const SignUp = () => {
  const signUpFormik = useFormik({
    initialValues: Object.keys(signUpSchema.fields).reduce((acc, key) => {
      acc[key] = "";
      return acc;
    }, {} as Record<string, string>),
    validationSchema: signUpSchema,
    onSubmit: (values) => {
      console.log("Form submitted with values:", values);
    },
  });

  return (
    <AuthLayout
      title="Create an account"
      description="Sign up to get started with Trackera"
      submitButtonText="Create Account"
      onSubmit={signUpFormik.handleSubmit}
      isSubmitBtnDisabled={!signUpFormik.isValid || signUpFormik.isSubmitting}
      isSubmitBtnLoading={signUpFormik.isSubmitting}
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
        <InputField
          label="First Name"
          icon={<User className={inputFieldClasses.input_icon} />}
          placeholder="Enter your first name"
          name="firstname"
          value={signUpFormik.values.firstname}
          onChange={signUpFormik.handleChange}
          onBlur={signUpFormik.handleBlur}
          type="text"
          error={
            signUpFormik.touched.firstname && signUpFormik.errors.firstname
              ? signUpFormik.errors.firstname
              : undefined
          }
        />
        <InputField
          label="Last Name"
          icon={<User className={inputFieldClasses.input_icon} />}
          placeholder="Enter your last name"
          name="lastname"
          value={signUpFormik.values.lastname}
          onChange={signUpFormik.handleChange}
          onBlur={signUpFormik.handleBlur}
          type="text"
          error={
            signUpFormik.touched.lastname && signUpFormik.errors.lastname
              ? signUpFormik.errors.lastname
              : undefined
          }
        />
      </div>
      <InputField
        label="Email"
        icon={<Mail className={inputFieldClasses.input_icon} />}
        placeholder="Enter your email"
        name="email"
        value={signUpFormik.values.email}
        onChange={signUpFormik.handleChange}
        onBlur={signUpFormik.handleBlur}
        type="email"
        error={
          signUpFormik.touched.email && signUpFormik.errors.email
            ? signUpFormik.errors.email
            : undefined
        }
      />
      <InputField
        label="Password"
        icon={<Lock className={inputFieldClasses.input_icon} />}
        placeholder="Enter your password"
        name="password"
        value={signUpFormik.values.password}
        onChange={signUpFormik.handleChange}
        onBlur={signUpFormik.handleBlur}
        type="password"
        error={
          signUpFormik.touched.password && signUpFormik.errors.password
            ? signUpFormik.errors.password
            : undefined
        }
      />
      <InputField
        label="Confirm Password"
        icon={<Lock className={inputFieldClasses.input_icon} />}
        placeholder="Confirm your password"
        name="confirmPassword"
        value={signUpFormik.values.confirmPassword}
        onChange={signUpFormik.handleChange}
        onBlur={signUpFormik.handleBlur}
        type="password"
        error={
          signUpFormik.touched.confirmPassword &&
          signUpFormik.errors.confirmPassword
            ? signUpFormik.errors.confirmPassword
            : undefined
        }
      />
    </AuthLayout>
  );
};

export default SignUp;
