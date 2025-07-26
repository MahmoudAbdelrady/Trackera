import { Button, Table } from "antd";
import type { WorklogTableProps } from "../../../utils/types";
import classes from "./scss/worklog-table.module.css";

const WorklogTable = (props: WorklogTableProps) => {
  return (
    <div className={classes.worklog_table_container}>
      <div className={classes.table_actions}>
        {props.actionButtons.map((button, index) => (
          <Button
            key={index}
            icon={button.icon}
            className={`${classes.log_button} ${button.customClasses
              ?.map((className) => classes[className])
              .join(" ")}`}
            onClick={button.onClick}
          >
            {button.label}
          </Button>
        ))}
      </div>
      <div className={classes.table_details}>
        <Table
          columns={props.columns}
          dataSource={props.dataSource}
          scroll={{ x: 768 }}
          className={classes.worklogs_table}
        />
      </div>
    </div>
  );
};

export default WorklogTable;
