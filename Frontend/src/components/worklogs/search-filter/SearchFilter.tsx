import { Funnel, Search, RotateCcw } from "lucide-react";
import { Button, Collapse, DatePicker, Input, InputNumber, Select } from "antd";
import { useState } from "react";

import classes from "./scss/search-filter.module.css";

const SearchFilter = () => {
  const [selectedHourOperator, setSelectedHourOperator] = useState<
    string | undefined
  >(undefined);
  const [selectedEvaluation, setSelectedEvaluation] = useState<
    string | undefined
  >(undefined);
  const [selectedStatus, setSelectedStatus] = useState<string | undefined>(
    undefined
  );

  const logHoursFilterOperators: Record<string, string>[] = [
    {
      label: "=",
      value: "equal",
    },
    {
      label: "!=",
      value: "notEqual",
    },
    {
      label: ">",
      value: "greaterThan",
    },
    {
      label: ">=",
      value: "greaterThanOrEqual",
    },
    {
      label: "<",
      value: "lessThan",
    },
    {
      label: "<=",
      value: "lessThanOrEqual",
    },
    {
      label: "Between",
      value: "between",
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
      label: "Unsynced",
      value: "UNSYNCED",
    },
  ];

  return (
    <div className={classes.search_filters_container}>
      <Collapse
        expandIconPosition="right"
        items={[
          {
            key: "1",
            label: (
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "8px",
                }}
              >
                <Funnel /> Filters
              </div>
            ),
            children: (
              <>
                <div className={classes.search_filters_fields_grid}>
                  <div className={classes.search_filter_input}>
                    <span className={classes.filter_label}>Log Name:</span>
                    <div className={classes.filter_input_box}>
                      <Input placeholder="Enter Log Name" />
                    </div>
                  </div>

                  <div className={classes.search_filter_input}>
                    <span className={classes.filter_label}>Total Hours:</span>
                    <div className={classes.filter_input_box}>
                      <Select
                        options={logHoursFilterOperators}
                        placeholder="Select Operator"
                        allowClear
                        onChange={(value) => setSelectedHourOperator(value)}
                      />
                      {selectedHourOperator === "between" ? (
                        <div className={classes.filter_range_inputs}>
                          <InputNumber placeholder="Min" min={1} />
                          <InputNumber placeholder="Max" min={1} />
                        </div>
                      ) : (
                        <InputNumber placeholder="Hours" min={1} />
                      )}
                    </div>
                  </div>

                  <div className={classes.search_filter_input}>
                    <span className={classes.filter_label}>Date From:</span>
                    <div className={classes.filter_input_box}>
                      <DatePicker
                        placeholder="Select Date"
                        style={{ width: "100%" }}
                      />
                    </div>
                  </div>

                  <div className={classes.search_filter_input}>
                    <span className={classes.filter_label}>Date To:</span>
                    <div className={classes.filter_input_box}>
                      <DatePicker
                        placeholder="Select Date"
                        style={{ width: "100%" }}
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
                        onChange={(value) => setSelectedEvaluation(value)}
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
                        onChange={(value) => setSelectedStatus(value)}
                      />
                    </div>
                  </div>
                </div>

                <div className={classes.search_filters_actions}>
                  <Button
                    icon={<RotateCcw />}
                    className={`${classes.filter_button} ${classes.clear}`}
                  >
                    Clear
                  </Button>
                  <Button
                    icon={<Search />}
                    className={`${classes.filter_button} ${classes.search}`}
                  >
                    Search
                  </Button>
                </div>
              </>
            ),
          },
        ]}
      />
    </div>
  );
};

export default SearchFilter;
