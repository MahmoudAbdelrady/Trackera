import { Button, Dropdown, Table, type MenuProps } from "antd";
import type { TrackeraTableEntity, WorklogTableActionButtonProps, WorklogTableProps } from "../../shared/types";
import classes from "./scss/trackera-table.module.css";

const TrackeraTable = <T extends TrackeraTableEntity>(props: WorklogTableProps<T>) => {
  const { actionButtons: tableActionBtns, properties: tableProperties } = props;

  const getTableActionButtons = (button: WorklogTableActionButtonProps): MenuProps["items"] => {
    return (
      button.options?.map((option) => ({
        key: option.label,
        label: option.label,
        icon: option.icon,
        onClick: option.onClick,
        disabled: option.disabled,
      })) ?? []
    );
  };

  return (
    <div className={classes.worklog_table_container}>
      {tableActionBtns && tableActionBtns.length > 0 && (
        <div className={classes.table_actions}>
          {tableActionBtns.map((button, index) => {
            return button.options && button.options.length > 0 ? (
              <Dropdown trigger={["click"]} menu={{ items: getTableActionButtons(button) }} className={classes.log_button_dropdown}>
                {button.label}
              </Dropdown>
            ) : (
              <Button
                key={index}
                icon={button.icon}
                disabled={button.disabled}
                className={`${classes.log_button} ${button.customClasses?.map((className) => classes[className]).join(" ")}`}
                onClick={button.onClick}
              >
                {button.label}
              </Button>
            );
          })}
        </div>
      )}
      <div className={classes.table_details}>
        <Table
          {...tableProperties}
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
