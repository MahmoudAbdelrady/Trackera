import { Funnel, Search, RotateCcw, Info } from "lucide-react";
import { Button, DatePicker, Form, Input, InputNumber, Radio, Select, Tooltip } from "antd";

import classes from "./scss/search-filter.module.css";
import CollapsibleSection from "../../collapsible-section/CollapsibleSection";
import {
  FILTER_OPERATORS_METADATA,
  OPERATORS,
  statusMetadata,
  WorkLogEvaluation,
  worklogEvaluationMetadata,
  WorkLogStatus,
  type WorkLogSearchFilter,
  type WorkLogsFilterProps,
} from "../../../shared/types";
import { useFormik } from "formik";
import { searchFilterSchema } from "../../../shared/yup-schemas";
import { formatDate, getFormikFieldError, getFormikFieldStatus, isNullOrEmpty } from "../../../utils";
import { useCallback, useState } from "react";
import type dayjs from "dayjs";

const FIELD_NAMES = {
  LOG_NAME: "logName",
  TOTAL_HOURS: "totalHours",
  DATE_FROM: "dateFrom",
  DATE_TO: "dateTo",
  EVALUATION: "evaluation",
  STATUS: "status",
} as const;

interface SearchFilterValues {
  [FIELD_NAMES.LOG_NAME]: string;
  [FIELD_NAMES.TOTAL_HOURS]: WorkLogSearchFilter;
  [FIELD_NAMES.DATE_FROM]: dayjs.Dayjs | null;
  [FIELD_NAMES.DATE_TO]: dayjs.Dayjs | null;
  [FIELD_NAMES.EVALUATION]: string | null;
  [FIELD_NAMES.STATUS]: string | null;
}

const fieldConfig: SearchFilterValues = {
  [FIELD_NAMES.LOG_NAME]: "",
  [FIELD_NAMES.TOTAL_HOURS]: { operator: null, value: null, secondValue: null },
  [FIELD_NAMES.DATE_FROM]: null,
  [FIELD_NAMES.DATE_TO]: null,
  [FIELD_NAMES.EVALUATION]: null,
  [FIELD_NAMES.STATUS]: null,
};

type CriteriaType = typeof FIELD_NAMES.TOTAL_HOURS | typeof FIELD_NAMES.EVALUATION;

const criteriaTypeItems = [
  { label: "Total Hours", value: FIELD_NAMES.TOTAL_HOURS },
  { label: "Evaluation", value: FIELD_NAMES.EVALUATION },
];

const EVALUATION_FILTER_OPTIONS = [
  { label: worklogEvaluationMetadata[WorkLogEvaluation.EXCELLENT].label, value: WorkLogEvaluation.EXCELLENT },
  { label: worklogEvaluationMetadata[WorkLogEvaluation.GOOD].label, value: WorkLogEvaluation.GOOD },
  { label: worklogEvaluationMetadata[WorkLogEvaluation.MODERATE].label, value: WorkLogEvaluation.MODERATE },
  { label: worklogEvaluationMetadata[WorkLogEvaluation.POOR].label, value: WorkLogEvaluation.POOR },
];

const STATUS_FILTER_OPTIONS = [
  { label: statusMetadata[WorkLogStatus.SYNCED].label, value: WorkLogStatus.SYNCED },
  { label: statusMetadata[WorkLogStatus.PARTIALLY].label, value: WorkLogStatus.PARTIALLY },
  { label: statusMetadata[WorkLogStatus.NOT_SYNCED].label, value: WorkLogStatus.NOT_SYNCED },
];

