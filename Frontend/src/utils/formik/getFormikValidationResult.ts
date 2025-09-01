import type { FormikErrors, FormikTouched } from "formik";

type FormikWithValues<V> = {
  values: V;
  touched: FormikTouched<V>;
  errors: FormikErrors<V>;
};

const getFormikFieldError = <V, T extends keyof V>(
  formik: FormikWithValues<V>,
  field: T
) => {
  return validateFieldError(formik, field) ? formik.errors[field] : "";
};

const getFormikFieldStatus = <V, T extends keyof V>(
  formik: FormikWithValues<V>,
  field: T
) => {
  return validateFieldError(formik, field) ? "error" : "";
};

const validateFieldError = <V, T extends keyof V>(
  formik: FormikWithValues<V>,
  field: T
): boolean => {
  return !!(formik.touched[field] && formik.errors[field]);
};

export { getFormikFieldError, getFormikFieldStatus };
