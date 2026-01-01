import { AuthFooter, AuthForm, AuthResult, InputField } from "../../../components";
import authClasses from "../scss/auth.module.css";
import { Lock, Mail, User } from "lucide-react";
import { useFormik } from "formik";
import { signUpSchema } from "../../../shared/yup-schemas";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { showErrorToast } from "../../../utils/toast-handler/showToast";
import { getFormikErrors } from "../../../utils";
import { authApis } from "../../../state/api";
import { getFormikFieldProps } from "../../../utils";
import type { SignUpFormFields } from "../../../shared/types";
import { AuthLayout } from "../../../layouts";

const SignUp = () => {
  const [showAuthResult, setShowAuthResult] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const navigate = useNavigate();

  const getInitialValues = (): SignUpFormFields => ({
    firstname: "",
    lastname: "",
    email: "",
    password: "",
    confirmPassword: "",
  });

  const signUpFormik = useFormik<SignUpFormFields>({
    initialValues: getInitialValues(),
    validationSchema: signUpSchema,
    onSubmit: async (values: SignUpFormFields) => {
      setIsLoading(true);
      try {
        await authApis.signUp(values);
        setShowAuthResult(true);
      } catch (error: any) {
        if (error.response?.data?.message === "Validation Error" && error.response?.data?.data) {
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
          isSubmitBtnDisabled={!signUpFormik.isValid || !signUpFormik.dirty || isLoading}
          isSubmitBtnLoading={isLoading}
          footer={
            <AuthFooter
              hasOAuthBtns={true}
              isOAuthBtnsDisabled={isLoading}
              footerText="Already have an account?"
              footerLink="/login"
              footerLinkText="Sign in"
              footerAdditionalInfo={
                <p className={authClasses.privacy_policy_text}>
                  By signing up or signing in, you agree to our{" "}
                  <Link to="/privacy-policy" className={authClasses.privacy_policy_link}>
                    Privacy Policy
                  </Link>
                  .
                </p>
              }
            />
          }
        >
          <div className={authClasses.composite_input_group}>
            <InputField
              label="First Name"
              icon={<User />}
              placeholder="Enter your first name"
              type="text"
              {...getFormikFieldProps(signUpFormik, "firstname", isLoading)}
            />
            <InputField
              label="Last Name"
              icon={<User />}
              placeholder="Enter your last name"
              type="text"
              {...getFormikFieldProps(signUpFormik, "lastname", isLoading)}
            />
          </div>
          <InputField
            label="Email"
            icon={<Mail />}
            placeholder="Enter your email"
            type="email"
            {...getFormikFieldProps(signUpFormik, "email", isLoading)}
          />
          <InputField
            label="Password"
            icon={<Lock />}
            placeholder="Enter your password"
            type="password"
            {...getFormikFieldProps(signUpFormik, "password", isLoading)}
          />
          <InputField
            label="Confirm Password"
            icon={<Lock />}
            placeholder="Confirm your password"
            type="password"
            {...getFormikFieldProps(signUpFormik, "confirmPassword", isLoading)}
          />
        </AuthForm>
      )}
    </AuthLayout>
  );
};

export default SignUp;
