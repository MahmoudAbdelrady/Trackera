export const formateToastResponse = (
  resObj: any,
  isError: boolean = false
): string => {
  const resData =
    typeof resObj === "string" || !isError ? resObj : resObj?.response;
  return (
    resData?.data?.message ||
    resData ||
    "Something went wrong. Please try again."
  );
};
