import { object, string } from "yup";

const editWorklogTask = object({
  taskName: string().trim().required("Task name is required"),
});

export default editWorklogTask;
