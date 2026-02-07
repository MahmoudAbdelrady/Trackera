import type dayjs from "dayjs";
import { mixed, object, string } from "yup";

const editWorklogEntry = object({
  taskName: string().trim().required("Task name is required"),
  fromTime: mixed<dayjs.Dayjs>().required("From time is required"),
  toTime: mixed<dayjs.Dayjs>()
    .required("To time is required")
    .test("is-after-fromTime", "To time must be after 'From time' field", function (value) {
      const { fromTime } = this.parent;
      return !fromTime || !value || value.isAfter(fromTime);
    }),
  duration: string().trim(),
  description: string().trim().required("Description is required"),
});

export default editWorklogEntry;
