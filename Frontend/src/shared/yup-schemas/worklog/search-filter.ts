import dayjs from "dayjs";
import * as yup from "yup";

const workLogSearchFilterSchema = yup.object({
  fieldName: yup.string().required(),
  operator: yup.string().nullable().optional(),
  value: yup.string().trim().nullable().optional(),
  extraValue: yup.string().trim().nullable().optional(),
});

const validateOperatorWithValues = function (this: yup.TestContext<any>, obj: any) {
  const { operator, value, extraValue } = obj || {};
  const errors: any[] = [];

  // if value is provided but no operator
  if (value && !operator) {
    errors.push({
      path: `${this.path}.operator`,
      message: "Operator is required",
    });
  }

  // if operator is provided but no value
  if (operator && !value) {
    errors.push({
      path: `${this.path}.value`,
      message: "Field is required",
    });
  }

  // if operator == "BETWEEN" but no extraValue
  if (operator === "BETWEEN" && !extraValue) {
    errors.push({
      path: `${this.path}.extraValue`,
      message: "Field is required",
    });
  }

  if (errors.length > 0) {
    return new yup.ValidationError(errors.map((err) => new yup.ValidationError(err.message, obj, err.path)));
  }
  return true;
};

const searchFilterSchema = yup.object({
  logName: workLogSearchFilterSchema,

  logHours: workLogSearchFilterSchema.test({
    name: "validate-logHours-fields",
    test: validateOperatorWithValues,
  }),

  dateFrom: workLogSearchFilterSchema.test({
    name: "validate-dateFrom-after-dateTo",
    test: function (value) {
      const { value: dateFromValue } = value || {};
      const dateToValue = this.parent.dateTo?.value;

      if (!dateFromValue || !dateToValue) {
        return true;
      }

      const from = dayjs(dateFromValue);
      const to = dayjs(dateToValue);

      if (!from.isValid() || !to.isValid()) {
        return true;
      }

      if (from.isAfter(to)) {
        return this.createError({ path: `${this.path}.value`, message: "Field must be before 'Date To'" });
      }
      return true;
    },
  }),

  dateTo: workLogSearchFilterSchema,

  evaluation: workLogSearchFilterSchema,

  status: workLogSearchFilterSchema,
});

export default searchFilterSchema;
