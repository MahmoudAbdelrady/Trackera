import { Funnel, Search, RotateCcw } from "lucide-react";
import { Button, DatePicker, Form, Input, InputNumber, Radio, Select } from "antd";

import classes from "./scss/search-filter.module.css";
import CollapsibleSection from "../../collapsible-section/CollapsibleSection";
import type { WorkLogSearchFilter, WorkLogsFilterProps } from "../../../shared/types";
import { useFormik } from "formik";
import { searchFilterSchema } from "../../../shared/yup-schemas";
import { formatDate, getFormikFieldError, getFormikFieldStatus } from "../../../utils";
import { useState } from "react";

const fieldConfig: Record<string, WorkLogSearchFilter> = {
  logName: { fieldName: "name", operator: null, value: null },
  logHours: { fieldName: "totalHours", operator: null, value: null, extraValue: null },
  dateFrom: { fieldName: "dateFrom", operator: null, value: null },
  dateTo: { fieldName: "dateTo", operator: null, value: null },
  evaluation: { fieldName: "evaluation", operator: null, value: null },
  status: { fieldName: "status", operator: null, value: null },
};

type CriteriaType = "logHours" | "evaluation";

const criteriaTypeItems = [
  { label: "Total Hours", value: "logHours" },
  { label: "Evaluation", value: "evaluation" },
];

type SearchFilterFields = {
  [K in keyof typeof fieldConfig]: WorkLogSearchFilter;
};

