import {
  AuthFooter,
  AuthForm,
  AuthLayout,
  AuthResult,
  InputField,
} from "../../../components";
import authClasses from "../scss/auth.module.css";
import inputFieldClasses from "../../../components/input-field/scss/input-field.module.css";
import { Lock, Mail, User } from "lucide-react";
import { useFormik } from "formik";
import { signUpSchema } from "../../../shared/yup-schemas";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import requestInstance from "../../../shared/api/request-instance";
import { showErrorToast } from "../../../utils/toast-handler/show-toast";
import { getFormikErrors } from "../../../utils";

interface SignUpFormFields {
  firstname: string;
  lastname: string;
  email: string;
  password: string;
  confirmPassword: string;
}

const SignUp = () => {
  const [showAuthResult, setShowAuthResult] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const signUpFormik = useFormik({
    initialValues: (
      Object.keys(signUpSchema.fields) as (keyof SignUpFormFields)[]
    ).reduce((acc, key) => {
      acc[key] = "";
      return acc;
    }, {} as SignUpFormFields),
    validationSchema: signUpSchema,
    onSubmit: async (values) => {
      setIsLoading(true);
      try {
        await requestInstance.post("/auth/signup", values);
        setShowAuthResult(true);
      } catch (error: any) {
        if (error.response?.data.message === "Validation Error") {
          signUpFormik.setErrors(getFormikErrors(error.response.data.data));
        } else {
          showErrorToast(error);
        }

        signUpFormik.setValues({
          ...signUpFormik.values,
          password: "",
          confirmPassword: "",
        });
      }
      setIsLoading(false);
    },
  });

  return (
    <AuthLayout>
      {showAuthResult ? (
        <AuthResult
          title="Account Created"
          description="Your account has been successfully created"
          message="Please check your email for verification instructions."
          buttonText="Go to Login"
          onClick={() => navigate("/login")}
        />
      ) : (
        <AuthForm
          title="Create an account"
          description="Sign up to get started with Trackera"
          submitButtonText="Create Account"
          onSubmit={signUpFormik.handleSubmit}
          isSubmitBtnDisabled={
            !signUpFormik.isValid || !signUpFormik.dirty || isLoading
          }
          isSubmitBtnLoading={isLoading}
          footer={
            <AuthFooter
              oAuthButtons={[
                {
                  label: "Continue with Google",
                  icon: (
                    <img src="./Assets/google_logo.webp" alt="Google Icon" />
                  ),
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
              disabled={isLoading}
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
              disabled={isLoading}
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
            disabled={isLoading}
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
            disabled={isLoading}
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
            disabled={isLoading}
            error={
              signUpFormik.touched.confirmPassword &&
              signUpFormik.errors.confirmPassword
                ? signUpFormik.errors.confirmPassword
                : undefined
            }
          />
        </AuthForm>
      )}
    </AuthLayout>
  );
};

export default SignUp;
