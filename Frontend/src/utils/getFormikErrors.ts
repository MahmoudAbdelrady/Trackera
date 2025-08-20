const getFormikErrors = (errorsRes: any) => {
  return errorsRes.reduce((acc: Record<string, string>, err: any) => {
    acc[err.field] = err.message;
    return acc;
  }, {});
};

export default getFormikErrors;
