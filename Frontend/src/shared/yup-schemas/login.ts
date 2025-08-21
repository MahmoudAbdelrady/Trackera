import { object, string } from "yup";

const loginSchema = object({
  email: string()
    .required("Email is required")
    .matches(
      new RegExp(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}$"
      ),
      "Invalid email format"
    ),
  password: string().required("Password is required"),
});

export default loginSchema;
