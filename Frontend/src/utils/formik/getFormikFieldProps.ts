import type { FormikProps } from "formik";

const getFormikFieldProps = <T extends Record<string, any>>(
  formik: FormikProps<T>,
  fieldName: keyof T,
  isLoading?: boolean
) => ({
  name: fieldName as string,
  value: formik.values[fieldName],
  onChange: formik.handleChange,
  onBlur: formik.handleBlur,
  disabled: isLoading,
  error: formik.touched[fieldName] && formik.errors[fieldName] ? (formik.errors[fieldName] as string) : undefined,
});

export default getFormikFieldProps;
