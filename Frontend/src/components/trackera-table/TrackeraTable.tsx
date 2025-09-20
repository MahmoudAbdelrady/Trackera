import { Button, Table } from "antd";
import type { TrackeraTableEntity, WorklogTableProps } from "../../shared/types";
import classes from "./scss/trackera-table.module.css";

const TrackeraTable = <T extends TrackeraTableEntity>(props: WorklogTableProps<T>) => {
  return (
    <div className={classes.worklog_table_container}>
      {props.actionButtons && props.actionButtons.length > 0 && (
        <div className={classes.table_actions}>
          {props.actionButtons.map((button, index) => (
            <Button
              key={index}
              icon={button.icon}
              disabled={button.disabled}
              className={`${classes.log_button} ${button.customClasses?.map((className) => classes[className]).join(" ")}`}
              onClick={button.onClick}
            >
              {button.label}
            </Button>
          ))}
        </div>
      )}
      <div className={classes.table_details}>
        <Table
          {...props.properties}
          scroll={{ x: 768 }}
          rowKey={(record) => {
            if ("row" in record && record.row != null) {
              return record.row.toString();
            } else if ("id" in record && record.id != null) {
              return record.id.toString();
            }
            return Math.random().toString();
          }}
          className={classes.worklogs_table}
        />
      </div>
    </div>
  );
};

export default TrackeraTable;
