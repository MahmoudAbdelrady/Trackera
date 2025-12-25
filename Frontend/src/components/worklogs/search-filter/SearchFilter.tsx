import { Funnel, Search, RotateCcw, Info } from "lucide-react";
import { Button, DatePicker, Form, Input, InputNumber, Radio, Select, Tooltip } from "antd";

import classes from "./scss/search-filter.module.css";
import CollapsibleSection from "../../collapsible-section/CollapsibleSection";
import type { WorkLogSearchFilter, WorkLogsFilterProps } from "../../../shared/types";
import { useFormik } from "formik";
import { searchFilterSchema } from "../../../shared/yup-schemas";
import { formatDate, getFormikFieldError, getFormikFieldStatus } from "../../../utils";
import { useState } from "react";

const fieldConfig: Record<string, any> = {
  logName: null,
  totalHours: { operator: null, value: null, secondValue: null } as WorkLogSearchFilter,
  dateFrom: null,
  dateTo: null,
  evaluation: null,
  status: null,
};

type CriteriaType = "totalHours" | "evaluation";

const criteriaTypeItems = [
  { label: "Total Hours", value: "totalHours" },
  { label: "Evaluation", value: "evaluation" },
];

type SearchFilterFields = {
  [K in keyof typeof fieldConfig]: (typeof fieldConfig)[K];
};

const SearchFilter = (props: WorkLogsFilterProps) => {
  const [criteriaType, setCriteriaType] = useState<CriteriaType>("totalHours");

  const totalHoursFilterOperators: Record<string, string>[] = [
    {
      label: "=",
      value: "=",
    },
    {
      label: "!=",
      value: "!=",
    },
    {
      label: ">",
      value: ">",
    },
    {
      label: ">=",
      value: ">=",
    },
    {
      label: "<",
      value: "<",
    },
    {
      label: "<=",
      value: "<=",
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
      acc[key] = fieldConfig[key];
      return acc;
    }, {} as SearchFilterFields),
    validationSchema: searchFilterSchema,
    onSubmit: (values) => {
      props.setFilters(buildSearchFilters(values));
    },
  });

  const buildSearchFilters = (values: typeof searchFormik.values) => {
    const filters: Record<string, any> = {};

    Object.entries(values).forEach(([fieldName, fieldValue]) => {
      if (fieldValue === null || fieldValue === undefined) {
        return;
      }

      if (fieldValue && typeof fieldValue === "object" && "operator" in fieldValue && "value" in fieldValue) {
        const { operator, value, secondValue } = fieldValue;
        if (value !== null && value !== undefined && value !== "") {
          filters[fieldName] = {
            operator,
            value,
            ...(secondValue !== null && secondValue !== undefined && secondValue !== "" ? { secondValue } : {}),
          };
        }
        return;
      }

      filters[fieldName] = fieldValue;
    });

    if (filters.dateFrom) filters.dateFrom = formatDate(filters.dateFrom);
    if (filters.dateTo) filters.dateTo = formatDate(filters.dateTo);

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
    if (value === "totalHours") {
      searchFormik.setFieldValue("evaluation", fieldConfig["evaluation"]);
    } else {
      searchFormik.setFieldValue("totalHours", fieldConfig["totalHours"]);
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
                  value={searchFormik.values.logName}
                  onChange={(e) => handleNestedChange("logName", e.target.value)}
                  onBlur={() => handleNestedBlur("logName")}
                />
              </div>
            </div>

            <div className={classes.search_filter_input}>
              <span className={classes.filter_label}>Date From:</span>
              <div className={classes.filter_input_box}>
                <Form.Item
                  className={classes.filter_form_item}
                  validateStatus={getFormikFieldStatus(searchFormik, "dateFrom")}
                  help={getFormikFieldError(searchFormik, "dateFrom") as string}
                >
                  <DatePicker
                    placeholder="Select Date"
                    style={{ width: "100%" }}
                    name="dateFrom"
                    value={searchFormik.values.dateFrom}
                    onChange={(value) => handleNestedChange("dateFrom", value)}
                    onBlur={() => handleNestedBlur("dateFrom")}
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
                  value={searchFormik.values.dateTo}
                  onChange={(value) => handleNestedChange("dateTo", value)}
                  onBlur={() => handleNestedBlur("dateTo")}
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
                  value={searchFormik.values.status}
                  onChange={(value) => handleNestedChange("status", value)}
                  onBlur={() => handleNestedBlur("status")}
                  disabled={!props.jiraLinked}
                />
                {!props.jiraLinked && (
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

            {criteriaType === "totalHours" ? (
              <div className={classes.search_filter_input}>
                <span className={classes.filter_label}>Total Hours:</span>
                <div className={`${classes.filter_input_box} ${classes.with_operator}`}>
                  <Form.Item
                    className={classes.filter_form_item}
                    validateStatus={getFormikFieldStatus(searchFormik, "totalHours.operator")}
                    help={getFormikFieldError(searchFormik, "totalHours.operator") as string}
                  >
                    <Select
                      className={classes.filter_operator}
                      options={totalHoursFilterOperators}
                      placeholder="Operator"
                      allowClear
                      value={searchFormik.values.totalHours.operator}
                      onChange={(value) => handleNestedChange("totalHours.operator", value)}
                      onBlur={() => handleNestedBlur("totalHours.operator")}
                    />
                  </Form.Item>
                  <div className={classes.filter_range_inputs}>
                    <Form.Item
                      className={classes.filter_form_item}
                      validateStatus={getFormikFieldStatus(searchFormik, "totalHours.value")}
                      help={getFormikFieldError(searchFormik, "totalHours.value") as string}
                    >
                      <InputNumber
                        className={classes.range_input}
                        placeholder={searchFormik.values.totalHours.operator === "BETWEEN" ? "Min" : "Hours"}
                        min={1}
                        name="totalHoursMin"
                        value={searchFormik.values.totalHours.value}
                        onChange={(value) => handleNestedChange("totalHours.value", value)}
                        onBlur={() => handleNestedBlur("totalHours.value")}
                      />
                    </Form.Item>
                    {searchFormik.values.totalHours.operator === "BETWEEN" && (
                      <Form.Item
                        className={classes.filter_form_item}
                        validateStatus={getFormikFieldStatus(searchFormik, "totalHours.secondValue")}
                        help={getFormikFieldError(searchFormik, "totalHours.secondValue") as string}
                      >
                        <InputNumber
                          className={classes.range_input}
                          placeholder="Max"
                          min={1}
                          name="totalHoursMax"
                          value={searchFormik.values.totalHours.secondValue}
                          onChange={(value) => handleNestedChange("totalHours.secondValue", value)}
                          onBlur={() => handleNestedBlur("totalHours.secondValue")}
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
                    value={searchFormik.values.evaluation}
                    onChange={(value) => handleNestedChange("evaluation", value)}
                    onBlur={() => handleNestedBlur("evaluation")}
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
                setCriteriaType("totalHours");
                props.setFilters({});
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
