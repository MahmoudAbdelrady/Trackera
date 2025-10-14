import { object, ref, string } from "yup";

const updatePasswordSchema = object({
  currentPassword: string(),
  newPassword: string()
    .required("New password is required")
    .matches(
      new RegExp("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])\\S{8,}$"),
      "New password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    ),
  confirmNewPassword: string()
    .required("Confirm new password is required")
    .oneOf([ref("newPassword")], "Passwords must match"),
});

export default updatePasswordSchema;
