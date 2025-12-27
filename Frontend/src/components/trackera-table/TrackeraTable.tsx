import { Button, Dropdown, Table, type MenuProps, type TableProps } from "antd";
import classes from "./scss/trackera-table.module.css";
import type { TableActionButtonProps } from "./trackera-table.types";
import type React from "react";

interface TrackeraTableProps<T> {
  properties: TableProps<T>;
  rowKey: (record: T) => React.Key;
  actionButtons?: TableActionButtonProps[];
}

const TrackeraTable = <T,>(props: TrackeraTableProps<T>) => {
  const { actionButtons: tableActionBtns, rowKey: tableRowKey, properties: tableProperties } = props;

  const getTableActionButtons = (button: TableActionButtonProps): MenuProps["items"] => {
    return (
      button.options?.map((option) => ({
        key: option.label,
        label: (
          <div
            className={`${classes.sync_item} ${option.customClasses?.map((className) => classes[className]).join(" ")}`}
          >
            {option.icon}
            <span className={classes.sync_item_label}>{option.label}</span>
          </div>
        ),
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
              <Dropdown
                trigger={["click"]}
                menu={{ items: getTableActionButtons(button) }}
                className={classes.log_button_dropdown}
              >
                <div className={classes.btn_info}>
                  {button.icon}
                  {button.label}
                </div>
              </Dropdown>
            ) : (
              <Button
                key={index}
                icon={button.icon}
                disabled={button.disabled}
                className={`${classes.log_button} ${button.customClasses
                  ?.map((className) => classes[className])
                  .join(" ")}`}
                onClick={button.onClick}
              >
                {button.label}
              </Button>
            );
          })}
        </div>
      )}
      <div className={classes.table_details}>
        <Table {...tableProperties} scroll={{ x: 768 }} rowKey={tableRowKey} className={classes.worklogs_table} />
      </div>
    </div>
  );
};

export default TrackeraTable;