const SearchFilter = (props: WorkLogsFilterProps) => {
  const { jiraLinked, setFilters } = props;
  const [criteriaType, setCriteriaType] = useState<CriteriaType>(FIELD_NAMES.TOTAL_HOURS);

  const searchFormik = useFormik({
    initialValues: fieldConfig,
    validationSchema: searchFilterSchema,
    onSubmit: (values) => {
      setFilters(buildSearchFilters(values));
    },
  });

  const buildSearchFilters = useCallback((values: SearchFilterValues) => {
    const filters: Record<string, any> = {};

    Object.entries(values).forEach(([fieldName, fieldValue]) => {
      if (isNullOrEmpty(fieldValue)) {
        return;
      }

      if (fieldValue && typeof fieldValue === "object" && "operator" in fieldValue && "value" in fieldValue) {
        const { operator, value, secondValue } = fieldValue;
        if (!isNullOrEmpty(value)) {
          filters[fieldName] = {
            operator,
            value,
            ...(!isNullOrEmpty(secondValue) ? { secondValue } : {}),
          };
        }
        return;
      }

      filters[fieldName] = fieldValue;
    });

    if (filters[FIELD_NAMES.DATE_FROM]) filters[FIELD_NAMES.DATE_FROM] = formatDate(filters[FIELD_NAMES.DATE_FROM]);
    if (filters[FIELD_NAMES.DATE_TO]) filters[FIELD_NAMES.DATE_TO] = formatDate(filters[FIELD_NAMES.DATE_TO]);

    return filters;
  }, []);

  const handleCriteriaTypeChange = useCallback((value: CriteriaType) => {
    setCriteriaType(value);
    if (value === FIELD_NAMES.TOTAL_HOURS) {
      searchFormik.setFieldValue(FIELD_NAMES.EVALUATION, fieldConfig[FIELD_NAMES.EVALUATION]);
    } else {
      searchFormik.setFieldValue(FIELD_NAMES.TOTAL_HOURS, fieldConfig[FIELD_NAMES.TOTAL_HOURS]);
    }
  }, []);

  const handleOperatorChange = useCallback((value: string) => {
    searchFormik.setFieldValue(`${FIELD_NAMES.TOTAL_HOURS}.operator`, value);
    if (value !== OPERATORS.BETWEEN) {
      searchFormik.setFieldValue(`${FIELD_NAMES.TOTAL_HOURS}.secondValue`, null);
    }
  }, []);

  return (
    <div className={classes.search_filters_container}>
      <CollapsibleSection title="Filters" icon={<Funnel />}>
        <form onSubmit={searchFormik.handleSubmit}>
          <div className={classes.search_filters_fields_grid}>
            <div className={classes.search_filter_input}>
              <span className={classes.filter_label}>Log Name:</span>
              <div className={classes.filter_input_box}>
                <Input
                  placeholder="Enter Log Name"
                  name={FIELD_NAMES.LOG_NAME}
                  value={searchFormik.values[FIELD_NAMES.LOG_NAME]}
                  onChange={(e) => searchFormik.setFieldValue(FIELD_NAMES.LOG_NAME, e.target.value)}
                  onBlur={() => searchFormik.setFieldTouched(FIELD_NAMES.LOG_NAME, true)}
                />
              </div>
            </div>

            <div className={classes.search_filter_input}>
              <span className={classes.filter_label}>Date From:</span>
              <div className={classes.filter_input_box}>
                <Form.Item
                  className={classes.filter_form_item}
                  validateStatus={getFormikFieldStatus(searchFormik, FIELD_NAMES.DATE_FROM)}
                  help={getFormikFieldError(searchFormik, FIELD_NAMES.DATE_FROM) as string}
                >
                  <DatePicker
                    placeholder="Select Date"
                    style={{ width: "100%" }}
                    name={FIELD_NAMES.DATE_FROM}
                    value={searchFormik.values[FIELD_NAMES.DATE_FROM]}
                    onChange={(value) => searchFormik.setFieldValue(FIELD_NAMES.DATE_FROM, value)}
                    onBlur={() => searchFormik.setFieldTouched(FIELD_NAMES.DATE_FROM, true)}
                  />
                </Form.Item>
              </div>
            </div>

            <div className={classes.search_filter_input}>
              <span className={classes.filter_label}>Date To:</span>
              <div className={classes.filter_input_box}>
                <DatePicker
                  placeholder="Select Date"
                  style={{ width: "100%" }}
                  name={FIELD_NAMES.DATE_TO}
                  value={searchFormik.values[FIELD_NAMES.DATE_TO]}
                  onChange={(value) => searchFormik.setFieldValue(FIELD_NAMES.DATE_TO, value)}
                  onBlur={() => searchFormik.setFieldTouched(FIELD_NAMES.DATE_TO, true)}
                />
              </div>
            </div>

            <div className={classes.search_filter_input}>
              <span className={classes.filter_label}>Status:</span>
              <div className={classes.filter_input_box}>
                <Select
                  className={classes.filter_operator}
                  options={STATUS_FILTER_OPTIONS}
                  placeholder="Status"
                  allowClear
                  value={searchFormik.values[FIELD_NAMES.STATUS]}
                  onChange={(value) => searchFormik.setFieldValue(FIELD_NAMES.STATUS, value)}
                  onBlur={() => searchFormik.setFieldTouched(FIELD_NAMES.STATUS, true)}
                  disabled={!jiraLinked}
                />
                {!jiraLinked && (
                  <Tooltip title="Link your Jira account in settings to enable this option.">
                    <Info size={16} color="#dc2626" style={{ margin: "auto" }} />
                  </Tooltip>
                )}
              </div>
            </div>

            <div className={classes.search_filter_input}>
              <span className={classes.filter_label}>Criteria Type:</span>
              <div className={classes.filter_input_box}>
                <Radio.Group
                  className={classes.filter_input_radio}
                  options={criteriaTypeItems}
                  value={criteriaType}
                  onChange={(e) => handleCriteriaTypeChange(e.target.value)}
                />
              </div>
            </div>

            {criteriaType === FIELD_NAMES.TOTAL_HOURS ? (
              <div className={classes.search_filter_input}>
                <span className={classes.filter_label}>Total Hours:</span>
                <div className={`${classes.filter_input_box} ${classes.with_operator}`}>
                  <Form.Item
                    className={classes.filter_form_item}
                    validateStatus={getFormikFieldStatus(searchFormik, `${FIELD_NAMES.TOTAL_HOURS}.operator`)}
                    help={getFormikFieldError(searchFormik, `${FIELD_NAMES.TOTAL_HOURS}.operator`) as string}
                  >
                    <Select
                      className={classes.filter_operator}
                      options={FILTER_OPERATORS_METADATA}
                      placeholder="Operator"
                      allowClear
                      value={searchFormik.values[FIELD_NAMES.TOTAL_HOURS].operator}
                      onChange={(value) => handleOperatorChange(value)}
                      onBlur={() => searchFormik.setFieldTouched(`${FIELD_NAMES.TOTAL_HOURS}.operator`, true)}
                    />
                  </Form.Item>
                  <div className={classes.filter_range_inputs}>
                    <Form.Item
                      className={classes.filter_form_item}
                      validateStatus={getFormikFieldStatus(searchFormik, `${FIELD_NAMES.TOTAL_HOURS}.value`)}
                      help={getFormikFieldError(searchFormik, `${FIELD_NAMES.TOTAL_HOURS}.value`) as string}
                    >
                      <InputNumber
                        className={classes.range_input}
                        placeholder={
                          searchFormik.values[FIELD_NAMES.TOTAL_HOURS].operator === OPERATORS.BETWEEN ? "Min" : "Hours"
                        }
                        min={1}
                        name="totalHoursMin"
                        value={searchFormik.values[FIELD_NAMES.TOTAL_HOURS].value}
                        onChange={(value) => searchFormik.setFieldValue(`${FIELD_NAMES.TOTAL_HOURS}.value`, value)}
                        onBlur={() => searchFormik.setFieldTouched(`${FIELD_NAMES.TOTAL_HOURS}.value`, true)}
                      />
                    </Form.Item>
                    {searchFormik.values[FIELD_NAMES.TOTAL_HOURS].operator === OPERATORS.BETWEEN && (
                      <Form.Item
                        className={classes.filter_form_item}
                        validateStatus={getFormikFieldStatus(searchFormik, `${FIELD_NAMES.TOTAL_HOURS}.secondValue`)}
                        help={getFormikFieldError(searchFormik, `${FIELD_NAMES.TOTAL_HOURS}.secondValue`) as string}
                      >
                        <InputNumber
                          className={classes.range_input}
                          placeholder="Max"
                          min={1}
                          name="totalHoursMax"
                          value={searchFormik.values[FIELD_NAMES.TOTAL_HOURS].secondValue}
                          onChange={(value) =>
                            searchFormik.setFieldValue(`${FIELD_NAMES.TOTAL_HOURS}.secondValue`, value)
                          }
                          onBlur={() => searchFormik.setFieldTouched(`${FIELD_NAMES.TOTAL_HOURS}.secondValue`, true)}
                        />
                      </Form.Item>
                    )}
                  </div>
                </div>
              </div>
            ) : (
              <div className={classes.search_filter_input}>
                <span className={classes.filter_label}>Evaluation:</span>
                <div className={classes.filter_input_box}>
                  <Select
                    className={classes.filter_operator}
                    options={EVALUATION_FILTER_OPTIONS}
                    placeholder="Evaluation"
                    allowClear
                    value={searchFormik.values[FIELD_NAMES.EVALUATION]}
                    onChange={(value) => searchFormik.setFieldValue(FIELD_NAMES.EVALUATION, value)}
                    onBlur={() => searchFormik.setFieldTouched(FIELD_NAMES.EVALUATION, true)}
                  />
                </div>
              </div>
            )}
          </div>

          <div className={classes.search_filters_actions}>
            <Button
              icon={<RotateCcw />}
              onClick={() => {
                searchFormik.resetForm();
                setCriteriaType(FIELD_NAMES.TOTAL_HOURS);
                setFilters({});
              }}
              className={`${classes.filter_button} ${classes.clear}`}
            >
              Clear
            </Button>
            <Button icon={<Search />} htmlType="submit" className={`${classes.filter_button} ${classes.search}`}>
              Search
            </Button>
          </div>
        </form>
      </CollapsibleSection>
    </div>
  );
};

export default SearchFilter;
