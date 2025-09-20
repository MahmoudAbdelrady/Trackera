import { getIn, type FormikErrors, type FormikTouched } from "formik";

type FormikWithValues<V> = {
  values: V;
  touched: FormikTouched<V>;
  errors: FormikErrors<V>;
  submitCount: number;
};

const getFormikFieldError = <V>(formik: FormikWithValues<V>, field: string) => {
  return validateFieldError(formik, field) ? getIn(formik.errors, field) : "";
};

const getFormikFieldStatus = <V>(formik: FormikWithValues<V>, field: string) => {
  return validateFieldError(formik, field) ? "error" : "";
};

const validateFieldError = <V>(formik: FormikWithValues<V>, field: string): boolean => {
  return !!((getIn(formik.touched, field) || formik.submitCount > 0) && getIn(formik.errors, field));
};

export { getFormikFieldError, getFormikFieldStatus };