const SearchFilter = (props: WorkLogsFilterProps) => {
  const [criteriaType, setCriteriaType] = useState<CriteriaType>("logHours");

  const logHoursFilterOperators: Record<string, string>[] = [
    {
      label: "=",
      value: "EQUALS",
    },
    {
      label: "!=",
      value: "NOT_EQUALS",
    },
    {
      label: ">",
      value: "GREATER_THAN",
    },
    {
      label: ">=",
      value: "GREATER_THAN_EQUAL",
    },
    {
      label: "<",
      value: "LESS_THAN",
    },
    {
      label: "<=",
      value: "LESS_THAN_EQUAL",
    },
    {
      label: "Between",
      value: "BETWEEN",
    },
  ];

  const evaluationFilterOptions: Record<string, string>[] = [
    {
      label: "Excellent",
      value: "EXCELLENT",
    },
    {
      label: "Good",
      value: "GOOD",
    },
    {
      label: "Moderate",
      value: "MODERATE",
    },
    {
      label: "Poor",
      value: "POOR",
    },
  ];

  const statusFilterOptions: Record<string, string>[] = [
    {
      label: "Synced",
      value: "SYNCED",
    },
    {
      label: "Partially",
      value: "PARTIALLY",
    },
    {
      label: "Not Synced",
      value: "NOT_SYNCED",
    },
  ];

  const searchFormik = useFormik({
    initialValues: Object.keys(fieldConfig).reduce((acc, key) => {
      acc[key] = { ...fieldConfig[key] };
      return acc;
    }, {} as SearchFilterFields),
    validationSchema: searchFilterSchema,
    onSubmit: (values) => {
      props.setFilters(buildSearchFilters(values));
      props.setFetchWorkLog(true);
    },
  });

  const buildSearchFilters = (values: typeof searchFormik.values) => {
    let filters = Object.values(values)
      .filter((filter) => filter.value !== null && filter.value !== undefined && filter.value !== "")
      .map((f) => ({ ...f })); // create a shallow copy to avoid mutating formik state

    const dateFromFilter = filters.find((f) => f.fieldName === "dateFrom");
    const dateToFilter = filters.find((f) => f.fieldName === "dateTo");

    if (dateFromFilter) {
      dateFromFilter.value = formatDate(dateFromFilter.value);
    }
    if (dateToFilter) {
      dateToFilter.value = formatDate(dateToFilter.value);
    }

    return filters;
  };

  const handleNestedChange = (field: keyof typeof searchFormik.values, value: any) => {
    searchFormik.setFieldValue(field, value);
  };

  const handleNestedBlur = (field: keyof typeof searchFormik.values) => {
    searchFormik.setFieldTouched(field, true);
  };

  const handleCriteriaTypeChange = (value: CriteriaType) => {
    setCriteriaType(value);
    if (value === "logHours") {
      searchFormik.setFieldValue("evaluation", { ...fieldConfig["evaluation"] });
    } else {
      searchFormik.setFieldValue("logHours", { ...fieldConfig["logHours"] });
    }
  };

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
                  name="logName"
                  value={searchFormik.values.logName.value}
                  onChange={(e) => handleNestedChange("logName.value", e.target.value)}
                  onBlur={() => handleNestedBlur("logName.value")}
                />
              </div>
            </div>

            <div className={classes.search_filter_input}>
              <span className={classes.filter_label}>Date From:</span>
              <div className={classes.filter_input_box}>
                <Form.Item
                  className={classes.filter_form_item}
                  validateStatus={getFormikFieldStatus(searchFormik, "dateFrom.value")}
                  help={getFormikFieldError(searchFormik, "dateFrom.value") as string}
                >
                  <DatePicker
                    placeholder="Select Date"
                    style={{ width: "100%" }}
                    name="dateFrom"
                    value={searchFormik.values.dateFrom.value}
                    onChange={(value) => handleNestedChange("dateFrom.value", value)}
                    onBlur={() => handleNestedBlur("dateFrom.value")}
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
                  name="dateTo"
                  value={searchFormik.values.dateTo.value}
                  onChange={(value) => handleNestedChange("dateTo.value", value)}
                  onBlur={() => handleNestedBlur("dateTo.value")}
                />
              </div>
            </div>

            <div className={classes.search_filter_input}>
              <span className={classes.filter_label}>Status:</span>
              <div className={classes.filter_input_box}>
                <Select
                  className={classes.filter_operator}
                  options={statusFilterOptions}
                  placeholder="Status"
                  allowClear
                  value={searchFormik.values.status.value}
                  onChange={(value) => handleNestedChange("status.value", value)}
                  onBlur={() => handleNestedBlur("status.value")}
                />
              </div>
            </div>

            <div className={classes.search_filter_input}>
              <span className={classes.filter_label}>Criteria Type:</span>
              <div className={classes.filter_input_box}>
                <Radio.Group className={classes.filter_input_radio} options={criteriaTypeItems} value={criteriaType} onChange={(e) => handleCriteriaTypeChange(e.target.value)} />
              </div>
            </div>

            {criteriaType === "logHours" ? (
              <div className={classes.search_filter_input}>
                <span className={classes.filter_label}>Total Hours:</span>
                <div className={`${classes.filter_input_box} ${classes.with_operator}`}>
                  <Form.Item
                    className={classes.filter_form_item}
                    validateStatus={getFormikFieldStatus(searchFormik, "logHours.operator")}
                    help={getFormikFieldError(searchFormik, "logHours.operator") as string}
                  >
                    <Select
                      className={classes.filter_operator}
                      options={logHoursFilterOperators}
                      placeholder="Operator"
                      allowClear
                      value={searchFormik.values.logHours.operator}
                      onChange={(value) => handleNestedChange("logHours.operator", value)}
                      onBlur={() => handleNestedBlur("logHours.operator")}
                    />
                  </Form.Item>
                  <div className={classes.filter_range_inputs}>
                    <Form.Item
                      className={classes.filter_form_item}
                      validateStatus={getFormikFieldStatus(searchFormik, "logHours.value")}
                      help={getFormikFieldError(searchFormik, "logHours.value") as string}
                    >
                      <InputNumber
                        className={classes.range_input}
                        placeholder={searchFormik.values.logHours.operator === "BETWEEN" ? "Min" : "Hours"}
                        min={1}
                        name="logHoursMin"
                        value={searchFormik.values.logHours.value}
                        onChange={(value) => handleNestedChange("logHours.value", value)}
                        onBlur={() => handleNestedBlur("logHours.value")}
                      />
                    </Form.Item>
                    {searchFormik.values.logHours.operator === "BETWEEN" && (
                      <Form.Item
                        className={classes.filter_form_item}
                        validateStatus={getFormikFieldStatus(searchFormik, "logHours.extraValue")}
                        help={getFormikFieldError(searchFormik, "logHours.extraValue") as string}
                      >
                        <InputNumber
                          className={classes.range_input}
                          placeholder="Max"
                          min={1}
                          name="logHoursMax"
                          value={searchFormik.values.logHours.extraValue}
                          onChange={(value) => handleNestedChange("logHours.extraValue", value)}
                          onBlur={() => handleNestedBlur("logHours.extraValue")}
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
                    options={evaluationFilterOptions}
                    placeholder="Evaluation"
                    allowClear
                    value={searchFormik.values.evaluation.value}
                    onChange={(value) => handleNestedChange("evaluation.value", value)}
                    onBlur={() => handleNestedBlur("evaluation.value")}
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
                setCriteriaType("logHours");
                props.setFilters([]);
                props.setFetchWorkLog(true);
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
