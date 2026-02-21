import type dayjs from "dayjs";
import { mixed, object, string } from "yup";

const editWorklogEntry = object({
  taskName: string().trim().required("Task name is required"),
  startTime: mixed<dayjs.Dayjs>().required("Start time is required"),
  endTime: mixed<dayjs.Dayjs>()
    .required("End time is required")
    .test("is-after-startTime", "End time must be after 'Start time' field", function (value) {
      const { startTime } = this.parent;
      return !startTime || !value || value.isAfter(startTime);
    }),
  duration: string().trim(),
  description: string().trim().required("Description is required"),
});

export default editWorklogEntry;
