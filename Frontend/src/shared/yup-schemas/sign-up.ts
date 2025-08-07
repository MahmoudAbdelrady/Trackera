import { object, ref, string } from "yup";

const signUpSchema = object({
  firstname: string()
    .required("First name is required")
    .matches(
      new RegExp("^[A-Za-z]{2,50}$"),
      "First name must be between 2 and 50 characters long and contain only letters"
    ),
  lastname: string()
    .required("Last name is required")
    .matches(
      new RegExp("^(?=.{2,120}$)[A-Za-z]+(?: [A-Za-z]+)*$"),
      "Last name must be between 2 and 120 characters long and contain only letters and spaces"
    ),
  email: string().required("Email is required").email("Invalid email format"),
  password: string()
    .required("Password is required")
    .matches(
      new RegExp(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])\\S{8,}$"
      ),
      "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    ),
  confirmPassword: string()
    .required("Confirm password is required")
    .oneOf([ref("password")], "Passwords must match"),
});

export default signUpSchema;
