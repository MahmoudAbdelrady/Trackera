import dayjs from "dayjs";
import * as yup from "yup";

const workLogSearchFilterSchema = yup.object({
  operator: yup.string().nullable().optional(),
  value: yup.string().trim().nullable().optional(),
  secondValue: yup.string().trim().nullable().optional(),
});

const validateOperatorWithValues = function (this: yup.TestContext<any>, obj: any) {
  const { operator, value, secondValue } = obj || {};
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

  // if operator == "BETWEEN" but no secondValue
  if (operator === "BETWEEN" && !secondValue) {
    errors.push({
      path: `${this.path}.secondValue`,
      message: "Field is required",
    });
  }

  if (errors.length > 0) {
    return new yup.ValidationError(errors.map((err) => new yup.ValidationError(err.message, obj, err.path)));
  }
  return true;
};

const searchFilterSchema = yup.object({
  logName: yup.string().nullable().optional(),

  totalHours: workLogSearchFilterSchema.nullable().optional().test({
    name: "validate-totalHours-fields",
    test: validateOperatorWithValues,
  }),

  dateFrom: yup
    .string()
    .nullable()
    .optional()
    .test({
      name: "validate-dateFrom-after-dateTo",
      message: "Field must be before 'Date To'",
      test: function (dateFromValue) {
        const dateToValue = this.parent.dateTo;

        if (!dateFromValue || !dateToValue) {
          return true;
        }

        const from = dayjs(dateFromValue);
        const to = dayjs(dateToValue);

        if (!from.isValid() || !to.isValid()) {
          return true;
        }

        return !from.isAfter(to);
      },
    }),

  dateTo: yup.string().nullable().optional(),

  evaluation: yup.string().nullable().optional(),

  status: yup.string().nullable().optional(),
});

export default searchFilterSchema;
