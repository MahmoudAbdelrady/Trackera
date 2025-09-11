import * as yup from "yup";

const workLogSearchFilterSchema = yup.object({
  fieldName: yup.string().required(),
  operator: yup.string().nullable().optional().trim(),
  value: yup.string().nullable().optional().trim(),
  extraValue: yup.string().nullable().optional().trim(),
});

const searchFilterSchema = yup.object({
  logName: workLogSearchFilterSchema.shape({
    operator: yup.string().nullable().optional(),
  }),

  logHours: workLogSearchFilterSchema.shape({
    operator: yup
      .string()
      .nullable()
      .when("value", {
        is: (val: string | null) => val != null && val !== "",
        then: (schema) => schema.required("Operator is required when value is provided"),
        otherwise: (schema) => schema.optional(),
      }),
    extraValue: yup
      .string()
      .nullable()
      .when("operator", {
        is: "BETWEEN",
        then: (schema) => schema.required("Second field is required for BETWEEN operator"),
        otherwise: (schema) => schema.optional(),
      }),
  }),

  dateFrom: workLogSearchFilterSchema.shape({
    operator: yup.string().nullable().optional(),
  }),

  dateTo: workLogSearchFilterSchema.shape({
    operator: yup.string().nullable().optional(),
  }),

  evaluation: workLogSearchFilterSchema.shape({
    operator: yup.string().nullable().optional(),
  }),

  status: workLogSearchFilterSchema.shape({
    operator: yup.string().nullable().optional(),
  }),
});

export default searchFilterSchema;
