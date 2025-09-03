import dayjs from "dayjs";
import { boolean, mixed, object, string } from "yup";

const ALLOWED_FILE_TYPES = [
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "text/csv",
  "application/csv",
  "application/vnd.google-apps.spreadsheet",
];

const manageWorkLog = object({
  mode: string().oneOf(["add", "edit"]).required(),
  logName: string().trim(),
  logDate: mixed<dayjs.Dayjs>().required("Log date is required"),
  reEvaluate: boolean().default(false),
  logFile: mixed<File>().when(["mode", "reEvaluate"], {
    is: (mode: string, reEvaluate: boolean) => mode === "add" || (mode === "edit" && reEvaluate === true),
    then: (schema) =>
      schema
        .required("Log file is required")
        .test("fileSize", "File size must be less than 5MB", (value?: File) => {
          return !value || value.size <= 5 * 1024 * 1024;
        })
        .test("fileType", "Invalid file type", (value?: File) => {
          return !value || ALLOWED_FILE_TYPES.includes(value.type);
        }),
    otherwise: (schema) => schema.nullable(),
  }),
  syncToJira: boolean().default(false),
});

export default manageWorkLog;
