import { Funnel, Search, RotateCcw } from "lucide-react";
import { Button, DatePicker, Input, InputNumber, Select } from "antd";

import classes from "./scss/search-filter.module.css";
import CollapsibleSection from "../../collapsible-section/CollapsibleSection";
import type { WorkLogSearchFilter, WorkLogsFilterProps } from "../../../shared/types";
import { useFormik } from "formik";
import { searchFilterSchema } from "../../../shared/yup-schemas";
import dayjs from "dayjs";
import { formatDate } from "../../../utils";

const fieldConfig: Record<string, WorkLogSearchFilter> = {
  logName: { fieldName: "name", operator: null, value: null },
  logHours: { fieldName: "totalHours", operator: null, value: null, extraValue: null },
  dateFrom: { fieldName: "dateFrom", operator: null, value: null },
  dateTo: { fieldName: "dateTo", operator: null, value: null },
  evaluation: { fieldName: "evaluation", operator: null, value: null },
  status: { fieldName: "status", operator: null, value: null },
};

type SearchFilterFields = {
  [K in keyof typeof fieldConfig]: WorkLogSearchFilter;
};

const SearchFilter = (props: WorkLogsFilterProps) => {
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

  const getTotalHoursFromEvaluation = (evaluation: string) => {
    switch (evaluation) {
      case "EXCELLENT":
        return 8;
      case "GOOD":
        return 7.5;
      case "MODERATE":
        return 7;
      case "POOR":
        return 6;
      default:
        return null;
    }
  };

  const buildSearchFilters = (values: typeof searchFormik.values) => {
    let filters = Object.values(values);

    const evaluationFilter = filters.find((f) => f.fieldName === "evaluation");

    if (evaluationFilter && evaluationFilter.value) {
      let logHoursFilter = filters.find((f) => f.fieldName === "totalHours");

      if (logHoursFilter) {
        logHoursFilter.value = getTotalHoursFromEvaluation(evaluationFilter.value);
      } else {
        filters.push({
          fieldName: "totalHours",
          operator: "GREATER_THAN_EQUAL",
          value: getTotalHoursFromEvaluation(evaluationFilter.value),
        });
      }

      filters = filters.filter((f) => f.fieldName !== "evaluation");
    }

    filters = filters.filter((filter) => filter.value !== null && filter.value !== undefined && filter.value !== "");

    return filters;
  };

  const handleNestedChange = (outerField: keyof typeof searchFormik.values, innerField: keyof WorkLogSearchFilter, value: any) => {
    searchFormik.setFieldValue(`${outerField}.${innerField}`, value);
  };

  const handleNestedBlur = (outerField: keyof typeof searchFormik.values, innerField: keyof WorkLogSearchFilter) => {
    searchFormik.setFieldTouched(`${outerField}.${innerField}`, true);
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
                  onChange={(e) => handleNestedChange("logName", "value", e.target.value)}
                  onBlur={() => handleNestedBlur("logName", "value")}
                />
              </div>
            </div>

            <div className={classes.search_filter_input}>
              <span className={classes.filter_label}>Total Hours:</span>
              <div className={classes.filter_input_box}>
                <Select
                  options={logHoursFilterOperators}
                  placeholder="Select Operator"
                  allowClear
                  value={searchFormik.values.logHours.operator}
                  onChange={(value) => handleNestedChange("logHours", "operator", value)}
                  onBlur={() => handleNestedBlur("logHours", "operator")}
                />
                {searchFormik.values.logHours.operator === "BETWEEN" ? (
                  <div className={classes.filter_range_inputs}>
                    <InputNumber
                      placeholder="Min"
                      min={1}
                      name="logHoursMin"
                      value={searchFormik.values.logHours.value}
                      onChange={(value) => handleNestedChange("logHours", "value", value)}
                      onBlur={() => handleNestedBlur("logHours", "value")}
                    />
                    <InputNumber
                      placeholder="Max"
                      min={1}
                      name="logHoursMax"
                      value={searchFormik.values.logHours.extraValue}
                      onChange={(value) => handleNestedChange("logHours", "extraValue", value)}
                      onBlur={() => handleNestedBlur("logHours", "extraValue")}
                    />
                  </div>
                ) : (
                  <InputNumber
                    placeholder="Hours"
                    min={1}
                    name="logHours"
                    value={searchFormik.values.logHours.value}
                    onChange={(value) => handleNestedChange("logHours", "value", value)}
                    onBlur={() => handleNestedBlur("logHours", "value")}
                  />
                )}
              </div>
            </div>

            <div className={classes.search_filter_input}>
              <span className={classes.filter_label}>Date From:</span>
              <div className={classes.filter_input_box}>
                <DatePicker
                  placeholder="Select Date"
                  style={{ width: "100%" }}
                  name="dateFrom"
                  value={searchFormik.values.dateFrom.value ? dayjs(searchFormik.values.dateFrom.value) : null}
                  onChange={(value) => handleNestedChange("dateFrom", "value", formatDate(value))}
                  onBlur={() => handleNestedBlur("dateFrom", "value")}
                />
              </div>
            </div>

            <div className={classes.search_filter_input}>
              <span className={classes.filter_label}>Date To:</span>
              <div className={classes.filter_input_box}>
                <DatePicker
                  placeholder="Select Date"
                  style={{ width: "100%" }}
                  name="dateTo"
                  value={searchFormik.values.dateTo.value ? dayjs(searchFormik.values.dateTo.value) : null}
                  onChange={(value) => handleNestedChange("dateTo", "value", formatDate(value))}
                  onBlur={() => handleNestedBlur("dateTo", "value")}
                />
              </div>
            </div>

            <div className={classes.search_filter_input}>
              <span className={classes.filter_label}>Evaluation:</span>
              <div className={classes.filter_input_box}>
                <Select
                  options={evaluationFilterOptions}
                  placeholder="Select Evaluation"
                  allowClear
                  value={searchFormik.values.evaluation.value}
                  onChange={(value) => handleNestedChange("evaluation", "value", value)}
                  onBlur={() => handleNestedBlur("evaluation", "value")}
                />
              </div>
            </div>

            <div className={classes.search_filter_input}>
              <span className={classes.filter_label}>Status:</span>
              <div className={classes.filter_input_box}>
                <Select
                  options={statusFilterOptions}
                  placeholder="Select Status"
                  allowClear
                  value={searchFormik.values.status.value}
                  onChange={(value) => handleNestedChange("status", "value", value)}
                  onBlur={() => handleNestedBlur("status", "value")}
                />
              </div>
            </div>
          </div>

          <div className={classes.search_filters_actions}>
            <Button
              icon={<RotateCcw />}
              onClick={() => {
                searchFormik.resetForm();
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
