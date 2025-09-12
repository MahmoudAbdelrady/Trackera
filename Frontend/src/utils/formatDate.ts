import type dayjs from "dayjs";

const formatDate = (date?: dayjs.Dayjs, format: string = "YYYY-MM-DD"): string | null => {
  return date ? date.format(format) : null;
};

export default formatDate;
