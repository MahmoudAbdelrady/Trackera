import type dayjs from "dayjs";

const formatDate = (date: dayjs.Dayjs, format: string = "YYYY-MM-DD"): string => {
  return date.format(format);
};

export default formatDate;
