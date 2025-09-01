import dayjs from "dayjs";
import { boolean, mixed, object, string } from "yup";

const ALLOWED_FILE_TYPES = [
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "text/csv",
  "application/csv",
  "application/vnd.google-apps.spreadsheet",
];

const addWorkLog = object({
  logName: string().trim(),
  logDate: mixed<dayjs.Dayjs>().required("Log date is required"),
  logFile: mixed<File>()
    .required("Log file is required")
    .test("fileSize", "File size must be less than 5MB", (value) => {
      return !value || value.size <= 5 * 1024 * 1024; // 5MB
    })
    .test("fileType", "Invalid file type", (value) => {
      return !value || ALLOWED_FILE_TYPES.includes(value.type);
    })
    .test("fileName", "File name is too long", (value) => {
      return !value || value.name.length <= 255;
    }),
  syncToJira: boolean(),
});

export default addWorkLog;
